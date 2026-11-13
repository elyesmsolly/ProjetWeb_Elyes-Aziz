import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, effect, input, output, signal, viewChild } from '@angular/core';
import { Injector, inject } from '@angular/core';
import * as L from 'leaflet';

export interface Repere { id: string | number; lat: number; lng: number; texte: string; style?: 'hot' | 'off' | ''; }

/** Carte Leaflet + OpenStreetMap : un repère par établissement avec l'attente écrite dessus. */
@Component({
  selector: 'app-carte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.plein]': 'plein()', '(document:keydown.escape)': 'plein.set(false)' },
  template: `
    <div #zone class="zone" role="region" aria-label="Carte des établissements"></div>
    <button type="button" class="agrandir" (click)="plein.set(!plein())"
            [attr.aria-label]="plein() ? 'Réduire la carte' : 'Agrandir la carte'" [title]="plein() ? 'Réduire (Échap)' : 'Agrandir'">
      @if (plein()) {
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5"/></svg>
      } @else {
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></svg>
      }
    </button>`,
  styles: `
    :host { display: block; position: relative; }
    .zone { position: absolute; inset: 0; border-radius: inherit; }
    :host(.plein) { position: fixed !important; inset: 12px !important; z-index: 2000; border-radius: 16px; overflow: hidden;
      box-shadow: 0 0 0 100vmax rgba(16, 31, 19, .55); }
    .agrandir { position: absolute; top: 10px; right: 10px; z-index: 1000; width: 36px; height: 36px; display: grid; place-items: center;
      background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: 8px; box-shadow: var(--shadow);
      transition: transform var(--t-press) var(--ease-out); }
    .agrandir:active { transform: scale(0.95); }
  `,
})
export class Carte implements AfterViewInit, OnDestroy {
  readonly reperes = input<Repere[]>([]);
  readonly moi = input<{ lat: number; lng: number } | null>(null);
  readonly zoom = input(14);
  readonly choisir = output<string | number>();
  readonly clic = output<{ lat: number; lng: number }>();

  private injector = inject(Injector);
  private zone = viewChild.required<ElementRef<HTMLDivElement>>('zone');
  private carte?: L.Map;
  private couche = L.layerGroup();
  private premier = true;
  /** Carte en plein écran (bouton en haut à droite, Échap pour sortir). */
  readonly plein = signal(false);

  constructor() {
    effect(() => this.dessiner(this.reperes(), this.moi()));
  }

  ngAfterViewInit() {
    const centre = this.moi() ?? this.reperes()[0] ?? { lat: 36.8625, lng: 10.1956 };
    this.carte = L.map(this.zone().nativeElement, { zoomControl: true, attributionControl: true }).setView([centre.lat, centre.lng], this.zoom());
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(this.carte);
    this.couche.addTo(this.carte);
    this.carte.on('click', (ev: L.LeafletMouseEvent) => this.clic.emit({ lat: ev.latlng.lat, lng: ev.latlng.lng }));
    // Recentre la carte quand on l'agrandit ou la réduit.
    effect(() => { this.plein(); setTimeout(() => this.carte?.invalidateSize(), 50); }, { injector: this.injector });
    this.dessiner(this.reperes(), this.moi());
    new ResizeObserver(() => this.carte?.invalidateSize()).observe(this.zone().nativeElement);
  }

  private dessiner(reperes: Repere[], moi: { lat: number; lng: number } | null) {
    if (!this.carte) return;
    this.couche.clearLayers();
    if (moi) L.marker([moi.lat, moi.lng], { icon: L.divIcon({ className: '', html: '<div class="dk-me"></div>', iconSize: [0, 0] }), interactive: false }).addTo(this.couche);
    for (const r of reperes) {
      const icon = L.divIcon({ className: '', iconSize: [0, 0], html: `<div class="dk-pin"><b class="${r.style ?? ''}">${echapper(r.texte)}</b><i></i></div>` });
      L.marker([r.lat, r.lng], { icon, keyboard: true, title: r.texte }).on('click', () => this.choisir.emit(r.id)).addTo(this.couche);
    }
    if (this.premier && reperes.length > 1) {
      const pts = reperes.map((r) => [r.lat, r.lng] as [number, number]);
      if (moi) pts.push([moi.lat, moi.lng]);
      this.carte.fitBounds(L.latLngBounds(pts), { padding: [48, 48], maxZoom: 15 });
      this.premier = false;
    }
  }

  ngOnDestroy() { this.carte?.remove(); }
}

function echapper(s: string) {
  return s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);
}
