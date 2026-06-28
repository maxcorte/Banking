-- =====================================================================
--  Beneficiaires enregistres
--  Chaque utilisateur peut sauvegarder des destinataires de virement
--  (un libelle + un numero de compte/IBAN) pour les reutiliser.
-- =====================================================================

CREATE TABLE beneficiaries (
    id              UUID          PRIMARY KEY,
    owner_id        UUID          NOT NULL REFERENCES users(id),
    label           VARCHAR(200)  NOT NULL,
    account_number  VARCHAR(34)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_beneficiary UNIQUE (owner_id, account_number)
);

CREATE INDEX idx_beneficiaries_owner ON beneficiaries(owner_id);
