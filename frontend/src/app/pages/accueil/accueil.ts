import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, Subscription, catchError, debounceTime, map, merge, of, skip, switchMap, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { badgeAttente, distance, heure, icone, note } from '../../core/format';
import { TicketVue } from '../../core/models';
import { TempsReelService } from '../../core/temps-reel.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-accueil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, Icon],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil {
  private api = inject(ApiService);
  private router = inject(Router);
  private tempsReel = inject(TempsReelService);
  protected readonly auth = inject(AuthService);

  protected readonly service = signal('');
  protected readonly lieu = signal('Ariana, Tunis');
  protected readonly categories = toSignal(this.api.categories().pipe(map((cs) => cs.filter((c) => !c.libre)), catchError(() => of([]))), { initialValue: [] });
  protected readonly proches = toSignal(this.api.rechercher().pipe(catchError(() => of(null))), { initialValue: undefined });

  protected readonly fmt = { distance, note, badgeAttente, icone, heure };
  protected readonly exemple = 'Je veux boire quelque chose';

  /**
   * Tickets en cours du client connecté (en attente ou appelé). L'API les donne déjà dans l'ordre de passage :
   * le premier est celui qui passe le plus tôt. `null` tant que la réponse n'est pas arrivée.
   */
  protected readonly enCours = signal<TicketVue[] | null>(null);
  private readonly recharger = new Subject<void>();
  private ecoute?: Subscription;
  private suivis = '';

  constructor() {
    if (!this.auth.client()) return;
    // Rechargé toutes les 30 s, et tout de suite quand une des files concernées bouge (temps réel).
    merge(timer(0, 30_000), this.recharger).pipe(
      switchMap(() => this.api.mesTickets().pipe(catchError(() => of(null)))),
      takeUntilDestroyed(),
    ).subscribe((liste) => {
      if (!liste) return;
      const actifs = liste.filter((t) => t.statut === 'EN_ATTENTE' || t.statut === 'APPELE');
      this.enCours.set(actifs);
      this.suivre(actifs.map((t) => t.etablissementId));
    });
    inject(DestroyRef).onDestroy(() => this.ecoute?.unsubscribe());
  }

  /** S'abonne aux files des tickets en cours (seulement quand la liste des établissements change). */
  private suivre(ids: number[]) {
    const cle = [...new Set(ids)].sort().join(',');
    if (cle === this.suivis) return;
    this.suivis = cle;
    this.ecoute?.unsubscribe();
    // skip(1) : chaque abonnement émet une première fois tout de suite, on vient déjà de charger.
    this.ecoute = merge(...cle.split(',').filter(Boolean).map((id) => this.tempsReel.file(+id).pipe(skip(1))))
      .pipe(debounceTime(300)).subscribe(() => this.recharger.next());
  }

  rechercher() {
    this.router.navigate(['/etablissements'], { queryParams: { q: this.service() || null } });
  }

  /** Recherche avec l'IA : la phrase tapée (ou l'exemple) est envoyée à l'assistant sur la page des résultats. */
  demanderAssistant(phrase = this.service().trim()) {
    this.router.navigate(['/etablissements'], { queryParams: { assistant: phrase || null }, fragment: phrase ? undefined : 'assistant' });
  }

  iconeCategorie(code: string) {
    return code === 'AUTRE' ? 'plus' : icone(code);
  }
}
