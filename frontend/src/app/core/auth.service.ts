import { HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Session } from './models';

const CLE = 'dourek.session';

/** Session (client ou espace pro) : jeton JWT gardé dans le navigateur. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<Session | null>(lire());
  readonly connecte = computed(() => this.session() !== null);
  readonly gerant = computed(() => this.session()?.role === 'GERANT');
  readonly client = computed(() => this.session()?.role === 'CLIENT');
  readonly pro = computed(() => this.connecte() && !this.client());

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

/** Ajoute le jeton aux appels de l'API (sauf connexion / inscription). */
export const jetonInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.session()?.token;
  if (token && req.url.startsWith('/api/') && !req.url.startsWith('/api/auth/'))
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(req);
};

/** Pages du client (ticket, suivi) : il faut être connecté avec un compte client. */
export const clientGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.client() ? true : inject(Router).createUrlTree(['/connexion'], { queryParams: { retour: state.url } });
};

export const proGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.pro() ? true : inject(Router).createUrlTree(['/pro/connexion']);
};

export const gerantGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.pro()) return inject(Router).createUrlTree(['/pro/connexion']);
  return auth.gerant() ? true : inject(Router).createUrlTree(['/pro/employe']);
};
