import { HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Session } from './models';

const CLE = 'dourek.session';

/**
 * Session (client ou espace pro) : jeton JWT gardé le temps de l'onglet (sessionStorage).
 * À chaque nouvelle ouverture du site, on repasse donc par la page de connexion.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<Session | null>(lire());
  readonly connecte = computed(() => this.session() !== null);
  readonly gerant = computed(() => this.session()?.role === 'GERANT');
  readonly client = computed(() => this.session()?.role === 'CLIENT');
  readonly pro = computed(() => this.connecte() && !this.client());

  ouvrir(s: Session) {
    this.session.set(s);
    try { sessionStorage.setItem(CLE, JSON.stringify(s)); } catch { /* navigation privée */ }
  }

  fermer() {
    this.session.set(null);
    try { sessionStorage.removeItem(CLE); } catch { /* rien */ }
  }
}

function lire(): Session | null {
  try {
    localStorage.removeItem(CLE); // ancienne version : la session restait d'une visite à l'autre
    const s = JSON.parse(sessionStorage.getItem(CLE) ?? 'null') as Session | null;
    if (!s) return null;
    const exp = JSON.parse(atob(s.token.split('.')[1])).exp as number;
    return exp * 1000 > Date.now() ? s : null;
  } catch { return null; }
}

/** Pages ouvertes sans compte : connexion et création de compte (client ou espace pro). */
function sansCompte(url: string): boolean {
  return ['/connexion', '/inscription', '/pro/connexion', '/pro/inscription'].some((p) => url.startsWith(p));
}

/**
 * Ajoute le jeton aux appels de l'API (sauf connexion / inscription).
 * Si le serveur le refuse (401 : jeton expiré, ou compte disparu après une remise à zéro de la base de démo),
 * la session est fermée et on revient à la page de connexion.
 */
export const jetonInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.session()?.token;
  if (!token || !req.url.startsWith('/api/') || req.url.startsWith('/api/auth/')) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(catchError((e) => {
    if (e?.status !== 401) return throwError(() => e);
    if (auth.session()?.token === token) auth.fermer();
    const url = router.url;
    if (sansCompte(url)) { if (req.method === 'GET') return next(req); } // lecture publique : on la rejoue sans jeton
    else if (url.startsWith('/pro')) router.navigate(['/pro/connexion']);
    else router.navigate(['/connexion'], { queryParams: { retour: url === '/' ? null : url } });
    return throwError(() => e);
  }));
};

/** Tout le site (accueil, établissements) demande une session : sans compte, on arrive sur la page de connexion. */
export const sessionGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.connecte() ? true : inject(Router).createUrlTree(['/connexion'], { queryParams: { retour: state.url === '/' ? null : state.url } });
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
