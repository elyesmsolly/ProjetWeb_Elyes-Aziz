import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApi } from '../../core/admin-api.service';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** Administration : Catégories. */
@Component({
  selector: 'app-admin-categories',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EnteteEcran, EtatChargement, FormsModule],
  template: `
    <app-entete-ecran
      titre="Catégories"
      sousTitre="Les types créés par les gérants peuvent être renommés ou fusionnés"
    />
    <div class="gestion">
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (c of liste; track $index) {
            <li>
              @if (c.libre && edition() === c.libelle) {
                <form class="ligne-form grow" (ngSubmit)="renommer(c.libelle)">
                  <div>
                    <label class="label" for="nouveau-nom"
                      >Nouveau nom de « {{ c.libelle }} »</label
                    >
                    <input
                      id="nouveau-nom"
                      class="input"
                      name="nouveau"
                      maxlength="40"
                      [(ngModel)]="nouveau"
                    />
                  </div>
                  <button
                    class="btn sm"
                    type="submit"
                    [disabled]="ecran.occupe() || !nouveau().trim()"
                  >
                    Enregistrer
                  </button>
                  <button class="btn ghost sm" type="button" (click)="edition.set(null)">
                    Annuler
                  </button>
                </form>
              } @else {
                <div class="grow">
                  <b>{{ c.libelle }}</b>
                  <div class="small muted">
                    {{ c.libre ? 'Créée par un gérant' : 'Catégorie fixe de la plateforme' }}
                  </div>
                </div>
                <span class="badge">{{ c.nombre }} établissement{{ c.nombre > 1 ? 's' : '' }}</span>
                @if (c.libre) {
                  <button
                    class="btn ghost sm"
                    type="button"
                    [attr.aria-label]="'Renommer ' + c.libelle"
                    (click)="edition.set(c.libelle); nouveau.set(c.libelle)"
                  >
                    Renommer
                  </button>
                }
              }
            </li>
          } @empty {
            <li class="muted">Aucune catégorie.</li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class AdminCategories {
  protected readonly api = inject(AdminApi);
  protected readonly ecran = creerEcran(() => this.api.categories());
  protected readonly edition = signal<string | null>(null);
  protected readonly nouveau = signal('');

  protected renommer(ancien: string) {
    this.ecran.agir(
      this.api.renommerCategorie(ancien, this.nouveau().trim()),
      'Catégorie renommée.',
      () => this.edition.set(null),
    );
  }

  constructor() {
    this.ecran.charger();
  }
}
