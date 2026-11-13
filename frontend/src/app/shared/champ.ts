import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChampFormulaire } from '../core/models';

/** Affiche un champ du formulaire défini par le gérant (fiche établissement et aperçu du constructeur). */
@Component({
  selector: 'app-champ',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  template: `
  @let c = champ();
  @if (c.type === 'CASE') {
    <label class="case">
      <input type="checkbox" [ngModel]="valeur() === true" (ngModelChange)="valeur.set($event)" [name]="c.cle" [disabled]="apercu()" />
      <span>{{ c.label }} @if (c.obligatoire) { <span class="req">*</span> }</span>
    </label>
  } @else {
    <label class="label" [attr.for]="id()">{{ c.label }} @if (c.obligatoire) { <span class="req">*</span> }</label>
    @switch (c.type) {
      @case ('TEXTE_LONG') {
        <textarea class="input" [id]="id()" [name]="c.cle" [ngModel]="valeur()" (ngModelChange)="valeur.set($event)" [placeholder]="c.aide ?? ''" [readonly]="apercu()" maxlength="500"></textarea>
      }
      @case ('CHOIX') {
        <div class="seg" role="radiogroup" [attr.aria-label]="c.label" [style.--n]="c.options.length > 3 ? 2 : c.options.length">
          @for (o of c.options; track o) {
            <button type="button" role="radio" [attr.aria-checked]="valeur() === o" [class.on]="valeur() === o" (click)="!apercu() && valeur.set(o)">{{ o }}</button>
          }
        </div>
      }
      @case ('LISTE') {
        <select class="input" [id]="id()" [name]="c.cle" [ngModel]="valeur() ?? ''" (ngModelChange)="valeur.set($event)" [disabled]="apercu()">
          <option value="" disabled>Choisir…</option>
          @for (o of c.options; track o) { <option [value]="o">{{ o }}</option> }
        </select>
      }
      @default {
        <input class="input" [id]="id()" [name]="c.cle" [type]="typeHtml()" [ngModel]="valeur()" (ngModelChange)="valeur.set($event)"
          [placeholder]="c.type === 'TELEPHONE' ? '+216 •• ••• •••' : (c.aide ?? '')" [readonly]="apercu()"
          [attr.inputmode]="c.type === 'TELEPHONE' ? 'tel' : c.type === 'NOMBRE' ? 'numeric' : null"
          [attr.autocomplete]="c.cle === 'nom' ? 'name' : c.type === 'TELEPHONE' ? 'tel' : null" />
      }
    }
    @if (c.aide && c.type !== 'TEXTE_LONG' && c.type !== 'TEXTE') { <div class="small muted aide">{{ c.aide }}</div> }
  }`,
  styles: `
  :host { display: block; }
  .seg { display: grid; grid-template-columns: repeat(var(--n, 2), 1fr); gap: 8px; }
  .seg button { min-height: 44px; padding: 6px 10px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); font-size: 14px; font-weight: 500;
    transition: border-color var(--t-fast) ease, color var(--t-fast) ease, transform var(--t-press) var(--ease-out); }
  .seg button:active { transform: scale(0.97); }
  .seg button.on { border-color: var(--primary); color: var(--primary); font-weight: 700; outline: 1.5px solid var(--primary); background: var(--tint); }
  .case { display: flex; gap: 10px; align-items: flex-start; font-size: 14px; cursor: pointer; }
  .case input { width: 18px; height: 18px; accent-color: var(--primary); margin-top: 2px; flex: none; }
  .aide { margin-top: 4px; }`,
})
export class Champ {
  readonly champ = input.required<ChampFormulaire>();
  readonly valeur = model<unknown>(null);
  readonly apercu = input(false);
  protected id() { return 'champ-' + this.champ().cle; }
  protected typeHtml() {
    return ({ TELEPHONE: 'tel', NOMBRE: 'number', DATE: 'date' } as Record<string, string>)[this.champ().type] ?? 'text';
  }
}
