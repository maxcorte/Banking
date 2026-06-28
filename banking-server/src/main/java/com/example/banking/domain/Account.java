package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** Solde en cache, en unités mineures (centimes). Mis à jour sous verrou. */
    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor = 0L;

    /**
     * Un compte "monde extérieur" autorisé à passer en négatif : il modélise
     * l'entrée d'argent dans le système (dépôts). Faux pour les comptes clients.
     */
    @Column(name = "allow_negative_balance", nullable = false)
    private boolean allowNegativeBalance = false;

    /** Propriétaire du compte (null pour le compte "monde extérieur"). */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Verrouillage optimiste géré par JPA/Hibernate. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Account() {
        // requis par JPA
    }

    public Account(UUID id, String accountNumber, String ownerName, String currency) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    public void credit(long amountMinor) {
        this.balanceMinor += amountMinor;
    }

    public void debit(long amountMinor) {
        this.balanceMinor -= amountMinor;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean allowsNegativeBalance() {
        return allowNegativeBalance;
    }

    // --- getters ---
    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public long getBalanceMinor() { return balanceMinor; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public void setStatus(AccountStatus status) { this.status = status; }
}
