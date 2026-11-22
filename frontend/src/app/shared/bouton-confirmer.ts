import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';

/** Action destructive en deux temps, sans fenêtre bloquante. */
@Component({
  selector: 'app-bouton-confirmer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    @if (demande()) {
      <button type="button" class="btn danger sm" [disabled]="desactive()" (click)="valider()">
        Confirmer
      </button>
      <button type="button" class="btn ghost sm" (click)="demande.set(false)">Garder</button>
    } @else {
      <button
        type="button"
        class="btn danger sm"
        [disabled]="desactive()"
        [attr.aria-label]="description() || null"
        (click)="demande.set(true)"
      >
        {{ libelle() }}
      </button>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  `,
})
export class BoutonConfirmer {
  readonly libelle = input('Supprimer');
  readonly description = input('');
  readonly desactive = input(false);
  readonly confirme = output<void>();
  protected readonly demande = signal(false);

  protected valider() {
    this.demande.set(false);
    this.confirme.emit();
  }
}
