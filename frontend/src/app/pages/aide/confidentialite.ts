import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageEntete } from '../../shared/page-entete';

/** À mettre à jour dès qu'une donnée ou un service externe est ajouté au projet. */
@Component({
  selector: 'app-confidentialite',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, PageEntete],
  template: `
    <div class="container page-enter texte">
      <app-page-entete
        titre="Confidentialité"
        intro="Dourek est un projet étudiant. Voici, sans jargon, ce qu’il fait de vos données."
      />
      <section class="card">
        <h2>Ce que nous enregistrons</h2>
        <ul>
          <li>
            Votre compte : nom, e-mail, mot de passe chiffré de façon irréversible (BCrypt) et, pour
            un client, téléphone.
          </li>
          <li>
            Vos tickets : établissement, prestation, heures de passage et réponses au formulaire.
          </li>
          <li>Vos avis et les messages envoyés depuis la page Contact.</li>
          <li>
            Pour un professionnel : les informations de son établissement, de ses services et de ses
            formulaires.
          </li>
        </ul>
      </section>
      <section class="card">
        <h2>Qui peut les voir</h2>
        <ul>
          <li>Le suivi d’un ticket n’est ouvert qu’au compte qui l’a pris.</li>
          <li>
            Le gérant et les employés de l’établissement voient le nom et les réponses des clients
            de leur file.
          </li>
          <li>Vos avis sont publics, signés de votre prénom et de l’initiale de votre nom.</li>
          <li>
            L’administration voit les comptes (sans mot de passe), les avis signalés et les messages
            de contact.
          </li>
          <li>Les autres clients de la file voient votre numéro de ticket, jamais votre nom.</li>
        </ul>
      </section>
      <section class="card">
        <h2>Votre position</h2>
        <p>
          Elle n’est demandée que pour calculer les distances, et seulement avec l’accord de votre
          navigateur. Notre serveur ne l’enregistre pas.
        </p>
      </section>
      <section class="card">
        <h2>Services externes</h2>
        <ul>
          <li>
            La carte est chargée depuis OpenStreetMap, qui reçoit votre adresse IP et la zone
            affichée.
          </li>
          <li>
            À l’inscription d’un établissement, le point cliqué sur la carte est envoyé à Nominatim
            pour trouver l’adresse.
          </li>
          <li>
            Avec la recherche par IA, votre phrase et la liste des établissements, dont leur
            distance, peuvent être envoyées à Google Gemini ou à Anthropic Claude. L’offre gratuite
            de Gemini peut utiliser ces échanges pour améliorer ses modèles : n’y écrivez rien de
            personnel.
          </li>
          <li>
            Avec le formulaire par IA, la description, le nom et l’activité de l’établissement sont
            envoyés au même service.
          </li>
        </ul>
      </section>
      <section class="card">
        <h2>Dans votre navigateur</h2>
        <p>
          Votre session reste le temps de l’onglet. Le code de votre dernier ticket est gardé
          jusqu’à la déconnexion. Dourek n’utilise aucun traceur publicitaire.
        </p>
      </section>
      <a class="btn ghost" routerLink="/aide">Retour à l’aide</a>
    </div>
  `,
  styles: `
    .texte {
      max-width: 820px;
    }
    section {
      margin-bottom: 16px;
    }
    h2 {
      font-size: 18px;
      margin-bottom: 10px;
    }
    ul {
      padding-left: 20px;
    }
    li {
      margin-bottom: 6px;
    }
  `,
})
export class Confidentialite {}
