import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { GestionApi } from '../../core/gestion-api.service';
import { Parametres as Fiche } from '../../core/gestion.models';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** « Paramètres » de l'établissement. */
@Component({
  selector: 'app-parametres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, EnteteEcran, EtatChargement],
  template: `
    <app-entete-ecran
      titre="Paramètres"
      sousTitre="Les informations affichées sur votre page publique"
    />
    <div class="gestion">
      @if (fiche(); as f) {
        <form class="card formulaire" (ngSubmit)="enregistrer()">
          <div>
            <label class="label" for="param-nom">Nom de l’établissement</label>
            <input
              id="param-nom"
              class="input"
              name="nom"
              maxlength="100"
              [ngModel]="f.nom"
              (ngModelChange)="changer({ nom: $event })"
            />
          </div>
          <div>
            <label class="label" for="param-tel">Téléphone</label>
            <input
              id="param-tel"
              class="input"
              type="tel"
              name="telephone"
              maxlength="25"
              [ngModel]="f.telephone"
              (ngModelChange)="changer({ telephone: $event })"
            />
          </div>
          <div>
            <label class="label" for="param-adresse">Adresse</label>
            <input
              id="param-adresse"
              class="input"
              name="adresse"
              maxlength="200"
              [ngModel]="f.adresse"
              (ngModelChange)="changer({ adresse: $event })"
            />
          </div>
          <div>
            <label class="label" for="param-quartier">Quartier</label>
            <input
              id="param-quartier"
              class="input"
              name="quartier"
              maxlength="80"
              [ngModel]="f.quartier"
              (ngModelChange)="changer({ quartier: $event })"
            />
          </div>
          <div>
            <label class="label" for="param-ouverture">Heure d’ouverture</label>
            <input
              id="param-ouverture"
              class="input"
              type="number"
              name="ouverture"
              min="0"
              max="23"
              [ngModel]="f.heureOuverture"
              (ngModelChange)="changer({ heureOuverture: $event })"
            />
          </div>
          <div>
            <label class="label" for="param-fermeture">Heure de fermeture</label>
            <input
              id="param-fermeture"
              class="input"
              type="number"
              name="fermeture"
              min="1"
              max="24"
              [ngModel]="f.heureFermeture"
              (ngModelChange)="changer({ heureFermeture: $event })"
            />
          </div>
          <div class="large">
            <label class="label" for="param-horaires"
              >Horaires affichés (une ligne par période)</label
            >
            <textarea
              id="param-horaires"
              class="input"
              name="horaires"
              maxlength="250"
              [ngModel]="f.horaires"
              (ngModelChange)="changer({ horaires: $event })"
            ></textarea>
          </div>
          <div class="large">
            <label class="label" for="param-description">Présentation</label>
            <textarea
              id="param-description"
              class="input"
              name="description"
              maxlength="250"
              [ngModel]="f.description"
              (ngModelChange)="changer({ description: $event })"
            ></textarea>
          </div>
          <button
            class="btn large"
            type="submit"
            [disabled]="ecran.occupe() || !f.nom.trim() || !f.adresse.trim()"
          >
            Enregistrer les paramètres
          </button>
        </form>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class Parametres {
  private readonly api = inject(GestionApi);
  private readonly auth = inject(AuthService);
  protected readonly fiche = signal<Fiche | null>(null);
  protected readonly ecran = creerEcran(
    () => this.api.parametres(),
    (f) => this.fiche.set(f),
  );

  constructor() {
    this.ecran.charger();
  }

  protected changer(c: Partial<Fiche>) {
    this.fiche.update((f) => (f ? { ...f, ...c } : f));
  }

  protected enregistrer() {
    const f = this.fiche();
    if (!f) return;
    this.ecran.agir(this.api.modifierParametres(f), 'Paramètres enregistrés.', () => {
      const s = this.auth.session();
      if (s) this.auth.ouvrir({ ...s, etablissementNom: f.nom.trim() });
    });
  }
}
