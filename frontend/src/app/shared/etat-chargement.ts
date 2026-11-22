import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** Squelette pendant le chargement, ou message d'erreur avec « Réessayer ». */
@Component({
  selector: 'app-etat-chargement',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    @if (erreur(); as e) {
      <div class="card" role="alert">
        <p class="error">{{ e }}</p>
        <button type="button" class="btn ghost sm reessayer" (click)="reessayer.emit()">
          Réessayer
        </button>
      </div>
    } @else {
      <div role="status">
        <span class="sr-only">Chargement…</span>
        @for (i of [1, 2, 3]; track i) {
          <div class="skeleton bloc"></div>
        }
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .bloc {
      height: 76px;
      margin-bottom: 12px;
    }
    .reessayer {
      margin-top: 12px;
    }
  `,
})
export class EtatChargement {
  readonly erreur = input<string | null>(null);
  readonly reessayer = output<void>();
}
