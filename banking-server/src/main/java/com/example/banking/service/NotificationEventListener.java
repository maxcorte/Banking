package com.example.banking.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Declenche la creation des notifications APRES le commit du virement. Si la
 * generation echoue, le virement reste valide (deja committe) ; on isole donc
 * totalement les notifications du chemin critique du grand livre.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoneyMoved(MoneyMovedEvent event) {
        try {
            notificationService.createForMoneyMoved(event);
        } catch (Exception ignored) {
            // Best-effort : une notification ratee ne doit rien casser.
        }
    }
}
