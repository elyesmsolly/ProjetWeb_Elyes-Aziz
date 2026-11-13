import { Injectable, signal } from '@angular/core';

/**
 * Position du client. Par défaut : Ariana. Le navigateur ne donne la position qu'en HTTPS
 * ou sur localhost (voir README) ; sinon on garde la position par défaut.
 */
@Injectable({ providedIn: 'root' })
export class PositionService {
  readonly coords = signal<{ lat: number; lng: number } | null>(null);
  readonly defaut = { lat: 36.8625, lng: 10.1956 };

  /** Résout true si une nouvelle position a été obtenue. Ne bloque jamais : si l'utilisateur ne répond pas
   *  à la demande d'autorisation (Safari, Firefox), on abandonne au bout de 6 s. */
  demander(): Promise<boolean> {
    return new Promise((resolve) => {
      if (!('geolocation' in navigator)) return resolve(false);
      const abandon = setTimeout(() => resolve(false), 6000);
      navigator.geolocation.getCurrentPosition(
        (p) => { clearTimeout(abandon); this.coords.set({ lat: p.coords.latitude, lng: p.coords.longitude }); resolve(true); },
        () => { clearTimeout(abandon); resolve(false); },
        { timeout: 4000, maximumAge: 300000 },
      );
    });
  }

  actuelle() { return this.coords() ?? this.defaut; }
}
