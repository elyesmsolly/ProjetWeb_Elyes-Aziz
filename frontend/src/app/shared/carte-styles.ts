import { ChangeDetectionStrategy, Component, ViewEncapsulation } from '@angular/core';

/** Le CSS de Leaflet n'est téléchargé qu'avec la carte, plus au démarrage du site. */
@Component({
  selector: 'app-carte-styles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Leaflet crée ses éléments hors d'Angular : ses styles ne peuvent pas être isolés.
  encapsulation: ViewEncapsulation.None,
  template: '',
  styleUrl: '../../../node_modules/leaflet/dist/leaflet.css',
})
export class CarteStyles {}
