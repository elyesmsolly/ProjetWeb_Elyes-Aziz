import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../core/api.service';
import { CompteApi } from '../core/compte-api.service';
import { Avis } from '../core/gestion.models';
import { Etoiles } from './etoiles';

/** « Donner son avis » : dernière étape du parcours client dans le sitemap. */
@Component({
  selector: 'app-avis-ticket',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Etoiles],
  template: `
    <section class="card">
      @if (avis(); as a) {
        <h2 class="titre-carte">Merci pour votre avis</h2>
        <app-etoiles [note]="a.note" />
        @if (a.commentaire) {
          <p class="small muted">{{ a.commentaire }}</p>
        }
      } @else if (pret()) {
        <h2 class="titre-carte">Donner mon avis</h2>
        <form (ngSubmit)="envoyer()">
          <fieldset>
            <legend class="small muted">Comment s’est passé votre passage ?</legend>
            @for (n of valeurs; track n) {
              <label class="choix" [class.on]="n <= note()">
                <input
                  class="sr-only"
                  type="radio"
                  name="note"
                  [value]="n"
                  [ngModel]="note()"
                  (ngModelChange)="note.set($event)"
                />
                <span aria-hidden="true">{{ n <= note() ? '★' : '☆' }}</span>
                <span class="sr-only">{{ n }} sur 5</span>
              </label>
            }
          </fieldset>
          <label class="label" for="avis-texte">Commentaire (facultatif)</label>
          <textarea
            id="avis-texte"
            class="input"
            name="commentaire"
            maxlength="500"
            [(ngModel)]="commentaire"
          ></textarea>
          @if (erreur()) {
            <p class="error" role="alert">{{ erreur() }}</p>
          }
          <button class="btn envoyer" type="submit" [disabled]="!note() || envoi()">
            {{ envoi() ? 'Envoi…' : 'Publier mon avis' }}
          </button>
        </form>
      } @else if (erreur()) {
        <p class="error" role="alert">{{ erreur() }}</p>
      } @else {
        <p class="small muted" role="status">Chargement de votre avis…</p>
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
    }
    fieldset {
      border: 0;
      margin: 8px 0;
    }
    .choix {
      display: inline-grid;
      place-items: center;
      width: 44px;
      height: 44px;
      font-size: 30px;
      color: var(--muted);
      cursor: pointer;
      border-radius: 8px;
    }
    .choix.on {
      color: var(--warn);
    }
    .choix:focus-within {
      outline: 2.5px solid var(--accent);
    }
    .envoyer {
      margin-top: 12px;
    }
  `,
})
export class AvisTicket {
  readonly code = input.required<string>();
  private readonly api = inject(CompteApi);
  protected readonly valeurs = [1, 2, 3, 4, 5];
  protected readonly avis = signal<Avis | null>(null);
  protected readonly pret = signal(false);
  protected readonly note = signal(0);
  protected readonly commentaire = signal('');
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.api.avisDuTicket(this.code()).subscribe({
        next: (a) => {
          this.avis.set(a);
          this.pret.set(true);
        },
        error: (e) => this.erreur.set(messageErreur(e)),
      });
    });
  }

  protected envoyer() {
    if (!this.note() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set(null);
    this.api.donnerAvis(this.code(), this.note(), this.commentaire()).subscribe({
      next: (a) => {
        this.avis.set(a);
        this.envoi.set(false);
      },
      error: (e) => {
        this.erreur.set(messageErreur(e));
        this.envoi.set(false);
      },
    });
  }
}
