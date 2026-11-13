import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
  <footer class="footer">
    <div class="container grid">
      <div>
        <div class="logo"><span class="logo-mark">D</span>Dourek</div>
        <p>Faites la queue depuis chez vous.<br />Projet Web · SUP'COM 2026/2027<br />Med Elyes Msolly · Med Aziz Ghazouani</p>
      </div>
      <div><h2>Clients</h2><ul>
        <li><a routerLink="/etablissements">Trouver un établissement</a></li>
        <li><a routerLink="/ticket">Suivre mon ticket</a></li>
        <li><a routerLink="/etablissements" fragment="assistant">Assistant de recherche</a></li></ul></div>
      <div><h2>Professionnels</h2><ul>
        <li><a routerLink="/pro/inscription">Créer ma file</a></li>
        <li><a routerLink="/pro/tableau-de-bord">Tableau de bord</a></li>
        <li><a routerLink="/pro/employe">Écran employé</a></li></ul></div>
      <div><h2>Aide</h2><ul><li>FAQ</li><li>Contact</li><li>Confidentialité</li></ul></div>
      <div class="copy"><span>© 2026 Dourek</span><span>Français · العربية (bientôt)</span></div>
    </div>
  </footer>`,
  styles: `
  .footer { background: var(--led); color: #b3c2ad; margin-top: 72px; }
  .grid { padding: 44px 0 24px; display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 32px; }
  .logo { color: #fff; margin-bottom: 12px; }
  .logo-mark::after { border-color: var(--led); }
  h2 { color: #fff; font-size: 14px; letter-spacing: 0; margin-bottom: 12px; font-family: var(--font-display); }
  p, li { font-size: 13px; list-style: none; line-height: 2; }
  a { transition: color var(--t-fast) ease; }
  @media (hover: hover) { a:hover { color: #fff; } }
  .copy { grid-column: 1 / -1; border-top: 1px solid rgba(255,255,255,.15); padding-top: 16px; font-size: 12px; display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
  @media (max-width: 760px) { .grid { grid-template-columns: 1fr 1fr; } .grid > div:first-child { grid-column: 1 / -1; } }`,
})
export class Footer {}
