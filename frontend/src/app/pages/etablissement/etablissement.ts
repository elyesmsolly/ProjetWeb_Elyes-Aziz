import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ApiService, messageErreur } from '../../core/api.service';
import { DernierTicket } from '../../core/dernier-ticket';
import { distance, heure, icone, note, prix } from '../../core/format';
import { EtablissementDetail } from '../../core/models';
import { PositionService } from '../../core/position.service';
import { TempsReelService } from '../../core/temps-reel.service';
import { ToastService } from '../../core/toast.service';
import { Carte } from '../../shared/carte';
import { Champ } from '../../shared/champ';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-etablissement',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, Icon, Champ, Carte],
  templateUrl: './etablissement.html',
  styleUrl: './etablissement.css',
})
export class Etablissement {
  readonly slug = input.required<string>();

  private api = inject(ApiService);
  private router = inject(Router);
  private tempsReel = inject(TempsReelService);
  private toast = inject(ToastService);
  private dernier = inject(DernierTicket);
  private destroy = inject(DestroyRef);
  protected readonly position = inject(PositionService);
  protected readonly auth = inject(AuthService);

  protected readonly detail = signal<EtablissementDetail | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly prestationId = signal<number | null>(null);
  protected readonly reponses = signal<Record<string, unknown>>({});
  protected readonly prioritaire = signal(false);
  protected readonly envoi = signal(false);
  protected readonly erreurForm = signal<string | null>(null);

  protected readonly fmt = { distance, note, prix, icone };

  protected readonly prestation = computed(() => this.detail()?.resume.prestations.find((p) => p.id === this.prestationId()));
  protected readonly passage = computed(() => {
    const d = this.detail();
    return d ? heure(new Date(Date.now() + d.resume.attenteMinutes * 60000)) : '—';
  });
  protected readonly horaires = computed(() => (this.detail()?.horaires ?? '').split('\n'));
  protected readonly reperes = computed(() => {
    const r = this.detail()?.resume;
    return r ? [{ id: r.id, lat: r.latitude, lng: r.longitude, texte: r.nom }] : [];
  });

  private ecoute?: Subscription;

  constructor() {
    effect(() => {
      const slug = this.slug();
      this.ecoute?.unsubscribe();
      this.api.etablissement(slug).subscribe({
        next: (d) => {
          this.detail.set(d);
          this.prestationId.set(d.resume.prestations[0]?.id ?? null);
          // Le nom et le téléphone du compte client sont proposés d'office.
          const s = this.auth.session();
          if (s?.role === 'CLIENT') this.reponses.update((r) => ({ nom: s.nom, telephone: s.telephone ?? '', ...r }));
          this.ecoute = this.tempsReel.file(d.resume.id)
            .pipe(switchMap(() => this.api.etablissement(slug)), takeUntilDestroyed(this.destroy))
            .subscribe((maj) => this.detail.set(maj));
        },
        error: (e) => this.erreur.set(e.status === 404 ? 'Cet établissement n’existe pas.' : messageErreur(e)),
      });
    });
  }

  repondre(cle: string, v: unknown) {
    this.reponses.update((r) => ({ ...r, [cle]: v }));
    this.erreurForm.set(null);
  }

  prendreTicket() {
    const d = this.detail();
    const p = this.prestationId();
    if (!d || !p || this.envoi()) return;
    const manquant = d.formulaire.champs.find((c) => c.obligatoire && (this.reponses()[c.cle] === undefined || String(this.reponses()[c.cle] ?? '').trim() === ''));
    if (manquant) { this.erreurForm.set(`Le champ « ${manquant.label} » est obligatoire.`); return; }
    this.envoi.set(true);
    this.api.prendreTicket(this.slug(), p, this.reponses(), this.prioritaire()).subscribe({
      next: (t) => {
        this.dernier.garder(t.code);
        this.toast.afficher(`Ticket ${t.numero} pris. Vous êtes dans la file.`);
        this.router.navigate(['/ticket', t.code]);
      },
      error: (e) => { this.erreurForm.set(messageErreur(e)); this.envoi.set(false); },
    });
  }
}
