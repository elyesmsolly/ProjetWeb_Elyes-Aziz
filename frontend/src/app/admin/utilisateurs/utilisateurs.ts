import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AdminApi } from '../../core/admin-api.service';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** Administration : Utilisateurs & rôles. */
@Component({
  selector: 'app-admin-utilisateurs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, EtatChargement, BoutonConfirmer],
  template: `
    <app-entete-ecran
      titre="Utilisateurs & rôles"
      sousTitre="Changer un rôle ou suspendre un compte ferme sa session"
    />
    <div class="gestion">
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (u of liste; track u.id) {
            <li>
              <span class="avatar" aria-hidden="true">{{ u.nom[0] }}</span>
              <div class="grow">
                <b>{{ u.nom }}</b>
                <div class="small muted">
                  {{ u.email }}{{ u.etablissement ? ' · ' + u.etablissement : '' }}
                </div>
              </div>
              <span class="badge primary">{{ roles[u.role] }}</span>
              @if (u.role === 'GERANT' || u.role === 'EMPLOYE') {
                <button
                  class="btn ghost sm"
                  type="button"
                  [disabled]="ecran.occupe()"
                  [attr.aria-label]="
                    (u.role === 'GERANT' ? 'Passer employé' : 'Passer gérant') + ' : ' + u.nom
                  "
                  (click)="
                    ecran.agir(
                      api.changerRole(u.id, u.role === 'GERANT' ? 'EMPLOYE' : 'GERANT'),
                      'Rôle modifié.'
                    )
                  "
                >
                  {{ u.role === 'GERANT' ? 'Passer employé' : 'Passer gérant' }}
                </button>
              }
              @if (u.suspendu) {
                <button
                  class="btn ghost sm"
                  type="button"
                  [disabled]="ecran.occupe()"
                  [attr.aria-label]="'Réactiver ' + u.nom"
                  (click)="ecran.agir(api.suspendreUtilisateur(u.id, false), 'Compte réactivé.')"
                >
                  Réactiver
                </button>
              } @else if (u.role !== 'ADMIN') {
                <app-bouton-confirmer
                  libelle="Suspendre"
                  [description]="'Suspendre ' + u.nom"
                  [desactive]="ecran.occupe()"
                  (confirme)="ecran.agir(api.suspendreUtilisateur(u.id, true), 'Compte suspendu.')"
                />
              }
            </li>
          } @empty {
            <li class="muted">Aucun utilisateur.</li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class AdminUtilisateurs {
  protected readonly api = inject(AdminApi);
  protected readonly ecran = creerEcran(() => this.api.utilisateurs());
  protected readonly roles: Record<string, string> = {
    CLIENT: 'Client',
    GERANT: 'Gérant',
    EMPLOYE: 'Employé',
    ADMIN: 'Administrateur',
  };

  constructor() {
    this.ecran.charger();
  }
}
