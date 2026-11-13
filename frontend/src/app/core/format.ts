// Petites fonctions d'affichage en français.

export function heure(iso: string | Date | null | undefined): string {
  if (!iso) return '—';
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return `${d.getHours()} h ${String(d.getMinutes()).padStart(2, '0')}`;
}

export function distance(km: number): string {
  return km < 1 ? `${Math.max(50, Math.round((km * 1000) / 50) * 50)} m` : `${km.toFixed(1).replace('.', ',')} km`;
}

export function note(n: number): string { return n.toFixed(1).replace('.', ','); }

export function minutesDepuis(iso: string | null): number {
  return iso ? Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000)) : 0;
}

export function prix(p: number): string { return p === 0 ? 'Gratuit' : `${p.toFixed(0)} DT`; }

export function badgeAttente(min: number): 'ok' | 'warn' | 'danger' { return min <= 20 ? 'ok' : min <= 45 ? 'warn' : 'danger'; }

/** Icône d'un type d'établissement. Pour « Autres », le type libre saisi par le gérant donne un indice (« Café »). */
export function icone(categorie: string, libelle = ''): string {
  if (categorie === 'AUTRE') return /caf|th[ée]|jus|boisson/i.test(libelle) ? 'cup' : 'store';
  return ({ LABORATOIRE: 'flask', AGENCE: 'bank', ADMINISTRATION: 'form' } as Record<string, string>)[categorie] ?? 'scissors';
}
