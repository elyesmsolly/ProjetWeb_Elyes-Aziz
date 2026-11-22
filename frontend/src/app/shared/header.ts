import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { DernierTicket } from '../core/dernier-ticket';
import { Icon } from './icon';

@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'menu.set(false)' },
  imports: [RouterLink, RouterLinkActive, Icon],
  template: ` <header class="header">
    <div class="container bar">
      <a class="logo" [routerLink]="auth.connecte() ? '/' : '/connexion'" title="Dourek"
        ><span class="logo-mark" aria-hidden="true">D</span>Dourek</a
      >
      <!-- Le clic remonte des liens (Entrée au clavier aussi) : il ne fait que refermer le menu. -->
      <!-- eslint-disable-next-line @angular-eslint/template/click-events-have-key-events, @angular-eslint/template/interactive-supports-focus -->
      <nav
        class="nav"
        aria-label="Navigation principale"
        [class.open]="menu()"
        (click)="menu.set(false)"
      >
        <!-- Sans compte, pas d'onglet vers le site : on se connecte d'abord. -->
        @if (auth.connecte()) {
          <a routerLink="/" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }"
            >Accueil</a
          >
          <a routerLink="/etablissements" routerLinkActive="on">Établissements</a>
        }
        @if (auth.client()) {
          <a routerLink="/ticket" routerLinkActive="on">Mes tickets</a>
        }
        @if (auth.admin()) {
          <a routerLink="/admin" routerLinkActive="on">Administration</a>
        } @else {
          <a routerLink="/pro" routerLinkActive="on">Espace pro</a>
        }
        <a routerLink="/aide" routerLinkActive="on">Aide</a>
        <div class="nav-actions">
          <span class="lang" title="La version arabe arrive bientôt">FR | ع</span>
          @if (auth.client()) {
            <a class="moi" routerLink="/profil" title="Mon profil"
              ><app-icon name="user" [size]="16" />{{ auth.session()?.nom }}</a
            >
            <button class="btn ghost sm" type="button" (click)="sortir()">Déconnexion</button>
          } @else if (auth.admin()) {
            <button class="btn ghost sm" type="button" (click)="sortir()">Déconnexion</button>
          } @else if (auth.pro()) {
            <a
              class="btn sm"
              [routerLink]="auth.gerant() ? '/pro/tableau-de-bord' : '/pro/employe'"
              >{{ auth.session()?.etablissementNom }}</a
            >
          } @else {
            <a class="btn ghost sm" routerLink="/connexion">Se connecter</a>
            <a class="btn sm" routerLink="/inscription">Créer un compte</a>
          }
        </div>
      </nav>
      <button
        class="burger"
        (click)="menu.set(!menu())"
        [attr.aria-expanded]="menu()"
        aria-label="Menu"
      >
        <app-icon [name]="menu() ? 'x' : 'menu'" [size]="22" />
      </button>
    </div>
  </header>`,
  styles: `
    .header {
      background: var(--surface);
      border-bottom: 1px solid var(--line);
      position: sticky;
      top: 0;
      z-index: 500;
    }
    .bar {
      height: 72px;
      display: flex;
      align-items: center;
      gap: 32px;
    }
    .nav {
      display: flex;
      align-items: center;
      gap: 24px;
      font-weight: 500;
      color: var(--muted);
      flex: 1;
    }
    .nav > a {
      position: relative;
      padding: 6px 0;
      transition: color var(--t-fast) ease;
    }
    .nav > a.on {
      color: var(--text);
      font-weight: 600;
    }
    .moi {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      color: var(--text);
      font-size: 14px;
    }
    .nav > a.on::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -2px;
      height: 2px;
      border-radius: 2px;
      background: var(--accent);
    }
    .nav-actions {
      margin-left: auto;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .lang {
      font-size: 13px;
      font-weight: 600;
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 6px 10px;
      cursor: default;
    }
    .burger {
      display: none;
      margin-left: auto;
      width: 44px;
      height: 44px;
      place-items: center;
      border-radius: 10px;
    }
    @media (hover: hover) {
      .nav > a:hover {
        color: var(--text);
      }
    }
    @media (max-width: 1120px) {
      .burger {
        display: grid;
      }
      .nav {
        position: absolute;
        top: 72px;
        left: 0;
        right: 0;
        background: var(--surface);
        border-bottom: 1px solid var(--line);
        flex-direction: column;
        align-items: stretch;
        gap: 0;
        padding: 8px 16px 16px;
        box-shadow: var(--shadow);
        opacity: 0;
        visibility: hidden;
        transform: translateY(-6px);
        pointer-events: none;
        transition:
          visibility var(--t-fast),
          opacity var(--t-fast) var(--ease-out),
          transform var(--t-fast) var(--ease-out);
      }
      .nav.open {
        visibility: visible;
        opacity: 1;
        transform: none;
        pointer-events: auto;
      }
      .nav > a {
        padding: 14px 4px;
        border-bottom: 1px solid var(--line-soft);
      }
      .nav > a.on::after {
        display: none;
      }
      .nav-actions {
        margin: 14px 0 0;
        flex-wrap: wrap;
      }
    }
  `,
})
export class Header {
  protected readonly auth = inject(AuthService);
  private readonly dernier = inject(DernierTicket);
  private readonly router = inject(Router);
  protected readonly menu = signal(false);

  sortir() {
    this.auth.fermer();
    this.dernier.oublier();
    this.router.navigateByUrl('/connexion');
  }
}
