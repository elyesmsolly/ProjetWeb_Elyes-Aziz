import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import qrcode from 'qrcode-generator';
import { AuthService } from '../../core/auth.service';
import { EnteteEcran } from '../../shared/entete-ecran';
import { Icon } from '../../shared/icon';

/** QR code d'accueil, dessiné en SVG dans le navigateur (aucun service externe). */
@Component({
  selector: 'app-qr-code',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, Icon],
  template: `
    <app-entete-ecran
      class="sans-impression"
      titre="QR code d’accueil"
      sousTitre="À afficher à l’entrée : vos clients le scannent et prennent leur ticket"
    />
    <div class="gestion">
      <section class="card affiche">
        <div class="logo"><span class="logo-mark" aria-hidden="true">D</span>Dourek</div>
        <h2>{{ auth.session()?.etablissementNom }}</h2>
        <p>Scannez pour faire la queue à distance</p>
        <svg
          class="qr"
          [attr.viewBox]="code().cadre"
          role="img"
          aria-label="QR code de votre page publique"
          shape-rendering="crispEdges"
        >
          <path [attr.d]="code().trace" fill="currentColor" />
        </svg>
        <p class="small muted adresse">{{ adresse }}</p>
      </section>
      @if (local) {
        <p class="card small sans-impression">
          Cette adresse ne marche que sur cet ordinateur. Pour un scan depuis un téléphone, ouvrez
          Dourek avec l’adresse IP du PC avant d’imprimer.
        </p>
      }
      <button type="button" class="btn sans-impression" (click)="imprimer()">
        <app-icon name="qr" />Imprimer l’affiche
      </button>
    </div>
  `,
  styles: `
    .affiche {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      text-align: center;
      padding: 32px 16px;
    }
    .qr {
      width: min(280px, 100%);
      color: var(--text);
      background: #fff;
    }
    .adresse {
      overflow-wrap: anywhere;
      max-width: 100%;
    }
  `,
})
export class QrCode {
  protected readonly auth = inject(AuthService);
  protected readonly local = ['localhost', '127.0.0.1'].includes(location.hostname);
  protected readonly adresse = `${location.origin}/etablissements/${this.auth.session()?.slug}`;
  protected readonly code = computed(() => {
    const qr = qrcode(0, 'M');
    qr.addData(this.adresse);
    qr.make();
    const taille = qr.getModuleCount();
    const marge = 4;
    let trace = '';
    for (let y = 0; y < taille; y++) {
      for (let x = 0; x < taille; x++)
        if (qr.isDark(y, x)) trace += `M${x + marge} ${y + marge}h1v1h-1z`;
    }
    return { trace, cadre: `0 0 ${taille + 2 * marge} ${taille + 2 * marge}` };
  });

  protected imprimer() {
    window.print();
  }
}
