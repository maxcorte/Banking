package com.example.banking.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Passkey (WebAuthn) enregistree par un utilisateur. Valeurs binaires en base64url. */
@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "credential_id", nullable = false, updatable = false, unique = true)
    private String credentialId;

    @Column(name = "public_key_cose", nullable = false, updatable = false, columnDefinition = "text")
    private String publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private long signatureCount;

    @Column(name = "user_handle", nullable = false, updatable = false)
    private String userHandle;

    @Column(nullable = false)
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected WebAuthnCredential() {
        // requis par JPA
    }

    public WebAuthnCredential(UUID id, UUID userId, String credentialId, String publicKeyCose,
                              long signatureCount, String userHandle, String label) {
        this.id = id;
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKeyCose = publicKeyCose;
        this.signatureCount = signatureCount;
        this.userHandle = userHandle;
        this.label = label;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCredentialId() { return credentialId; }
    public String getPublicKeyCose() { return publicKeyCose; }
    public long getSignatureCount() { return signatureCount; }
    public String getUserHandle() { return userHandle; }
    public String getLabel() { return label; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
