package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime at;

    @Column(updatable = false)
    private String actor;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(columnDefinition = "text", updatable = false)
    private String detail;

    protected AuditEntry() {
        // requis par JPA
    }

    public AuditEntry(UUID id, OffsetDateTime at, String actor, String action, String detail) {
        this.id = id;
        this.at = at;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
    }

    public UUID getId() { return id; }
    public OffsetDateTime getAt() { return at; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
