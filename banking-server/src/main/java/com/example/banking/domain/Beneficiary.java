package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String label;

    @Column(name = "account_number", nullable = false, updatable = false)
    private String accountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Beneficiary() {
        // requis par JPA
    }

    public Beneficiary(UUID id, UUID ownerId, String label, String accountNumber) {
        this.id = id;
        this.ownerId = ownerId;
        this.label = label;
        this.accountNumber = accountNumber;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getLabel() { return label; }
    public String getAccountNumber() { return accountNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
