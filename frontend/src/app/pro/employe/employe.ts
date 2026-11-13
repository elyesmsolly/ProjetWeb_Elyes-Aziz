import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { heure, minutesDepuis } from '../../core/format';
import { EtatFile, TicketLigne } from '../../core/models';
import { TempsReelService } from '../../core/temps-reel.service';
import { ToastService } from '../../core/toast.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-employe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon],
  templateUrl: './employe.html',
  styleUrl: './employe.css',
})
export class Employe {
  private api = inject(ApiService);
  private router = inject(Router);
  private toast = inject(ToastService);
  private destroy = inject(DestroyRef);
  protected readonly auth = inject(AuthService);
  protected readonly enDirect = inject(TempsReelService).connecte;

  protected readonly etat = signal<EtatFile | null>(null);
  protected readonly occupe = signal(false);
  protected readonly maintenant = signal(Date.now());
  protected readonly nomSurPlace = signal('');
  protected readonly prestationSurPlace = signal<number | null>(null);
  protected readonly prioSurPlace = signal(false);

  protected readonly heure = heure;
  protected readonly monPoste = computed(() => {
    const s = this.auth.session();
    return s ? `${s.poste || 'Poste 1'} · ${s.nom}` : '';
  });
  protected readonly courant = computed(() => this.etat()?.enCours.find((t) => t.poste === this.monPoste()) ?? null);
  protected readonly chrono = computed(() => {
    const t = this.courant();
    if (!t?.appeleLe) return '00:00';
    const s = Math.max(0, Math.floor((this.maintenant() - new Date(t.appeleLe).getTime()) / 1000));
    return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  });
  protected readonly reponses = computed(() => {
    const t = this.courant();
    if (!t) return [];
    const champs = t.champs.filter((c) => c.cle !== 'nom' && c.cle !== 'telephone');
    const liste = champs.map((c) => ({ label: c.label, valeur: formater(t.reponses[c.cle]) }));
    return [...liste.slice(0, 2), { label: 'Ticket pris', valeur: `${heure(t.creeLe)} · ${t.surPlace ? 'sur place' : 'en ligne'}` }];
  });

  constructor() {
    const minuteur = setInterval(() => this.maintenant.set(Date.now()), 1000);
    this.destroy.onDestroy(() => clearInterval(minuteur));
    const s = this.auth.session();
    if (!s) return;
    inject(TempsReelService).file(s.etablissementId!)
      .pipe(switchMap(() => this.api.etatFile()), takeUntilDestroyed())
      .subscribe({
        next: (e) => { this.etat.set(e); if (this.prestationSurPlace() === null) this.prestationSurPlace.set(e.prestations[0]?.id ?? null); },
        error: (e) => { if (e.status === 401) this.sortir(); else this.toast.afficher(messageErreur(e), true); },
      });
  }

  private action(appel: Observable<EtatFile>, succes?: (e: EtatFile) => string) {
    if (this.occupe()) return;
    this.occupe.set(true);
    appel.subscribe({
      next: (e) => { this.etat.set(e); this.occupe.set(false); if (succes) this.toast.afficher(succes(e)); },
      error: (e) => { this.occupe.set(false); this.toast.afficher(messageErreur(e), true); },
    });
  }

  appelerSuivant() {
    this.action(this.api.appelerSuivant(), (e) => {
      const t = e.enCours.find((x) => x.poste === this.monPoste());
      return t ? `${t.numero} appelé : ${t.nomClient}` : 'Personne en attente.';
    });
  }
  servi() { this.action(this.api.servi()); }
  absent() { this.action(this.api.absent(), () => 'Client marqué absent.'); }
  pause() {
    const e = this.etat();
    if (e) this.action(this.api.ouverture(!e.fileOuverte), (x) => (x.fileOuverte ? 'File rouverte.' : 'File en pause : plus de nouveaux tickets.'));
  }

  ajouterSurPlace() {
    const nom = this.nomSurPlace().trim();
    const p = this.prestationSurPlace();
    if (!nom || !p) { this.toast.afficher('Saisissez le prénom du client.', true); return; }
    this.action(this.api.surPlace(nom, p, this.prioSurPlace()), (e) => {
      const t = [...e.enAttente].reverse().find((x) => x.nomClient === nom);
      this.nomSurPlace.set(''); this.prioSurPlace.set(false);
      return t ? `${nom} ajouté : ticket ${t.numero}` : `${nom} ajouté.`;
    });
  }

  badge(t: TicketLigne, i: number): { texte: string; cls: string } {
    if (i === 0) return { texte: 'Suivant', cls: 'primary' };
    if (t.prioritaire) return { texte: 'Prioritaire', cls: 'warn' };
    if (t.surPlace) return { texte: 'Sur place', cls: '' };
    return { texte: 'En ligne', cls: '' };
  }

  attendDepuis(t: TicketLigne) { return minutesDepuis(t.creeLe); }

  sortir() { this.auth.fermer(); this.router.navigateByUrl('/pro/connexion'); }
}

function formater(v: unknown): string {
  if (v === true) return 'Oui';
  if (v === false || v === null || v === undefined || v === '') return '—';
  return String(v);
}
