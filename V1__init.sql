-- =====================================================================
--  Schéma du registre bancaire en partie double
--  Principe : aucun solde "magique". Toute somme d'argent est tracée
--  par des écritures (postings) immuables. Le solde d'un compte est
--  la somme de ses écritures. Une transaction = un ensemble d'écritures
--  dont la somme vaut TOUJOURS zéro (l'argent ne se crée ni ne disparaît).
--  Tous les montants sont en unités mineures (centimes) : des entiers,
--  JAMAIS des flottants.
-- =====================================================================

CREATE TABLE accounts (
    id              UUID            PRIMARY KEY,
    account_number  VARCHAR(34)     NOT NULL UNIQUE,   -- type IBAN, lisible
    owner_name      VARCHAR(200)    NOT NULL,
    currency        CHAR(3)         NOT NULL,          -- code ISO 4217, ex. EUR
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / FROZEN / CLOSED
    -- Solde mis en cache, recalculé sous verrou à chaque mouvement.
    -- La source de vérité reste la somme des écritures (postings).
    balance_minor   BIGINT          NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,  -- verrouillage optimiste JPA
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

-- Une transaction représente une opération financière complète
-- (ex. un virement de A vers B). Elle regroupe plusieurs écritures.
CREATE TABLE transactions (
    id               UUID           PRIMARY KEY,
    reference        VARCHAR(50)    NOT NULL UNIQUE,    -- référence métier lisible
    -- Clé d'idempotence : si le client renvoie la même requête (réseau coupé),
    -- on retrouve la transaction existante au lieu d'en créer une nouvelle.
    idempotency_key  VARCHAR(100)   UNIQUE,
    description      VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Les écritures : immuables. On n'UPDATE ni ne DELETE jamais une ligne.
-- Une annulation se fait par une écriture compensatoire (de signe opposé).
-- amount_minor : signé. Négatif = débit, positif = crédit.
CREATE TABLE postings (
    id              UUID            PRIMARY KEY,
    transaction_id  UUID            NOT NULL REFERENCES transactions(id),
    account_id      UUID            NOT NULL REFERENCES accounts(id),
    amount_minor    BIGINT          NOT NULL,
    currency        CHAR(3)         NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_amount_nonzero CHECK (amount_minor <> 0)
);

CREATE INDEX idx_postings_account     ON postings(account_id);
CREATE INDEX idx_postings_transaction ON postings(transaction_id);

-- =====================================================================
--  Invariant de partie double, garanti par la base elle-même :
--  pour CHAQUE transaction, la somme des écritures doit valoir 0.
--  Ce trigger refuse tout déséquilibre. C'est la dernière ligne de
--  défense, même si du code applicatif bogué tentait de tricher.
-- =====================================================================
CREATE OR REPLACE FUNCTION check_transaction_balanced()
RETURNS TRIGGER AS $$
DECLARE
    total BIGINT;
BEGIN
    SELECT COALESCE(SUM(amount_minor), 0) INTO total
    FROM postings
    WHERE transaction_id = NEW.transaction_id;

    IF total <> 0 THEN
        RAISE EXCEPTION
            'Transaction % desequilibree : somme des ecritures = % (doit valoir 0)',
            NEW.transaction_id, total;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Vérifié à la fin de la transaction SQL (DEFERRABLE), une fois toutes
-- les écritures insérées.
CREATE CONSTRAINT TRIGGER trg_transaction_balanced
    AFTER INSERT ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION check_transaction_balanced();
