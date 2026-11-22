import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Avis,
  CategorieAdmin,
  EtablissementAdmin,
  MessageRecu,
  UtilisateurAdmin,
} from './gestion.models';

const BASE = '/api/admin';

/** Pages d'administration de la plateforme. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  etablissements() {
    return this.http.get<EtablissementAdmin[]>(`${BASE}/etablissements`);
  }
  suspendreEtablissement(id: number, suspendu: boolean) {
    return this.http.put<void>(`${BASE}/etablissements/${id}/suspension`, { suspendu });
  }
  utilisateurs() {
    return this.http.get<UtilisateurAdmin[]>(`${BASE}/utilisateurs`);
  }
  suspendreUtilisateur(id: number, suspendu: boolean) {
    return this.http.put<void>(`${BASE}/utilisateurs/${id}/suspension`, { suspendu });
  }
  changerRole(id: number, role: string) {
    return this.http.put<void>(`${BASE}/utilisateurs/${id}/role`, { role });
  }
  avis() {
    return this.http.get<Avis[]>(`${BASE}/avis`);
  }
  conserverAvis(id: number) {
    return this.http.put<void>(`${BASE}/avis/${id}/conserver`, {});
  }
  supprimerAvis(id: number) {
    return this.http.delete<void>(`${BASE}/avis/${id}`);
  }
  categories() {
    return this.http.get<CategorieAdmin[]>(`${BASE}/categories`);
  }
  renommerCategorie(ancien: string, nouveau: string) {
    return this.http.put<void>(`${BASE}/categories`, { ancien, nouveau });
  }
  messages() {
    return this.http.get<MessageRecu[]>(`${BASE}/messages`);
  }
  supprimerMessage(id: number) {
    return this.http.delete<void>(`${BASE}/messages/${id}`);
  }
}
