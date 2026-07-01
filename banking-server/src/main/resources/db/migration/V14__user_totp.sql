-- =====================================================================
--  Authentification forte (2FA / TOTP) par utilisateur.
--  secret : clef partagee (base32). enabled : true une fois confirmee.
-- =====================================================================

CREATE TABLE user_totp (
    user_id      UUID         PRIMARY KEY REFERENCES users(id),
    secret       VARCHAR(64)  NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT false,
    confirmed_at TIMESTAMPTZ
);
