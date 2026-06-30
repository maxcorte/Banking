-- =====================================================================
--  Notifications in-app (cloche) : un enregistrement par evenement
--  destine a un utilisateur (paiement recu / envoye, depot).
-- =====================================================================

CREATE TABLE notifications (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    type        VARCHAR(40)  NOT NULL,           -- TRANSFER_IN / TRANSFER_OUT / DEPOSIT
    title       VARCHAR(140) NOT NULL,
    body        VARCHAR(400),
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_user_date ON notifications (user_id, created_at DESC);
