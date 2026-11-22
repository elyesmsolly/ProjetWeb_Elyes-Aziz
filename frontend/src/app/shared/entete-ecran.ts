import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Bandeau de titre des écrans de gestion. */
@Component({
  selector: 'app-entete-ecran',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    <h1 class="titre-page">{{ titre() }}</h1>
    <p class="small muted">{{ sousTitre() }}</p>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-height: 72px;
      padding: 12px 32px;
      background: var(--surface);
      border-bottom: 1px solid var(--line);
    }
    @media (max-width: 700px) {
      :host {
        padding-inline: 16px;
      }
    }
  `,
})
export class EnteteEcran {
  readonly titre = input.required<string>();
  readonly sousTitre = input('');
}
