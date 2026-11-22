// Types des pages de l'Objectif 4 (miroir de GestionDtos.java).

export interface Profil {
  nom: string;
  email: string;
  telephone: string | null;
}
export interface ProfilModifie {
  nom: string;
  telephone: string;
  motDePasseActuel: string | null;
  nouveauMotDePasse: string | null;
}
export interface Avis {
  id: number;
  note: number;
  commentaire: string | null;
  auteur: string;
  creeLe: string;
  signale: boolean;
  etablissement: string;
}
export interface Contact {
  nom: string;
  email: string;
  sujet: string;
  message: string;
}
export interface Parametres {
  nom: string;
  telephone: string | null;
  adresse: string;
  quartier: string | null;
  description: string | null;
  horaires: string | null;
  heureOuverture: number;
  heureFermeture: number;
}
export interface Employe {
  id: number;
  nom: string;
  email: string;
  role: string;
  poste: string | null;
}
export interface Equipe {
  postesOuverts: number;
  membres: Employe[];
}
export interface NouvelEmploye {
  nom: string;
  email: string;
  motDePasse: string;
  poste: string;
}
export interface EtablissementAdmin {
  id: number;
  slug: string;
  nom: string;
  categorie: string;
  quartier: string | null;
  fileOuverte: boolean;
  suspendu: boolean;
}
export interface UtilisateurAdmin {
  id: number;
  nom: string;
  email: string;
  role: string;
  etablissement: string | null;
  suspendu: boolean;
}
export interface CategorieAdmin {
  libelle: string;
  nombre: number;
  libre: boolean;
}
export interface MessageRecu extends Contact {
  id: number;
  creeLe: string;
}
