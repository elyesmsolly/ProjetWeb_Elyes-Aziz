import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ChampFormulaire } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { Constructeur, nouveauChamp } from '../../shared/constructeur';
import { Icon } from '../../shared/icon';
import { FormulaireService } from '../../shared/modeles';

@Component({
  selector: 'app-constructeur-formulaire',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Constructeur, Icon],
  template: `
  <div class="topbar">
    <div><h2>Formulaire client</h2><div class="small muted">Ce que vos clients remplissent avant de prendre un ticket</div></div>
    <span class="badge primary">Version {{ version() }} · publiée</span>
    @if (modifie()) { <span class="badge accent">Modifications non publiées</span> }
    <div class="row wrap actions">
      <div class="modeles">
        <button class="btn ghost sm" (click)="menuModeles.set(!menuModeles())" [attr.aria-expanded]="menuModeles()"><app-icon name="copy" [size]="14" />Partir d’un modèle</button>
        @if (menuModeles()) {
          <div class="card menu" role="menu">
            @for (m of modeles.liste; track m.nom) { <button role="menuitem" (click)="appliquer(m.champs)">{{ m.nom }}</button> }
          </div>
        }
      </div>
      <a class="btn ghost sm" [href]="'/etablissements/' + auth.session()?.slug" target="_blank"><app-icon name="eye" [size]="14" />Voir ma page</a>
      <button class="btn sm" (click)="publier()" [disabled]="!modifie() || envoi()">Publier la version {{ version() + 1 }}</button>
    </div>
  </div>
  <div class="main">
    @if (champs(); as c) {
      <app-constructeur [champs]="c" (champsChange)="champs.set($event)" [nomEtablissement]="auth.session()?.etablissementNom ?? ''" [adresse]="'localhost:4200/etablissements/' + auth.session()?.slug" />
      <div class="card small info"><b>Bon à savoir :</b> les clients déjà dans la file gardent la version qu’ils ont remplie (v{{ version() }}). Les nouveaux tickets utiliseront la v{{ version() + 1 }} après publication.</div>
    } @else {
      <div class="skeleton" style="height:420px"></div>
    }
  </div>`,
  styles: `
  .topbar { min-height: 72px; display: flex; align-items: center; flex-wrap: wrap; gap: 12px 16px; padding: 12px 32px; border-bottom: 1px solid var(--line); background: var(--surface); }
  .topbar h2 { font-size: 20px; }
  .actions { margin-left: auto; gap: 8px; }
  .modeles { position: relative; }
  .menu { position: absolute; top: 42px; left: 0; z-index: 50; padding: 6px; min-width: 220px; display: flex; flex-direction: column; animation: page-in 180ms var(--ease-out) both; transform-origin: top left; }
  .menu button { text-align: left; padding: 10px 12px; border-radius: 8px; font-size: 14px; }
  @media (hover: hover) { .menu button:hover { background: var(--tint); } }
  .main { padding: 28px 32px; display: flex; flex-direction: column; gap: 20px; }
  .info { padding: 14px 16px; }
  @media (max-width: 700px) { .topbar, .main { padding-inline: 16px; } }`,
})
export class ConstructeurFormulaire {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly modeles = inject(FormulaireService);

  protected readonly champs = signal<ChampFormulaire[] | null>(null);
  protected readonly publies = signal<string>('');
  protected readonly version = signal(0);
  protected readonly envoi = signal(false);
  protected readonly menuModeles = signal(false);
  protected readonly modifie = computed(() => this.champs() !== null && JSON.stringify(this.champs()) !== this.publies());

  constructor() {
    this.api.formulaire().subscribe({
      next: (f) => { this.champs.set(f.champs); this.publies.set(JSON.stringify(f.champs)); this.version.set(f.version); },
      error: (e) => this.toast.afficher(messageErreur(e), true),
    });
  }

  appliquer(champs: ChampFormulaire[]) {
    this.champs.set(champs.map((c) => (c.cle === 'nom' || c.cle === 'telephone' ? { ...c } : { ...nouveauChamp(c.type), label: c.label, options: [...c.options], obligatoire: c.obligatoire, aide: c.aide })));
    this.menuModeles.set(false);
  }

  publier() {
    const c = this.champs();
    if (!c || this.envoi()) return;
    this.envoi.set(true);
    this.api.publierFormulaire(c).subscribe({
      next: (f) => {
        this.version.set(f.version); this.champs.set(f.champs); this.publies.set(JSON.stringify(f.champs));
        this.envoi.set(false); this.toast.afficher(`Version ${f.version} publiée. Les nouveaux tickets l’utilisent déjà.`);
      },
      error: (e) => { this.envoi.set(false); this.toast.afficher(messageErreur(e), true); },
    });
  }
}
