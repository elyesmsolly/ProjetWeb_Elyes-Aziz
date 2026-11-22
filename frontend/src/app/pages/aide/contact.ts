import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CompteApi } from '../../core/compte-api.service';
import { PageEntete } from '../../shared/page-entete';

/** Le message est enregistré, puis lu dans Administration > Messages. */
@Component({
  selector: 'app-contact',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, PageEntete],
  template: `
    <div class="container page-enter texte">
      <app-page-entete
        titre="Contact"
        intro="Une question ou un problème ? Écrivez à l’équipe Dourek."
      />
      @if (envoye()) {
        <div class="card merci" role="status">
          <h2>Message envoyé</h2>
          <p class="muted">L’administration de Dourek peut maintenant le lire.</p>
          <a class="btn" routerLink="/aide">Retour à l’aide</a>
        </div>
      } @else {
        <form class="card formulaire" (ngSubmit)="envoyer()">
          <div>
            <label class="label" for="contact-nom">Votre nom</label>
            <input
              id="contact-nom"
              class="input"
              name="nom"
              maxlength="120"
              autocomplete="name"
              [(ngModel)]="nom"
            />
          </div>
          <div>
            <label class="label" for="contact-email">Votre e-mail</label>
            <input
              id="contact-email"
              class="input"
              type="email"
              name="email"
              maxlength="160"
              autocomplete="email"
              [(ngModel)]="email"
            />
          </div>
          <div class="large">
            <label class="label" for="contact-sujet">Sujet</label>
            <input
              id="contact-sujet"
              class="input"
              name="sujet"
              maxlength="120"
              [(ngModel)]="sujet"
            />
          </div>
          <div class="large">
            <label class="label" for="contact-message">Message (10 caractères au moins)</label>
            <textarea
              id="contact-message"
              class="input"
              name="message"
              maxlength="2000"
              [(ngModel)]="message"
            ></textarea>
          </div>
          @if (erreur()) {
            <p class="error large" role="alert">{{ erreur() }}</p>
          }
          <button class="btn large" type="submit" [disabled]="envoi()">
            {{ envoi() ? 'Envoi…' : 'Envoyer mon message' }}
          </button>
        </form>
      }
    </div>
  `,
  styles: `
    .texte {
      max-width: 820px;
    }
    .merci {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }
  `,
})
export class Contact {
  private readonly api = inject(CompteApi);
  private readonly session = inject(AuthService).session();
  protected readonly nom = signal(this.session?.nom ?? '');
  protected readonly email = signal(this.session?.email ?? '');
  protected readonly sujet = signal('');
  protected readonly message = signal('');
  protected readonly envoi = signal(false);
  protected readonly envoye = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected envoyer() {
    if (this.envoi()) return;
    const c = {
      nom: this.nom().trim(),
      email: this.email().trim(),
      sujet: this.sujet().trim(),
      message: this.message().trim(),
    };
    if (
      !c.nom ||
      !c.sujet ||
      c.message.length < 10 ||
      !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(c.email)
    ) {
      this.erreur.set(
        'Remplissez tous les champs, avec un e-mail valide et un message de 10 caractères au moins.',
      );
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);
    this.api.contacter(c).subscribe({
      next: () => {
        this.envoye.set(true);
        this.envoi.set(false);
      },
      error: (e) => {
        this.erreur.set(messageErreur(e));
        this.envoi.set(false);
      },
    });
  }
}
