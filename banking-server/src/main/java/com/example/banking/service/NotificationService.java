package com.example.banking.service;

import com.example.banking.domain.Account;
import com.example.banking.domain.Notification;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cree et expose les notifications in-app. La creation se fait en reaction a un
 * MoneyMovedEvent, APRES le commit du virement (voir NotificationEventListener) :
 * une eventuelle erreur de notification n'affecte donc jamais le grand livre.
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final AccountRepository accounts;

    public NotificationService(NotificationRepository notifications, AccountRepository accounts) {
        this.notifications = notifications;
        this.accounts = accounts;
    }

    /** Genere les notifications pour les proprietaires des comptes concernes. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForMoneyMoved(MoneyMovedEvent e) {
        Account to = accounts.findById(e.toAccountId()).orElse(null);
        if (to == null) {
            return;
        }
        Account from = accounts.findById(e.fromAccountId()).orElse(null);
        boolean isDeposit = DepositService.EXTERNAL_ACCOUNT_ID.equals(e.fromAccountId());
        String amount = formatMinor(e.amountMinor());

        // Beneficiaire (compte credite)
        if (to.getOwnerId() != null) {
            if (isDeposit) {
                save(to.getOwnerId(), "DEPOSIT", "Dépôt reçu",
                        "Vous avez reçu " + amount + " (dépôt).");
            } else {
                String fromName = from != null ? from.getOwnerName() : "un tiers";
                save(to.getOwnerId(), "TRANSFER_IN", "Paiement reçu",
                        "Vous avez reçu " + amount + " de " + fromName + ".");
            }
        }

        // Emetteur (compte debite), sauf pour un depot (compte exterieur)
        if (!isDeposit && from != null && from.getOwnerId() != null) {
            save(from.getOwnerId(), "TRANSFER_OUT", "Virement envoyé",
                    "Vous avez envoyé " + amount + " à " + to.getOwnerName() + ".");
        }
    }

    private void save(UUID userId, String type, String title, String body) {
        notifications.save(new Notification(UUID.randomUUID(), userId, type, title, body));
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
        // Format simple "12,34 €" (virgule decimale a la francaise).
        return String.format("%.2f €", minor / 100.0).replace('.', ',');
    }
}
