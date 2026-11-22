import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { CompteApi } from '../../core/compte-api.service';
import { creerEcran } from '../../core/ecran';
import { Profil as DonneesProfil } from '../../core/gestion.models';
import { EtatChargement } from '../../shared/etat-chargement';
import { PageEntete } from '../../shared/page-entete';

/** « Mon profil » du sitemap. */
@Component({
  selector: 'app-profil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, EtatChargement, PageEntete],
  template: `
    <div class="container page-enter texte">
      <app-page-entete
        titre="Mon profil"
        intro="Votre nom et votre téléphone sont proposés à chaque nouveau ticket."
      />
      @if (ecran.donnees(); as p) {
        <form class="card formulaire" (ngSubmit)="enregistrer()">
          <div class="large">
            <span class="label">E-mail de connexion</span>
            <p class="muted">{{ p.email }}</p>
          </div>
          <div>
            <label class="label" for="profil-nom">Nom et prénom</label>
            <input
              id="profil-nom"
              class="input"
              name="nom"
              maxlength="120"
              autocomplete="name"
              [(ngModel)]="nom"
            />
          </div>
          <div>
            <label class="label" for="profil-tel">Téléphone</label>
            <input
              id="profil-tel"
              class="input"
              type="tel"
              name="telephone"
              maxlength="25"
              autocomplete="tel"
              [(ngModel)]="telephone"
            />
          </div>
          <h2 class="titre-carte large">Changer de mot de passe (facultatif)</h2>
          <div>
            <label class="label" for="profil-actuel">Mot de passe actuel</label>
            <input
              id="profil-actuel"
              class="input"
              type="password"
              name="actuel"
              autocomplete="current-password"
              [(ngModel)]="actuel"
            />
          </div>
          <div>
            <label class="label" for="profil-nouveau">Nouveau mot de passe</label>
            <input
              id="profil-nouveau"
              class="input"
              type="password"
              name="nouveau"
              minlength="6"
              maxlength="72"
              autocomplete="new-password"
              [(ngModel)]="nouveau"
            />
          </div>
          <button
            class="btn large"
            type="submit"
            [disabled]="
              ecran.occupe() ||
              !nom().trim() ||
              !telephone().trim() ||
              (nouveau() && (nouveau().length < 6 || !actuel()))
            "
          >
            {{ ecran.occupe() ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
        </form>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
  styles: `
    .texte {
      max-width: 820px;
    }
  `,
})
export class Profil {
  private readonly api = inject(CompteApi);
  private readonly auth = inject(AuthService);
  protected readonly nom = signal('');
  protected readonly telephone = signal('');
  protected readonly actuel = signal('');
  protected readonly nouveau = signal('');
  protected readonly ecran = creerEcran(
    () => this.api.profil(),
    (p: DonneesProfil) => {
      this.nom.set(p.nom);
      this.telephone.set(p.telephone ?? '');
    },
  );

  constructor() {
    this.ecran.charger();
  }

  protected enregistrer() {
    const nom = this.nom().trim();
    const telephone = this.telephone().trim();
    const nouveauMotDePasse = this.nouveau() || null;
    const appel = this.api.modifierProfil({
      nom,
      telephone,
      nouveauMotDePasse,
      motDePasseActuel: this.actuel() || null,
    });
    this.ecran.agir(appel, 'Profil enregistré.', () => {
      const s = this.auth.session();
      if (s) this.auth.ouvrir({ ...s, nom, telephone });
      this.actuel.set('');
      this.nouveau.set('');
    });
  }
}
