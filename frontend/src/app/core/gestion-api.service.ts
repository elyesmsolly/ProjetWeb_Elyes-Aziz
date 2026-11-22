import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Equipe, NouvelEmploye, Parametres } from './gestion.models';
import { Prestation } from './models';

const BASE = '/api/pro/gerant';

/** Pages de gestion du gérant. */
@Injectable({ providedIn: 'root' })
export class GestionApi {
  private readonly http = inject(HttpClient);

  services() {
    return this.http.get<Prestation[]>(`${BASE}/services`);
  }
  ajouterService(p: Omit<Prestation, 'id'>) {
    return this.http.post<Prestation>(`${BASE}/services`, p);
  }
  modifierService(id: number, p: Omit<Prestation, 'id'>) {
    return this.http.put<Prestation>(`${BASE}/services/${id}`, p);
  }
  retirerService(id: number) {
    return this.http.delete<void>(`${BASE}/services/${id}`);
  }
  equipe() {
    return this.http.get<Equipe>(`${BASE}/equipe`);
  }
  changerPostes(postesOuverts: number) {
    return this.http.put<void>(`${BASE}/equipe/postes`, { postesOuverts });
  }
  ajouterEmploye(e: NouvelEmploye) {
    return this.http.post<void>(`${BASE}/equipe`, e);
  }
  retirerEmploye(id: number) {
    return this.http.delete<void>(`${BASE}/equipe/${id}`);
  }
  parametres() {
    return this.http.get<Parametres>(`${BASE}/parametres`);
  }
  modifierParametres(p: Parametres) {
    return this.http.put<Parametres>(`${BASE}/parametres`, p);
  }
}
