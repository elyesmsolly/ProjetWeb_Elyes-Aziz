import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin, switchMap } from 'rxjs';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { heure, minutesDepuis } from '../../core/format';
import { EtatFile, Statistiques, TicketLigne } from '../../core/models';
import { TempsReelService } from '../../core/temps-reel.service';
import { ToastService } from '../../core/toast.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-tableau-de-bord',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, DatePipe],
  templateUrl: './tableau-de-bord.html',
  styleUrl: './tableau-de-bord.css',
})
export class TableauDeBord {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly stats = signal<Statistiques | null>(null);
  protected readonly etat = signal<EtatFile | null>(null);
  protected readonly aujourdhui = new Date();
  protected readonly heure = heure;

  protected readonly heureActuelle = new Date().getHours();
  protected readonly maxAffluence = computed(() => Math.max(1, ...(this.stats()?.affluence.map((a) => a.nombre) ?? [1])));
  protected readonly lignes = computed(() => {
    const e = this.etat();
    const s = this.stats();
    if (!e) return [];
    return [...e.enCours, ...e.enAttente, ...(s?.derniers ?? [])];
  });

  constructor() {
    const s = this.auth.session()!;
    inject(TempsReelService).file(s.etablissementId!)
      .pipe(switchMap(() => forkJoin([this.api.statistiques(), this.api.etatFile()])), takeUntilDestroyed())
      .subscribe({
        next: ([st, et]) => { this.stats.set(st); this.etat.set(et); },
        error: (e) => this.toast.afficher(messageErreur(e), true),
      });
  }

  pause() {
    const e = this.etat();
    if (!e) return;
    this.api.ouverture(!e.fileOuverte).subscribe({
      next: (x) => { this.etat.set(x); this.toast.afficher(x.fileOuverte ? 'File rouverte.' : 'File en pause : plus de nouveaux tickets.'); },
      error: (err) => this.toast.afficher(messageErreur(err), true),
    });
  }

  statut(t: TicketLigne, i: number): { texte: string; cls: string } {
    switch (t.statut) {
      case 'APPELE': return { texte: 'En service · ' + (t.poste?.split(' · ')[0] ?? ''), cls: 'ok' };
      case 'SERVI': return { texte: 'Servi', cls: '' };
      case 'ABSENT': return { texte: 'Absent', cls: 'danger' };
      default:
        if (i === (this.etat()?.enCours.length ?? 0)) return { texte: 'Suivant', cls: 'primary' };
        return t.prioritaire ? { texte: 'Prioritaire', cls: 'warn' } : { texte: 'En attente', cls: '' };
    }
  }

  attente(t: TicketLigne) {
    if (t.statut === 'EN_ATTENTE') return `${minutesDepuis(t.creeLe)} min`;
    if (t.appeleLe) return `${Math.max(0, Math.round((new Date(t.appeleLe).getTime() - new Date(t.creeLe).getTime()) / 60000))} min`;
    return '—';
  }
}
