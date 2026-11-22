import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Etoiles } from '../../shared/etoiles';
import { AdminApi } from '../../core/admin-api.service';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** Administration : Avis signalés. */
@Component({
  selector: 'app-admin-avis',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, EtatChargement, BoutonConfirmer, Etoiles],
  template: `
    <app-entete-ecran
      titre="Avis signalés"
      sousTitre="Conserver un avis ou le supprimer (la moyenne est recalculée)"
    />
    <div class="gestion">
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (a of liste; track a.id) {
            <li>
              <div class="grow">
                <b>{{ a.etablissement }}</b> <app-etoiles [note]="a.note" />
                <p>{{ a.commentaire || 'Sans commentaire' }}</p>
                <div class="small muted">Par {{ a.auteur }}</div>
              </div>
              <button
                class="btn ghost sm"
                type="button"
                [disabled]="ecran.occupe()"
                (click)="ecran.agir(api.conserverAvis(a.id), 'Avis conservé.')"
              >
                Conserver
              </button>
              <app-bouton-confirmer
                [desactive]="ecran.occupe()"
                (confirme)="ecran.agir(api.supprimerAvis(a.id), 'Avis supprimé.')"
              />
            </li>
          } @empty {
            <li class="muted">Aucun avis signalé.</li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class AdminAvis {
  protected readonly api = inject(AdminApi);
  protected readonly ecran = creerEcran(() => this.api.avis());

  constructor() {
    this.ecran.charger();
  }
}
