import { HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Session } from './models';

const CLE = 'dourek.session';

/** Session de l'espace pro (jeton JWT gardé dans le navigateur). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<Session | null>(lire());
  readonly connecte = computed(() => this.session() !== null);
  readonly gerant = computed(() => this.session()?.role === 'GERANT');

  ouvrir(s: Session) {
    this.session.set(s);
    try { localStorage.setItem(CLE, JSON.stringify(s)); } catch { /* navigation privée */ }
  }

  fermer() {
    this.session.set(null);
    try { localStorage.removeItem(CLE); } catch { /* rien */ }
  }
}

function lire(): Session | null {
  try {
    const s = JSON.parse(localStorage.getItem(CLE) ?? 'null') as Session | null;
    if (!s) return null;
    const exp = JSON.parse(atob(s.token.split('.')[1])).exp as number;
    return exp * 1000 > Date.now() ? s : null;
  } catch { return null; }
}

/** Ajoute le jeton aux appels /api/pro et déconnecte si le serveur le refuse. */
export const jetonInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.session()?.token;
  if (token && req.url.startsWith('/api/pro')) req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(req);
};

export const proGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.connecte() ? true : inject(Router).createUrlTree(['/pro/connexion']);
};

export const gerantGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.connecte()) return inject(Router).createUrlTree(['/pro/connexion']);
  return auth.gerant() ? true : inject(Router).createUrlTree(['/pro/employe']);
};
