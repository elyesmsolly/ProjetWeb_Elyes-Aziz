// Types échangés avec l'API Spring Boot (miroir des records Java de Dtos.java).

export type TypeChamp = 'TEXTE' | 'TEXTE_LONG' | 'TELEPHONE' | 'NOMBRE' | 'LISTE' | 'CHOIX' | 'CASE' | 'DATE';

export interface ChampFormulaire {
  cle: string;
  type: TypeChamp;
  label: string;
  obligatoire: boolean;
  options: string[];
  aide?: string | null;
}

export interface Formulaire { version: number; champs: ChampFormulaire[]; publieLe: string | null; }

export interface Prestation { id: number; nom: string; description: string; dureeMinutes: number; prix: number; }

export interface EtablissementResume {
  id: number; slug: string; nom: string; categorie: string; categorieLibelle: string;
  quartier: string; adresse: string; latitude: number; longitude: number; distanceKm: number;
  note: number; nombreAvis: number; fileOuverte: boolean; heureFermeture: number;
  personnesEnAttente: number; attenteMinutes: number; postesOuverts: number;
  prestations: Prestation[]; score: number; meilleurChoix: boolean;
}

export interface EtablissementDetail {
  resume: EtablissementResume; description: string; horaires: string; telephone: string; formulaire: Formulaire;
}

export interface Categorie { code: string; libelle: string; nombre: number; libre?: boolean; }

export interface PastilleFile { numero: string; etat: 'SERVI_EN_COURS' | 'ATTENTE' | 'MOI'; }

export type Statut = 'EN_ATTENTE' | 'APPELE' | 'SERVI' | 'ABSENT' | 'ANNULE';

export interface TicketVue {
  code: string; numero: string; statut: Statut; etablissementId: number; etablissementSlug: string; etablissementNom: string;
  adresse: string; latitude: number; longitude: number; prestation: string; personnesAvant: number;
  attenteMinutes: number; passageEstime: string; creeLe: string; appeleLe: string | null; termineLe: string | null;
  poste: string | null; formulaireVersion: number; prioritaire: boolean; file: PastilleFile[];
}

export interface TicketLigne {
  code: string; numero: string; nomClient: string; prestation: string; statut: Statut; prioritaire: boolean;
  surPlace: boolean; creeLe: string; appeleLe: string | null; attenteEstimeeMinutes: number; poste: string | null;
  reponses: Record<string, unknown>; champs: ChampFormulaire[];
}

export interface EtatFile {
  etablissementId: number; etablissementNom: string; fileOuverte: boolean; postesOuverts: number;
  dureeMoyenneMinutes: number; enCours: TicketLigne[]; enAttente: TicketLigne[]; prestations: Prestation[];
}

export interface Statistiques {
  servis: number; enAttente: number; attenteMoyenneMinutes: number; tauxAbsence: number; absents: number;
  ecartEstimeReelMinutes: number; affluence: { heure: number; nombre: number }[];
  postes: { poste: string; employe: string; ticket: string | null; prestation: string | null; depuisMinutes: number | null }[];
  derniers: TicketLigne[];
}

export interface Session {
  token: string; nom: string; role: 'GERANT' | 'EMPLOYE' | 'CLIENT'; etablissementId: number | null;
  etablissementNom: string | null; slug: string | null; poste: string | null; email: string; telephone: string | null;
}

export interface Criteres {
  categorie: string | null; prestation: string | null; quartier: string | null;
  avantHeure: number | null; attenteMax: number | null; petitPrix: boolean | null;
}

export interface AssistantReponse { reponse: string; criteres: Criteres; resultats: EtablissementResume[]; source: 'claude' | 'local'; }

export interface Inscription {
  nom: string; email: string; motDePasse: string; nomEtablissement: string; telephone: string;
  categorie: string; categorieLibre: string | null; adresse: string; quartier: string; latitude: number; longitude: number;
  heureOuverture: number; heureFermeture: number; horaires: string; postes: number;
  prestations: Omit<Prestation, 'id'>[]; champs: ChampFormulaire[];
}
