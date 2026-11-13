import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { badgeAttente, distance, icone, note } from '../../core/format';
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

  protected readonly service = signal('');
  protected readonly lieu = signal('Ariana, Tunis');
  protected readonly categories = toSignal(this.api.categories().pipe(map((cs) => cs.filter((c) => !c.libre)), catchError(() => of([]))), { initialValue: [] });
  protected readonly proches = toSignal(this.api.rechercher().pipe(catchError(() => of(null))), { initialValue: undefined });

  protected readonly fmt = { distance, note, badgeAttente, icone };
  protected readonly exemple = 'Je veux boire quelque chose';

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
