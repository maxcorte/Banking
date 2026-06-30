-- =====================================================================
--  Abonnements aux notifications push web (un par navigateur/endpoint).
-- =====================================================================

CREATE TABLE push_subscriptions (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    endpoint    TEXT         NOT NULL UNIQUE,
    p256dh      TEXT         NOT NULL,
    auth        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_sub_user ON push_subscriptions (user_id);
