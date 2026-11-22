import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageEntete } from '../../shared/page-entete';
import { QUESTIONS } from './questions';

/** FAQ en accordéon natif (details / summary) : utilisable au clavier sans code en plus. */
@Component({
  selector: 'app-faq',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, PageEntete],
  template: `
    <div class="container page-enter texte">
      <app-page-entete
        titre="Questions fréquentes"
        intro="Les réponses courtes aux questions les plus courantes."
      />
      @for (g of groupes; track g.theme) {
        <section>
          <h2>{{ g.theme }}</h2>
          @for (x of g.questions; track x.q) {
            <details>
              <summary>{{ x.q }}</summary>
              <p>{{ x.r }}</p>
            </details>
          }
        </section>
      }
      <a class="btn ghost" routerLink="/aide">Retour à l’aide</a>
    </div>
  `,
  styles: `
    .texte {
      max-width: 820px;
    }
    section {
      margin-bottom: 32px;
    }
    h2 {
      margin-bottom: 12px;
    }
    details {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      margin-bottom: 8px;
    }
    summary {
      padding: 14px 16px;
      min-height: 48px;
      font-weight: 600;
      cursor: pointer;
    }
    details p {
      padding: 0 16px 16px;
      color: var(--muted);
      animation: page-in var(--t-ui) var(--ease-out);
    }
  `,
})
export class Faq {
  protected readonly groupes = QUESTIONS;
}
