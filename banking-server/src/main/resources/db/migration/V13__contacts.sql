-- =====================================================================
--  Carnet de contacts : les utilisateurs qu'un compte connaît, pour
--  pré-remplir le destinataire d'une demande de remboursement.
-- =====================================================================

CREATE TABLE contacts (
    id              UUID         PRIMARY KEY,
    owner_user_id   UUID         NOT NULL REFERENCES users(id),
    contact_user_id UUID         NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_contact UNIQUE (owner_user_id, contact_user_id),
    CONSTRAINT no_self_contact CHECK (owner_user_id <> contact_user_id)
);

CREATE INDEX idx_contacts_owner ON contacts (owner_user_id, created_at DESC);
