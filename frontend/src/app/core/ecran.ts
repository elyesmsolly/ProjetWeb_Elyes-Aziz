import { DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { messageErreur } from './api.service';
import { ToastService } from './toast.service';

/**
 * État commun d'une page de données : chargement, erreur, action en cours.
 * À créer dans un champ de composant (utilise `inject`).
 */
export function creerEcran<T>(source: () => Observable<T>, recu?: (donnees: T) => void) {
  const toast = inject(ToastService);
  const destruction = inject(DestroyRef);
  const donnees = signal<T | null>(null);
  const erreur = signal<string | null>(null);
  const occupe = signal(false);

  const charger = () => {
    erreur.set(null);
    source()
      .pipe(takeUntilDestroyed(destruction))
      .subscribe({
        next: (d) => {
          donnees.set(d);
          recu?.(d);
        },
        error: (e) => erreur.set(messageErreur(e)),
      });
  };

  /** Lance une modification, l'annonce, puis recharge la page. */
  const agir = (appel: Observable<unknown>, succes: string, ensuite?: () => void) => {
    if (occupe()) return;
    occupe.set(true);
    appel.subscribe({
      next: () => {
        occupe.set(false);
        toast.afficher(succes);
        ensuite?.();
        charger();
      },
      error: (e) => {
        occupe.set(false);
        toast.afficher(messageErreur(e), true);
      },
    });
  };

  return { donnees, erreur, occupe, charger, agir };
}
