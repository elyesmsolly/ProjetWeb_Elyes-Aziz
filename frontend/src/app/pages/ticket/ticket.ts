import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subscription, switchMap } from 'rxjs';
import { ApiService, messageErreur } from '../../core/api.service';
import { DernierTicket } from '../../core/dernier-ticket';
import { heure } from '../../core/format';
import { TicketVue } from '../../core/models';
import { PositionService } from '../../core/position.service';
import { TempsReelService } from '../../core/temps-reel.service';
import { ToastService } from '../../core/toast.service';
import { Carte } from '../../shared/carte';
import { Icon } from '../../shared/icon';

const SEUIL_ALERTE_MIN = 15;

@Component({
  selector: 'app-ticket',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, Carte],
  templateUrl: './ticket.html',
  styleUrl: './ticket.css',
})
export class Ticket {
  readonly code = input<string>();

  private api = inject(ApiService);
  private router = inject(Router);
  private tempsReel = inject(TempsReelService);
  private toast = inject(ToastService);
  private destroy = inject(DestroyRef);
  protected readonly dernier = inject(DernierTicket);
  protected readonly position = inject(PositionService);
  protected readonly enDirect = this.tempsReel.connecte;

  protected readonly t = signal<TicketVue | null>(null);
  protected readonly historique = signal<TicketVue[] | null>(null);

  protected readonly erreur = signal<string | null>(null);
  protected readonly majIl = signal(0);
  protected readonly confirmerAnnulation = signal(false);
  protected readonly alerte15 = signal(true);
  protected readonly son = signal(true);
  protected readonly flash = signal(false);

  protected readonly heure = heure;
  protected readonly bientot = computed(() => {
    const t = this.t();
    return !!t && t.statut === 'EN_ATTENTE' && this.alerte15() && t.attenteMinutes <= SEUIL_ALERTE_MIN;
  });
  protected readonly etapes = computed(() => {
    const s = this.t()?.statut;
    const ordre = ['EN_ATTENTE', 'APPELE', 'SERVI'];
    const i = s === 'ABSENT' || s === 'ANNULE' ? -1 : ordre.indexOf(s ?? '');
    return { pris: true, attente: i >= 0, appele: i >= 1, servi: i >= 2 };
  });
  protected readonly reperes = computed(() => {
    const t = this.t();
    return t ? [{ id: 1, lat: t.latitude, lng: t.longitude, texte: t.numero }] : [];
  });

  private ecoute?: Subscription;
  private dernierStatut?: string;
  private derniereMaj = Date.now();

  constructor() {
    effect(() => {
      const code = this.code();
      if (code) { this.charger(code); return; }
      // « Mon ticket » sans code : on ouvre le ticket en cours du compte, sinon on montre l'historique.
      this.api.mesTickets().subscribe({
        next: (liste) => {
          const actif = liste.find((x) => x.statut === 'EN_ATTENTE' || x.statut === 'APPELE');
          if (actif) this.router.navigate(['/ticket', actif.code], { replaceUrl: true });
          else this.historique.set(liste);
        },
        error: (e) => this.erreur.set(messageErreur(e)),
      });
    });
    const minuteur = setInterval(() => this.majIl.set(Math.round((Date.now() - this.derniereMaj) / 1000)), 1000);
    this.destroy.onDestroy(() => clearInterval(minuteur));
  }

  private charger(code: string) {
    this.ecoute?.unsubscribe();
    this.api.ticket(code).subscribe({
      next: (t) => {
        this.appliquer(t);
        this.ecoute = this.tempsReel.file(t.etablissementId)
          .pipe(switchMap(() => this.api.ticket(code)), takeUntilDestroyed(this.destroy))
          .subscribe((maj) => this.appliquer(maj));
      },
      error: (e) => {
        if (e.status === 404) { this.dernier.oublier(); this.erreur.set('Ce ticket n’existe plus.'); }
        else if (e.status === 403) this.erreur.set('Ce ticket appartient à un autre compte.');
        else this.erreur.set(messageErreur(e));
      },
    });
  }

  private appliquer(t: TicketVue) {
    const avant = this.dernierStatut;
    this.t.set(t);
    this.derniereMaj = Date.now();
    this.majIl.set(0);
    this.dernierStatut = t.statut;
    if (avant && avant !== t.statut && t.statut === 'APPELE') this.cestVotreTour();
    else if (avant === 'EN_ATTENTE' && t.statut === 'EN_ATTENTE' && t.attenteMinutes <= SEUIL_ALERTE_MIN && this.son()) this.signal(1);
  }

  /** Le moment de l'application : l'afficheur s'allume, son et vibration. */
  private cestVotreTour() {
    this.flash.set(true);
    setTimeout(() => this.flash.set(false), 1600);
    if (this.son()) this.signal(3);
  }

  private signal(fois: number) {
    try { navigator.vibrate?.(Array.from({ length: fois * 2 - 1 }, (_, i) => (i % 2 ? 120 : 220))); } catch { /* non supporté */ }
    try {
      const ctx = new AudioContext();
      for (let i = 0; i < fois; i++) {
        const o = ctx.createOscillator(); const g = ctx.createGain();
        o.frequency.value = 880; o.connect(g); g.connect(ctx.destination);
        const t0 = ctx.currentTime + i * 0.35;
        g.gain.setValueAtTime(0.0001, t0); g.gain.exponentialRampToValueAtTime(0.25, t0 + 0.02); g.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.28);
        o.start(t0); o.stop(t0 + 0.3);
      }
    } catch { /* audio bloqué tant que l'utilisateur n'a pas interagi */ }
  }

  laisserPasser() {
    const t = this.t(); if (!t) return;
    this.api.laisserPasser(t.code).subscribe({
      next: (maj) => { this.appliquer(maj); this.toast.afficher('Vous avez laissé passer une personne. Votre ticket est gardé.'); },
      error: (e) => this.toast.afficher(messageErreur(e), true),
    });
  }

  annuler() {
    const t = this.t(); if (!t) return;
    if (!this.confirmerAnnulation()) { this.confirmerAnnulation.set(true); return; }
    this.api.annuler(t.code).subscribe({
      next: (maj) => { this.appliquer(maj); this.confirmerAnnulation.set(false); this.dernier.oublier(); this.toast.afficher('Ticket annulé.'); },
      error: (e) => this.toast.afficher(messageErreur(e), true),
    });
  }

  itineraire() {
    const t = this.t(); if (!t) return;
    window.open(`https://www.openstreetmap.org/directions?route=;${t.latitude},${t.longitude}#map=16/${t.latitude}/${t.longitude}`, '_blank', 'noopener');
  }

  libelleStatut(s: string) {
    return ({ EN_ATTENTE: 'En attente', APPELE: 'C’est votre tour', SERVI: 'Servi', ABSENT: 'Absent', ANNULE: 'Annulé' } as Record<string, string>)[s] ?? s;
  }
}
