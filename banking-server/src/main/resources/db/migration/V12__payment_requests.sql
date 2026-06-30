-- =====================================================================
--  Demandes de remboursement entre utilisateurs (avec suivi de statut).
-- =====================================================================

CREATE TABLE payment_requests (
    id                UUID         PRIMARY KEY,
    requester_user_id UUID         NOT NULL REFERENCES users(id),
    to_account_id     UUID         NOT NULL REFERENCES accounts(id),
    payer_user_id     UUID         NOT NULL REFERENCES users(id),
    amount_minor      BIGINT       NOT NULL CHECK (amount_minor > 0),
    currency          VARCHAR(3)   NOT NULL,
    description       VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    from_account_id   UUID         REFERENCES accounts(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMPTZ
);

CREATE INDEX idx_payreq_payer     ON payment_requests (payer_user_id, status, created_at DESC);
CREATE INDEX idx_payreq_requester ON payment_requests (requester_user_id, created_at DESC);
