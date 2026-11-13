package tn.supcom.dourek.service;

import java.time.Instant;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Prévient les écrans (client, gérant, employé) qu'une file a changé.
 * Le message est envoyé après le commit : les écrans rechargent alors l'état complet
 * depuis l'API, ce qui évite tout état incohérent (y compris après une reconnexion).
 */
@Component
public class TempsReel {

    private final SimpMessagingTemplate broker;

    public TempsReel(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    public void fileModifiee(Long etablissementId) {
        String destination = "/topic/etablissements/" + etablissementId;
        Object message = Map.of("type", "MAJ", "etablissementId", etablissementId, "le", Instant.now().toString());
        Runnable envoi = () -> broker.convertAndSend(destination, message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoi.run();
                }
            });
        } else {
            envoi.run();
        }
    }
}
