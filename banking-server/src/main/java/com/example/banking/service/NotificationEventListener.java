package com.example.banking.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Declenche la creation des notifications APRES le commit du virement, et de
 * maniere ASYNCHRONE (executeur dedie). C'est essentiel : un listener AFTER_COMMIT
 * synchrone s'execute AVANT la liberation de la connexion du virement ; ouvrir une
 * 2e connexion (notification) a ce moment-la doublerait la demande de connexions
 * et pourrait epuiser le pool sous forte concurrence. En asynchrone, la connexion
 * du virement est deja relachee quand la notification est ecrite.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoneyMoved(MoneyMovedEvent event) {
        try {
            notificationService.createForMoneyMoved(event);
        } catch (Exception ignored) {
            // Best-effort : une notification ratee ne doit rien casser.
        }
    }
}
