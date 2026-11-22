import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GestionApi } from '../../core/gestion-api.service';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** « Postes & employés » du tableau de bord. */
@Component({
  selector: 'app-equipe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, BoutonConfirmer, EnteteEcran, EtatChargement],
  template: `
    <app-entete-ecran
      titre="Postes & employés"
      sousTitre="Qui sert vos clients, et sur combien de postes"
    />
    <div class="gestion">
      @if (ecran.donnees(); as e) {
        <form class="card ligne-form" (ngSubmit)="changerPostes()">
          <div>
            <label class="label" for="nb-postes">Postes ouverts en même temps</label>
            <input
              id="nb-postes"
              class="input"
              type="number"
              name="postes"
              min="1"
              max="20"
              [(ngModel)]="postes"
            />
          </div>
          <button
            class="btn"
            type="submit"
            [disabled]="ecran.occupe() || postes() === e.postesOuverts"
          >
            Enregistrer
          </button>
        </form>
        <ul class="card lignes">
          @for (m of e.membres; track m.id) {
            <li>
              <span class="avatar" aria-hidden="true">{{ m.nom[0] }}</span>
              <div class="grow">
                <b>{{ m.nom }}</b>
                <div class="small muted">{{ m.email }}</div>
              </div>
              <span class="badge">{{ m.poste }}</span>
              <span class="badge primary">{{ m.role === 'GERANT' ? 'Gérant' : 'Employé' }}</span>
              @if (m.role === 'EMPLOYE') {
                <app-bouton-confirmer
                  libelle="Retirer"
                  [description]="'Retirer ' + m.nom"
                  [desactive]="ecran.occupe()"
                  (confirme)="retirer(m.id)"
                />
              }
            </li>
          }
        </ul>
        <form class="card formulaire" (ngSubmit)="ajouter()">
          <h2 class="titre-carte large">Ajouter un employé</h2>
          <div>
            <label class="label" for="employe-nom">Prénom ou nom</label>
            <input
              id="employe-nom"
              class="input"
              name="nom"
              maxlength="60"
              autocomplete="off"
              [(ngModel)]="nom"
            />
          </div>
          <div>
            <label class="label" for="employe-poste">Poste</label>
            <input
              id="employe-poste"
              class="input"
              name="poste"
              maxlength="30"
              placeholder="Poste 3"
              [(ngModel)]="poste"
            />
          </div>
          <div>
            <label class="label" for="employe-email">E-mail de connexion</label>
            <input
              id="employe-email"
              class="input"
              type="email"
              name="email"
              maxlength="160"
              autocomplete="off"
              [(ngModel)]="email"
            />
          </div>
          <div>
            <label class="label" for="employe-mdp">Mot de passe provisoire</label>
            <input
              id="employe-mdp"
              class="input"
              type="password"
              name="mdp"
              minlength="6"
              maxlength="72"
              autocomplete="new-password"
              [(ngModel)]="motDePasse"
            />
          </div>
          <button
            class="btn large"
            type="submit"
            [disabled]="
              ecran.occupe() ||
              !nom().trim() ||
              !poste().trim() ||
              !email().trim() ||
              motDePasse().length < 6
            "
          >
            Ajouter l’employé
          </button>
        </form>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class Equipe {
  private readonly api = inject(GestionApi);
  protected readonly postes = signal(1);
  protected readonly nom = signal('');
  protected readonly poste = signal('');
  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  protected readonly ecran = creerEcran(
    () => this.api.equipe(),
    (e) => this.postes.set(e.postesOuverts),
  );

  constructor() {
    this.ecran.charger();
  }

  protected changerPostes() {
    this.ecran.agir(this.api.changerPostes(this.postes()), 'Nombre de postes enregistré.');
  }

  protected retirer(id: number) {
    this.ecran.agir(this.api.retirerEmploye(id), 'Employé retiré.');
  }

  protected ajouter() {
    const e = {
      nom: this.nom().trim(),
      poste: this.poste().trim(),
      email: this.email().trim(),
      motDePasse: this.motDePasse(),
    };
    this.ecran.agir(
      this.api.ajouterEmploye(e),
      'Employé ajouté : il peut se connecter depuis l’Espace pro.',
      () => {
        for (const champ of [this.nom, this.poste, this.email, this.motDePasse]) champ.set('');
      },
    );
  }
}
