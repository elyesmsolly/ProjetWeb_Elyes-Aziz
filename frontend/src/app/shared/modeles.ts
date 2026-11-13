import { Injectable } from '@angular/core';
import { ChampFormulaire } from '../core/models';

const NOM: ChampFormulaire = { cle: 'nom', type: 'TEXTE', label: 'Nom et prénom', obligatoire: true, options: [], aide: null };
const TEL: ChampFormulaire = { cle: 'telephone', type: 'TELEPHONE', label: 'Téléphone', obligatoire: true, options: [], aide: 'Sert aux alertes' };

/** Modèles de formulaires prêts à l'emploi (démo : un deuxième type d'établissement en 2 minutes). */
@Injectable({ providedIn: 'root' })
export class FormulaireService {
  readonly defaut: ChampFormulaire[] = [NOM, TEL];
  readonly liste: { nom: string; champs: ChampFormulaire[] }[] = [
    { nom: 'Salon de coiffure', champs: [NOM, TEL,
      { cle: 'x1', type: 'CHOIX', label: 'Coiffeur préféré', obligatoire: false, options: ['Peu importe'], aide: null },
      { cle: 'x2', type: 'TEXTE_LONG', label: 'Remarque pour le coiffeur', obligatoire: false, options: [], aide: 'Ex. dégradé court sur les côtés' }] },
    { nom: 'Laboratoire d’analyses', champs: [NOM, TEL,
      { cle: 'x1', type: 'DATE', label: 'Date de naissance', obligatoire: true, options: [], aide: null },
      { cle: 'x2', type: 'CASE', label: 'Je suis à jeun', obligatoire: false, options: [], aide: null },
      { cle: 'x3', type: 'LISTE', label: 'Ordonnance', obligatoire: true, options: ['Papier', 'Numérique', 'Sans ordonnance'], aide: null }] },
    { nom: 'Agence de services', champs: [NOM, TEL,
      { cle: 'x1', type: 'LISTE', label: 'Motif de la visite', obligatoire: true, options: ['Paiement', 'Abonnement', 'Réclamation'], aide: null },
      { cle: 'x2', type: 'NOMBRE', label: 'Numéro de client', obligatoire: false, options: [], aide: null }] },
  ];
}
