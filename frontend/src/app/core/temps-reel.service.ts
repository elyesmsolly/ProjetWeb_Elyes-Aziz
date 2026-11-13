import { Injectable, OnDestroy, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { Observable } from 'rxjs';

interface Abonnement { destination: string; rappel: () => void; sub?: StompSubscription; }

/**
 * Temps réel (STOMP sur WebSocket). Le serveur envoie seulement « la file a changé » :
 * l'écran recharge alors l'état complet depuis l'API. On recharge aussi à chaque
 * (re)connexion, par exemple quand un téléphone sort de veille.
 */
@Injectable({ providedIn: 'root' })
export class TempsReelService implements OnDestroy {
  readonly connecte = signal(false);
  private readonly abonnements = new Set<Abonnement>();
  private readonly client: Client;

  constructor() {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    this.client = new Client({
      brokerURL: `${proto}://${location.host}/ws`,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    this.client.onConnect = () => {
      this.connecte.set(true);
      for (const a of this.abonnements) this.brancher(a);
    };
    this.client.onWebSocketClose = () => this.connecte.set(false);
    this.client.activate();
  }

  /** Émet tout de suite, puis à chaque changement de la file et à chaque reconnexion. */
  file(etablissementId: number): Observable<void> {
    return new Observable<void>((obs) => {
      const a: Abonnement = { destination: `/topic/etablissements/${etablissementId}`, rappel: () => obs.next() };
      this.abonnements.add(a);
      if (this.client.connected) this.brancher(a);
      else obs.next();
      return () => {
        this.abonnements.delete(a);
        try { a.sub?.unsubscribe(); } catch { /* connexion déjà fermée */ }
      };
    });
  }

  private brancher(a: Abonnement) {
    a.sub = this.client.subscribe(a.destination, () => a.rappel());
    a.rappel();
  }

  ngOnDestroy() { this.client.deactivate(); }
}
