package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.Notification;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cree et expose les notifications in-app. La creation se fait en reaction a un
 * MoneyMovedEvent, APRES le commit du virement (voir NotificationEventListener),
 * de maniere asynchrone : une eventuelle erreur n'affecte donc jamais le grand
 * livre. La methode renvoie aussi la liste des messages a pousser (push web).
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final AccountRepository accounts;

    public NotificationService(NotificationRepository notifications, AccountRepository accounts) {
        this.notifications = notifications;
        this.accounts = accounts;
    }

    /** Genere les notifications in-app et renvoie les messages a pousser. */
    @Transactional
    public List<PushMessage> createForMoneyMoved(MoneyMovedEvent e) {
        List<PushMessage> messages = new ArrayList<>();

        Account to = accounts.findById(e.toAccountId()).orElse(null);
        if (to == null) {
            return messages;
        }
        Account from = accounts.findById(e.fromAccountId()).orElse(null);
        boolean isDeposit = DepositService.EXTERNAL_ACCOUNT_ID.equals(e.fromAccountId());
        String amount = formatMinor(e.amountMinor());

        // Beneficiaire (compte credite)
        if (to.getOwnerId() != null) {
            if (isDeposit) {
                messages.add(save(to.getOwnerId(), "DEPOSIT", "Dépôt reçu",
                        "Vous avez reçu " + amount + " (dépôt)."));
            } else {
                String fromName = from != null ? from.getOwnerName() : "un tiers";
                messages.add(save(to.getOwnerId(), "TRANSFER_IN", "Paiement reçu",
                        "Vous avez reçu " + amount + " de " + fromName + "."));
            }
        }

        // Emetteur (compte debite), sauf pour un depot (compte exterieur)
        if (!isDeposit && from != null && from.getOwnerId() != null) {
            messages.add(save(from.getOwnerId(), "TRANSFER_OUT", "Virement envoyé",
                    "Vous avez envoyé " + amount + " à " + to.getOwnerName() + "."));
        }

        return messages;
    }

    private PushMessage save(UUID userId, String type, String title, String body) {
        notifications.save(new Notification(UUID.randomUUID(), userId, type, title, body));
        return new PushMessage(userId, title, body);
    }

    @Transactional(readOnly = true)
    public List<Notification> list(UUID userId) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notifications.markAllRead(userId);
    }

    private static String formatMinor(long minor) {
        return String.format("%.2f €", minor / 100.0).replace('.', ',');
    }

    /** Message destine au push web (un par utilisateur notifie). */
    public record PushMessage(UUID userId, String title, String body) {}
}
