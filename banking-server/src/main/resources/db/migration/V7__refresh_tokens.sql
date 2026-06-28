-- =====================================================================
--  Refresh tokens (jetons de rafraichissement)
--  Stockes HACHES (jamais en clair). Permettent de renouveler un jeton
--  d'acces court, et d'etre revoques (vraie deconnexion cote serveur).
-- =====================================================================

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,   -- SHA-256 hex du jeton
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
