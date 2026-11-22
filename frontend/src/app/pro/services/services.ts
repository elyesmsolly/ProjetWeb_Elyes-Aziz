import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { prix } from '../../core/format';
import { GestionApi } from '../../core/gestion-api.service';
import { Prestation } from '../../core/models';
import { BoutonConfirmer } from '../../shared/bouton-confirmer';
import { creerEcran } from '../../core/ecran';
import { EnteteEcran } from '../../shared/entete-ecran';
import { EtatChargement } from '../../shared/etat-chargement';

/** « Services » du tableau de bord. */
@Component({
  selector: 'app-services',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, BoutonConfirmer, EnteteEcran, EtatChargement],
  template: `
    <app-entete-ecran
      titre="Services"
      sousTitre="Ce que vos clients choisissent avant de prendre un ticket"
    />
    <div class="gestion">
      <form class="card formulaire" (ngSubmit)="enregistrer()">
        <h2 class="titre-carte large">
          {{ edition() ? 'Modifier le service' : 'Ajouter un service' }}
        </h2>
        <div>
          <label class="label" for="service-nom">Nom</label>
          <input id="service-nom" class="input" name="nom" maxlength="80" [(ngModel)]="nom" />
        </div>
        <div>
          <label class="label" for="service-description">Description (facultatif)</label>
          <input
            id="service-description"
            class="input"
            name="description"
            maxlength="200"
            [(ngModel)]="description"
          />
        </div>
        <div>
          <label class="label" for="service-duree">Durée (minutes)</label>
          <input
            id="service-duree"
            class="input"
            type="number"
            name="duree"
            min="1"
            max="240"
            [(ngModel)]="duree"
          />
        </div>
        <div>
          <label class="label" for="service-prix">Prix (DT)</label>
          <input
            id="service-prix"
            class="input"
            type="number"
            name="prix"
            min="0"
            step="0.1"
            [(ngModel)]="montant"
          />
        </div>
        <div class="row wrap large">
          <button class="btn" type="submit" [disabled]="ecran.occupe() || !nom().trim()">
            {{ edition() ? 'Enregistrer' : 'Ajouter' }}
          </button>
          @if (edition()) {
            <button class="btn ghost" type="button" (click)="vider()">Annuler</button>
          }
        </div>
      </form>
      @if (ecran.donnees(); as liste) {
        <ul class="card lignes">
          @for (s of liste; track s.id) {
            <li>
              <div class="grow">
                <b>{{ s.nom }}</b>
                <div class="small muted">{{ s.description }}</div>
              </div>
              <span class="badge">{{ s.dureeMinutes }} min</span>
              <b>{{ prix(s.prix) }}</b>
              <button
                class="btn ghost sm"
                type="button"
                [attr.aria-label]="'Modifier ' + s.nom"
                (click)="modifier(s)"
              >
                Modifier
              </button>
              <app-bouton-confirmer
                [description]="'Supprimer ' + s.nom"
                [desactive]="ecran.occupe() || liste.length === 1"
                (confirme)="retirer(s.id)"
              />
            </li>
          }
        </ul>
      } @else {
        <app-etat-chargement [erreur]="ecran.erreur()" (reessayer)="ecran.charger()" />
      }
    </div>
  `,
})
export class Services {
  private readonly api = inject(GestionApi);
  protected readonly prix = prix;
  protected readonly ecran = creerEcran(() => this.api.services());
  protected readonly edition = signal<number | null>(null);
  protected readonly nom = signal('');
  protected readonly description = signal('');
  protected readonly duree = signal(15);
  protected readonly montant = signal(0);

  constructor() {
    this.ecran.charger();
  }

  protected enregistrer() {
    const id = this.edition();
    const p = {
      nom: this.nom().trim(),
      description: this.description().trim(),
      dureeMinutes: this.duree(),
      prix: this.montant(),
    };
    const appel = id ? this.api.modifierService(id, p) : this.api.ajouterService(p);
    this.ecran.agir(appel, id ? 'Service modifié.' : 'Service ajouté.', () => this.vider());
  }

  protected modifier(s: Prestation) {
    this.edition.set(s.id);
    this.nom.set(s.nom);
    this.description.set(s.description ?? '');
    this.duree.set(s.dureeMinutes);
    this.montant.set(s.prix);
    document.getElementById('service-nom')?.focus();
  }

  protected retirer(id: number) {
    if (this.edition() === id) this.vider();
    this.ecran.agir(this.api.retirerService(id), 'Service supprimé.');
  }

  protected vider() {
    this.edition.set(null);
    this.nom.set('');
    this.description.set('');
    this.duree.set(15);
    this.montant.set(0);
  }
}
