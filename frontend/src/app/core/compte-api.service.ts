import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Avis, Contact, Profil, ProfilModifie } from './gestion.models';

/** Profil, avis et contact. */
@Injectable({ providedIn: 'root' })
export class CompteApi {
  private readonly http = inject(HttpClient);

  profil() {
    return this.http.get<Profil>('/api/client/profil');
  }
  modifierProfil(p: ProfilModifie) {
    return this.http.put<Profil>('/api/client/profil', p);
  }
  avisDuTicket(code: string) {
    return this.http.get<Avis | null>(`/api/tickets/${code}/avis`);
  }
  donnerAvis(code: string, note: number, commentaire: string) {
    return this.http.post<Avis>(`/api/tickets/${code}/avis`, { note, commentaire });
  }
  avis(slug: string) {
    return this.http.get<Avis[]>(`/api/etablissements/${slug}/avis`);
  }
  signaler(id: number) {
    return this.http.post<void>(`/api/avis/${id}/signaler`, {});
  }
  contacter(c: Contact) {
    return this.http.post<void>('/api/contact', c);
  }
}
