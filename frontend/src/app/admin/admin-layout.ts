import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Icon } from '../shared/icon';

const ONGLETS = [
  { lien: 'etablissements', icone: 'store', texte: 'Établissements' },
  { lien: 'utilisateurs', icone: 'users', texte: 'Utilisateurs & rôles' },
  { lien: 'avis', icone: 'star', texte: 'Avis signalés' },
  { lien: 'categories', icone: 'list', texte: 'Catégories' },
  { lien: 'messages', icone: 'form', texte: 'Messages' },
];

/** Coquille de la branche « Administration » du sitemap. */
@Component({
  selector: 'app-admin-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  template: `
    <header>
      <div class="haut">
        <a class="logo" routerLink="/"><span class="logo-mark" aria-hidden="true">D</span>Dourek</a>
        <span class="badge primary">Administration</span>
        <button type="button" class="btn ghost sm sortir" (click)="sortir()">
          <app-icon name="logout" [size]="14" />Déconnexion
        </button>
      </div>
      <nav aria-label="Administration">
        @for (o of onglets; track o.lien) {
          <a [routerLink]="o.lien" routerLinkActive="on" ariaCurrentWhenActive="page"
            ><app-icon [name]="o.icone" [size]="16" />{{ o.texte }}</a
          >
        }
      </nav>
    </header>
    <main id="contenu" tabindex="-1"><router-outlet /></main>
  `,
  styles: `
    header {
      background: var(--surface);
      border-bottom: 1px solid var(--line);
    }
    .haut {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
      padding: 14px 32px;
    }
    .sortir {
      margin-left: auto;
    }
    nav {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      padding: 0 32px 12px;
    }
    nav a {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 40px;
      padding: 8px 12px;
      border-radius: var(--radius);
      color: var(--muted);
      font-weight: 500;
      transition: background-color var(--t-fast) ease;
    }
    nav a.on {
      background: var(--primary);
      color: var(--primary-ink);
    }
    @media (max-width: 700px) {
      .haut,
      nav {
        padding-inline: 16px;
      }
    }
  `,
})
export class AdminLayout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly onglets = ONGLETS;

  protected sortir() {
    this.auth.fermer();
    this.router.navigateByUrl('/pro/connexion');
  }
}
