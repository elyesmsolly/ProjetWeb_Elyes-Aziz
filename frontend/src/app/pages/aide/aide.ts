import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { PageEntete } from '../../shared/page-entete';

const RUBRIQUES = [
  {
    lien: '/aide/contact',
    icone: 'form',
    titre: 'Contact',
    texte: 'Une question ou un problème : écrivez à l’équipe.',
  },
  {
    lien: '/aide/faq',
    icone: 'list',
    titre: 'Questions fréquentes',
    texte: 'Tickets, attente, priorité, annulation et espace pro.',
  },
  {
    lien: '/aide/confidentialite',
    icone: 'eye',
    titre: 'Confidentialité',
    texte: 'Les données utilisées par Dourek et qui peut les voir.',
  },
];

/** Accueil de la branche « Aide » du sitemap. */
@Component({
  selector: 'app-aide',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, PageEntete],
  template: `
    <div class="container page-enter">
      <app-page-entete
        titre="Aide"
        intro="Tout pour prendre un ticket ou gérer votre file sans hésiter."
      />
      <div class="rubriques">
        @for (r of rubriques; track r.lien) {
          <a class="card rubrique" [routerLink]="r.lien">
            <app-icon [name]="r.icone" [size]="28" />
            <h2 class="titre-carte">{{ r.titre }}</h2>
            <p class="small muted">{{ r.texte }}</p>
          </a>
        }
      </div>
    </div>
  `,
  styles: `
    .rubriques {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(240px, 100%), 1fr));
      gap: 20px;
    }
    .rubrique {
      display: flex;
      flex-direction: column;
      gap: 10px;
      color: var(--primary);
    }
    .rubrique h2 {
      color: var(--text);
    }
  `,
})
export class Aide {
  protected readonly rubriques = RUBRIQUES;
}
