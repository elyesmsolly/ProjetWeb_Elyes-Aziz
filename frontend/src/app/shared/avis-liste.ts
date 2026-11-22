import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { messageErreur } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { CompteApi } from '../core/compte-api.service';
import { Avis } from '../core/gestion.models';
import { ToastService } from '../core/toast.service';
import { Etoiles } from './etoiles';

/** Avis publics d'un établissement, avec signalement à l'administration. */
@Component({
  selector: 'app-avis-liste',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, Etoiles],
  template: `
    <section class="card">
      <h2 class="titre-carte">Avis des clients</h2>
      @if (erreur()) {
        <p class="error" role="alert">{{ erreur() }}</p>
      } @else {
        @for (a of liste() ?? []; track a.id) {
          <article>
            <div class="row between wrap">
              <b>{{ a.auteur }}</b><app-etoiles [note]="a.note" />
            </div>
            @if (a.commentaire) {
              <p>{{ a.commentaire }}</p>
            }
            <div class="row between wrap small muted">
              <span>{{ a.creeLe | date: 'd MMMM y' }}</span>
              @if (signales().has(a.id) || a.signale) {
                <span>Signalé</span>
              } @else if (auth.connecte()) {
                <button type="button" class="btn link small" (click)="signaler(a.id)">
                  Signaler cet avis
                </button>
              }
            </div>
          </article>
        } @empty {
          <p class="small muted">
            {{ liste() ? 'Aucun avis écrit sur Dourek pour le moment.' : 'Chargement des avis…' }}
          </p>
        }
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
    }
    article {
      padding: 12px 0;
      border-bottom: 1px solid var(--line-soft);
      overflow-wrap: anywhere;
    }
    article:last-child {
      border: 0;
    }
  `,
})
export class AvisListe {
  readonly slug = input.required<string>();
  private readonly api = inject(CompteApi);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly liste = signal<Avis[] | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly signales = signal(new Set<number>());

  constructor() {
    effect(() => {
      this.api.avis(this.slug()).subscribe({
        next: (l) => this.liste.set(l),
        error: (e) => this.erreur.set(messageErreur(e)),
      });
    });
  }

  protected signaler(id: number) {
    this.api.signaler(id).subscribe({
      next: () => {
        this.signales.update((s) => new Set(s).add(id));
        this.toast.afficher('Avis signalé à l’administration.');
      },
      error: (e) => this.toast.afficher(messageErreur(e), true),
    });
  }
}
