package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Une écriture comptable. IMMUABLE : jamais modifiée ni supprimée après
 * création. amountMinor est signé : négatif = débit, positif = crédit.
 */
@Entity
@Table(name = "postings")
public class Posting {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private BankTransaction transaction;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Posting() {
        // requis par JPA
    }

    public Posting(UUID id, UUID accountId, long amountMinor, String currency) {
        this.id = id;
        this.accountId = accountId;
        this.amountMinor = amountMinor;
        this.currency = currency;
    }

    void setTransaction(BankTransaction transaction) {
        this.transaction = transaction;
    }

    // --- getters ---
    public UUID getId() { return id; }
    public BankTransaction getTransaction() { return transaction; }
    public UUID getAccountId() { return accountId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
