import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Icon } from './icon';

/** Espace gérant : barre latérale de la maquette + contenu. */
@Component({
  selector: 'app-pro-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  template: `
  <div class="app">
    <aside class="side" [class.open]="menu()" (click)="menu.set(false)">
      <a class="logo" routerLink="/"><span class="logo-mark">D</span>Dourek <span class="badge primary">Pro</span></a>
      <div class="sep">Établissement</div>
      <a routerLink="/pro/tableau-de-bord" routerLinkActive="on"><app-icon name="chart" />Tableau de bord</a>
      <a routerLink="/pro/employe"><app-icon name="users" />File en direct</a>
      <a routerLink="/pro/formulaire" routerLinkActive="on"><app-icon name="form" />Formulaire client</a>
      <a [routerLink]="['/etablissements', auth.session()?.slug]"><app-icon name="eye" />Ma page publique</a>
      <div class="sep">Outils</div>
      <a routerLink="/pro/employe"><app-icon name="monitor" />Écran employé</a>
      <span class="soon"><app-icon name="qr" />QR code d’accueil <small>bientôt</small></span>
      <div class="fill"></div>
      <button (click)="sortir()"><app-icon name="logout" />Déconnexion</button>
    </aside>
    <div class="content">
      <button class="burger btn ghost sm" (click)="menu.set(!menu())" aria-label="Menu de l’espace pro"><app-icon name="menu" />Menu</button>
      <router-outlet />
    </div>
  </div>`,
  styles: `
  .app { display: grid; grid-template-columns: 240px 1fr; min-height: 100vh; }
  .side { background: var(--surface); border-right: 1px solid var(--line); padding: 20px 16px; display: flex; flex-direction: column; gap: 4px; position: sticky; top: 0; height: 100vh; }
  .side .logo { margin: 0 8px 20px; font-size: 20px; }
  .side .badge { margin-left: 2px; }
  .side > a, .side > button, .soon { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: var(--radius); font-weight: 500; color: var(--muted); text-align: left; transition: background-color var(--t-fast) ease, color var(--t-fast) ease; }
  .side > a.on { background: var(--primary); color: #fff; font-weight: 600; }
  .side > a.on app-icon { color: var(--accent); }
  .soon { opacity: .6; } .soon small { margin-left: auto; font-size: 11px; }
  .sep { margin: 14px 12px 4px; font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); font-weight: 700; }
  .fill { flex: 1; }
  .content { min-width: 0; }
  .burger { display: none; }
  @media (hover: hover) { .side > a:not(.on):hover, .side > button:hover { background: var(--tint); color: var(--text); } }
  @media (max-width: 900px) {
    .app { grid-template-columns: 1fr; }
    .side { position: fixed; z-index: 600; left: 0; top: 0; width: 260px; transform: translateX(-100%); transition: transform var(--t-ui) var(--ease-out); box-shadow: var(--shadow-lift); }
    .side.open { transform: none; }
    .burger { display: inline-flex; margin: 12px 16px 0; }
  }`,
})
export class ProLayout {
  protected readonly auth = inject(AuthService);
  private router = inject(Router);
  protected readonly menu = signal(false);

  sortir() {
    this.auth.fermer();
    this.router.navigateByUrl('/pro/connexion');
  }
}
