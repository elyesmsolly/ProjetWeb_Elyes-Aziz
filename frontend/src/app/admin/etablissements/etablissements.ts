import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AdminApi } from '../../core/admin-api.service';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** Administration : Établissements. */
@Component({
  selector: 'app-admin-etablissements',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, EtatChargement, BoutonConfirmer],
  template: `
    <app-entete-ecran
      titre="Établissements"
      sousTitre="Un établissement suspendu disparaît de la recherche"
    />
    <div class="gestion">
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (e of liste; track e.id) {
            <li>
              <div class="grow">
                <b>{{ e.nom }}</b>
                <div class="small muted">{{ e.categorie }} · {{ e.quartier }}</div>
              </div>
              @if (e.suspendu) {
                <span class="badge danger">Suspendu</span>
                <button
                  class="btn ghost sm"
                  type="button"
                  [disabled]="ecran.occupe()"
                  [attr.aria-label]="'Réactiver ' + e.nom"
                  (click)="
                    ecran.agir(api.suspendreEtablissement(e.id, false), 'Établissement réactivé.')
                  "
                >
                  Réactiver
                </button>
              } @else {
                <span class="badge ok">{{
                  e.fileOuverte ? 'En ligne' : 'En ligne · file en pause'
                }}</span>
                <app-bouton-confirmer
                  libelle="Suspendre"
                  [description]="'Suspendre ' + e.nom"
                  [desactive]="ecran.occupe()"
                  (confirme)="
                    ecran.agir(api.suspendreEtablissement(e.id, true), 'Établissement suspendu.')
                  "
                />
              }
            </li>
          } @empty {
            <li class="muted">Aucun établissement.</li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class AdminEtablissements {
  protected readonly api = inject(AdminApi);
  protected readonly ecran = creerEcran(() => this.api.etablissements());

  constructor() {
    this.ecran.charger();
  }
}
