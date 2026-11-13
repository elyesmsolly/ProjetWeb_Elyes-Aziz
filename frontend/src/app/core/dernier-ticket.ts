import { Injectable, signal } from '@angular/core';

const CLE = 'dourek.ticket';

/** Le dernier ticket pris sur cet appareil, pour le lien « Mon ticket ». */
@Injectable({ providedIn: 'root' })
export class DernierTicket {
  readonly code = signal<string | null>(lire());

  garder(code: string) {
    this.code.set(code);
    try { localStorage.setItem(CLE, code); } catch { /* navigation privée */ }
  }

  oublier() {
    this.code.set(null);
    try { localStorage.removeItem(CLE); } catch { /* rien */ }
  }
}

function lire(): string | null {
  try { return localStorage.getItem(CLE); } catch { return null; }
}
