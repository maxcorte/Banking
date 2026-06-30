package com.example.banking.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Traite, de maniere ASYNCHRONE et APRES COMMIT, la creation des notifications
 * (in-app + push) :
 *  - sur un mouvement d'argent (MoneyMovedEvent) ;
 *  - sur une notification applicative ponctuelle (UserNotificationEvent),
 *    par ex. une demande de remboursement recue ou refusee.
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
            // Best-effort.
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserNotification(UserNotificationEvent event) {
        try {
            notificationService.create(event.userId(), event.type(), event.title(), event.body());
            webPushService.sendToUser(event.userId(), event.title(), event.body());
        } catch (Exception ignored) {
            // Best-effort.
        }
    }
}
