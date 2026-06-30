-- =====================================================================
--  E-mail des utilisateurs + jetons de reinitialisation de mot de passe
--
--  - Ajout d'une colonne email (nullable : les comptes existants n'en ont
--    pas ; unique parmi les valeurs renseignees).
--  - Table des jetons de reinitialisation : stockes HACHES (SHA-256), a
--    usage unique, avec expiration. Meme convention que refresh_tokens.
-- =====================================================================

ALTER TABLE users ADD COLUMN email VARCHAR(255);

-- Unicite des e-mails renseignes (PostgreSQL autorise plusieurs NULL).
CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE TABLE password_reset_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,   -- SHA-256 hex du jeton
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_reset_user ON password_reset_tokens (user_id);
