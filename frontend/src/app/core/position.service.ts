import { Injectable, signal } from '@angular/core';

/**
 * Position du client. Par défaut : Ariana. Le navigateur ne donne la position qu'en HTTPS
 * ou sur localhost (voir README) ; sinon on garde la position par défaut.
 */
@Injectable({ providedIn: 'root' })
export class PositionService {
  readonly coords = signal<{ lat: number; lng: number } | null>(null);
  readonly defaut = { lat: 36.8625, lng: 10.1956 };

  demander(): Promise<void> {
    return new Promise((resolve) => {
      if (!('geolocation' in navigator)) return resolve();
      navigator.geolocation.getCurrentPosition(
        (p) => { this.coords.set({ lat: p.coords.latitude, lng: p.coords.longitude }); resolve(); },
        () => resolve(),
        { timeout: 4000, maximumAge: 300000 },
      );
    });
  }

  actuelle() { return this.coords() ?? this.defaut; }
}
