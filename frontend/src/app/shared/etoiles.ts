import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Note affichée en étoiles, avec le chiffre pour les lecteurs d'écran. */
@Component({
  selector: 'app-etoiles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    <span aria-hidden="true"
      >{{ '★★★★★'.slice(0, note())
      }}<span class="vides">{{ '☆☆☆☆☆'.slice(0, 5 - note()) }}</span></span
    >
    <span class="sr-only">{{ note() }} sur 5</span>
  `,
  styles: `
    :host {
      color: var(--warn);
      letter-spacing: 2px;
    }
    .vides {
      color: var(--muted);
    }
  `,
})
export class Etoiles {
  readonly note = input.required<number>();
}
