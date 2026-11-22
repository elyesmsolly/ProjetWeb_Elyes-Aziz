import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Adresse inconnue : on l'explique au lieu de renvoyer en silence vers l'accueil. */
@Component({
  selector: 'app-introuvable',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="container introuvable page-enter">
      <p class="display code" aria-hidden="true">404</p>
      <h1>Cette page n’existe pas</h1>
      <p class="muted">L’adresse est peut-être incomplète, ou la page a été déplacée.</p>
      <a class="btn lg" routerLink="/">Revenir à l’accueil</a>
    </div>
  `,
  styles: `
    .introuvable {
      padding-block: 72px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      text-align: center;
    }
    .code {
      font-size: 96px;
      font-weight: 800;
      color: var(--primary);
      line-height: 1;
    }
  `,
})
export class Introuvable {}
