package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Demande de remboursement : un utilisateur demande de l'argent a un autre. */
@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

    @Id
    private UUID id;

    @Column(name = "requester_user_id", nullable = false, updatable = false)
    private UUID requesterUserId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private UUID toAccountId;

    @Column(name = "payer_user_id", nullable = false, updatable = false)
    private UUID payerUserId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRequestStatus status = PaymentRequestStatus.PENDING;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected PaymentRequest() {
        // requis par JPA
    }

    public PaymentRequest(UUID id, UUID requesterUserId, UUID toAccountId, UUID payerUserId,
                          long amountMinor, String currency, String description) {
        this.id = id;
        this.requesterUserId = requesterUserId;
        this.toAccountId = toAccountId;
        this.payerUserId = payerUserId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.description = description;
    }

    public void markAccepted(UUID fromAccountId) {
        this.status = PaymentRequestStatus.ACCEPTED;
        this.fromAccountId = fromAccountId;
        this.resolvedAt = OffsetDateTime.now();
    }

    public void markRefused() {
        this.status = PaymentRequestStatus.REFUSED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public void markCancelled() {
        this.status = PaymentRequestStatus.CANCELLED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public UUID getToAccountId() { return toAccountId; }
    public UUID getPayerUserId() { return payerUserId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public PaymentRequestStatus getStatus() { return status; }
    public UUID getFromAccountId() { return fromAccountId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
}
