package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un contact : l'utilisateur `contactUserId` enregistre par `ownerUserId`. */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "contact_user_id", nullable = false, updatable = false)
    private UUID contactUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Contact() {
        // requis par JPA
    }

    public Contact(UUID id, UUID ownerUserId, UUID contactUserId) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.contactUserId = contactUserId;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getContactUserId() { return contactUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
