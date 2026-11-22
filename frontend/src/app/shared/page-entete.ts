import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Titre et introduction communs aux pages d'information. */
@Component({
  selector: 'app-page-entete',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>{{ titre() }}</h1>
    <p class="muted intro">{{ intro() }}</p>
  `,
  styles: `
    :host {
      display: block;
      padding-block: 48px 28px;
    }
    h1 {
      font-size: clamp(28px, 4vw, 40px);
    }
    .intro {
      margin-top: 10px;
      max-width: 620px;
      font-size: 16px;
    }
  `,
})
export class PageEntete {
  readonly titre = input.required<string>();
  readonly intro = input.required<string>();
}
