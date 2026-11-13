import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly message = signal<{ texte: string; erreur: boolean } | null>(null);
  private minuteur?: ReturnType<typeof setTimeout>;

  afficher(texte: string, erreur = false) {
    this.message.set({ texte, erreur });
    clearTimeout(this.minuteur);
    this.minuteur = setTimeout(() => this.message.set(null), 3200);
  }
}
