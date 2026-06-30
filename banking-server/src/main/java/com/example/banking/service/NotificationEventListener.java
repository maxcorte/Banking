package com.example.banking.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Apres le commit d'un virement, et de maniere ASYNCHRONE :
 *  1. cree les notifications in-app ;
 *  2. envoie les notifications push web aux navigateurs abonnes.
 *
 * Asynchrone car (a) un listener AFTER_COMMIT synchrone tient encore la connexion
 * du virement et (b) l'envoi push fait des appels reseau : il ne doit jamais
 * ralentir ni bloquer le chemin transactionnel du grand livre.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final WebPushService webPushService;

    public NotificationEventListener(NotificationService notificationService,
                                     WebPushService webPushService) {
        this.notificationService = notificationService;
        this.webPushService = webPushService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoneyMoved(MoneyMovedEvent event) {
        try {
            List<NotificationService.PushMessage> messages =
                    notificationService.createForMoneyMoved(event);
            for (NotificationService.PushMessage m : messages) {
                webPushService.sendToUser(m.userId(), m.title(), m.body());
            }
        } catch (Exception ignored) {
            // Best-effort : une notification ratee ne doit rien casser.
        }
    }
}
