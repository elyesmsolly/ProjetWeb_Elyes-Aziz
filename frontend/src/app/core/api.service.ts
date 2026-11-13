import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssistantReponse, Categorie, ChampFormulaire, EtablissementDetail, EtablissementResume, EtatFile,
  Formulaire, Inscription, Session, Statistiques, TicketVue,
} from './models';
import { PositionService } from './position.service';

/** Accès à l'API REST (servie sous /api, proxy du serveur Angular vers Spring Boot). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private position = inject(PositionService);

  private pos(params = new HttpParams()): HttpParams {
    const p = this.position.coords();
    return p ? params.set('lat', p.lat).set('lng', p.lng) : params;
  }

  categories(): Observable<Categorie[]> { return this.http.get<Categorie[]>('/api/categories'); }

  rechercher(categorie?: string | null, q?: string | null): Observable<EtablissementResume[]> {
    let params = this.pos();
    if (categorie) params = params.set('categorie', categorie);
    if (q) params = params.set('q', q);
    return this.http.get<EtablissementResume[]>('/api/etablissements', { params });
  }

  etablissement(slug: string): Observable<EtablissementDetail> {
    return this.http.get<EtablissementDetail>(`/api/etablissements/${slug}`, { params: this.pos() });
  }

  prendreTicket(slug: string, prestationId: number, reponses: Record<string, unknown>, prioritaire: boolean) {
    return this.http.post<{ code: string; numero: string }>(`/api/etablissements/${slug}/tickets`, { prestationId, reponses, prioritaire });
  }

  /** Adresse d'un point de la carte (OpenStreetMap / Nominatim, via notre serveur). */
  adresse(lat: number, lng: number) {
    return this.http.get<{ adresse: string; quartier: string }>('/api/geo/adresse', { params: { lat, lng } });
  }
  mesTickets(): Observable<TicketVue[]> { return this.http.get<TicketVue[]>('/api/client/tickets'); }
  ticket(code: string): Observable<TicketVue> { return this.http.get<TicketVue>(`/api/tickets/${code}`); }
  annuler(code: string): Observable<TicketVue> { return this.http.post<TicketVue>(`/api/tickets/${code}/annuler`, {}); }
  laisserPasser(code: string): Observable<TicketVue> { return this.http.post<TicketVue>(`/api/tickets/${code}/laisser-passer`, {}); }

  assistant(message: string): Observable<AssistantReponse> {
    const p = this.position.coords();
    return this.http.post<AssistantReponse>('/api/assistant', { message, lat: p?.lat ?? null, lng: p?.lng ?? null });
  }

  // ---- espace pro
  connexion(email: string, motDePasse: string) { return this.http.post<Session>('/api/auth/connexion', { email, motDePasse }); }
  inscriptionClient(c: { nom: string; email: string; telephone: string; motDePasse: string }) {
    return this.http.post<Session>('/api/auth/inscription-client', c);
  }
  inscription(i: Inscription) { return this.http.post<Session>('/api/auth/inscription', i); }
  etatFile() { return this.http.get<EtatFile>('/api/pro/file'); }
  appelerSuivant() { return this.http.post<EtatFile>('/api/pro/file/appeler-suivant', {}); }
  servi() { return this.http.post<EtatFile>('/api/pro/file/servi', {}); }
  absent() { return this.http.post<EtatFile>('/api/pro/file/absent', {}); }
  surPlace(nom: string, prestationId: number, prioritaire: boolean) {
    return this.http.post<EtatFile>('/api/pro/file/sur-place', { nom, prestationId, prioritaire });
  }
  statistiques() { return this.http.get<Statistiques>('/api/pro/gerant/statistiques'); }
  ouverture(ouverte: boolean) { return this.http.post<EtatFile>('/api/pro/gerant/file/ouverture', { ouverte }); }
  formulaire() { return this.http.get<Formulaire>('/api/pro/gerant/formulaire'); }
  publierFormulaire(champs: ChampFormulaire[]) { return this.http.post<Formulaire>('/api/pro/gerant/formulaire', { champs }); }
}

/** Message d'erreur lisible à partir d'une réponse HTTP de l'API. */
export function messageErreur(e: unknown): string {
  const err = e as { status?: number; error?: { message?: string } };
  if (err?.status === 0) return 'Le serveur Dourek ne répond pas. Vérifiez qu’il est lancé.';
  return err?.error?.message || 'Une erreur est survenue. Réessayez.';
}
