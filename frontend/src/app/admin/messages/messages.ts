import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AdminApi } from '../../core/admin-api.service';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** Administration : Messages. */
@Component({
  selector: 'app-admin-messages',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, EtatChargement, BoutonConfirmer, DatePipe],
  template: `
    <app-entete-ecran titre="Messages" sousTitre="Reçus depuis la page Contact" />
    <div class="gestion">
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (m of liste; track m.id) {
            <li>
              <div class="grow">
                <b>{{ m.sujet }}</b>
                <p>{{ m.message }}</p>
                <div class="small muted">
                  {{ m.nom }} · {{ m.email }} · {{ m.creeLe | date: 'd MMM y, HH:mm' }}
                </div>
              </div>
              <app-bouton-confirmer
                [desactive]="ecran.occupe()"
                (confirme)="ecran.agir(api.supprimerMessage(m.id), 'Message supprimé.')"
              />
            </li>
          } @empty {
            <li class="muted">Aucun message.</li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class AdminMessages {
  protected readonly api = inject(AdminApi);
  protected readonly ecran = creerEcran(() => this.api.messages());

  constructor() {
    this.ecran.charger();
  }
}
