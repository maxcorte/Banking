package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Secret TOTP d'un utilisateur (2FA). enabled=true une fois confirme. */
@Entity
@Table(name = "user_totp")
public class UserTotp {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    protected UserTotp() {
        // requis par JPA
    }

    public UserTotp(UUID userId, String secret) {
        this.userId = userId;
        this.secret = secret;
    }

    public void resetSecret(String newSecret) {
        this.secret = newSecret;
        this.enabled = false;
        this.confirmedAt = null;
    }

    public void confirm() {
        this.enabled = true;
        this.confirmedAt = OffsetDateTime.now();
    }

    public UUID getUserId() { return userId; }
    public String getSecret() { return secret; }
    public boolean isEnabled() { return enabled; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
}
