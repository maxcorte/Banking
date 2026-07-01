-- =====================================================================
--  Passkeys (WebAuthn) : cles publiques enregistrees par utilisateur.
--  credential_id / public_key_cose / user_handle sont stockes en base64url.
-- =====================================================================

CREATE TABLE webauthn_credentials (
    id               UUID          PRIMARY KEY,
    user_id          UUID          NOT NULL REFERENCES users(id),
    credential_id    VARCHAR(512)  NOT NULL UNIQUE,
    public_key_cose  TEXT          NOT NULL,
    signature_count  BIGINT        NOT NULL DEFAULT 0,
    user_handle      VARCHAR(128)  NOT NULL,
    label            VARCHAR(120)  NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_webauthn_user   ON webauthn_credentials(user_id);
CREATE INDEX idx_webauthn_handle ON webauthn_credentials(user_handle);
