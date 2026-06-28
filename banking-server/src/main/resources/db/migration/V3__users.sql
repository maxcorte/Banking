-- =====================================================================
--  Utilisateurs (authentification)
--  Les mots de passe ne sont JAMAIS stockes en clair : on stocke un
--  hachage BCrypt (~60 caracteres).
-- =====================================================================

CREATE TABLE users (
    id             UUID          PRIMARY KEY,
    username       VARCHAR(100)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100)  NOT NULL,
    role           VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'))
);
