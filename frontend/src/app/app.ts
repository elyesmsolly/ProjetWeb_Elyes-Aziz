import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: ` <button type="button" class="evitement" (click)="allerAuContenu()">
      Aller au contenu
    </button>
    @if (navigation()) {
      <div class="chargement-page" role="progressbar" aria-label="Chargement de la page"></div>
    }
    <router-outlet />
    <!-- La zone d'annonce existe avant le message, sinon les lecteurs d'écran ne le lisent pas. -->
    <div role="status" aria-live="polite">
      @if (toast.message(); as m) {
        <div class="toast" [class.error]="m.erreur">{{ m.texte }}</div>
      }
    </div>`,
})
export class App {
  protected readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  /** Vrai pendant le téléchargement d'une page chargée à la demande. */
  protected readonly navigation = computed(() => this.router.currentNavigation() !== null);

  protected allerAuContenu() {
    document.getElementById('contenu')?.focus();
  }
}
