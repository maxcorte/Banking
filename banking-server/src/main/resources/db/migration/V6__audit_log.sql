-- =====================================================================
--  Journal d'audit
--  Trace les actions sensibles : connexions, virements, depots,
--  creations/clotures de comptes, gestion des beneficiaires.
--  Append-only par usage (on n'expose ni modification ni suppression).
-- =====================================================================

CREATE TABLE audit_log (
    id      UUID         PRIMARY KEY,
    at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor   VARCHAR(150),          -- identifiant de l'acteur (ou 'anonyme')
    action  VARCHAR(60)  NOT NULL, -- ex. LOGIN_SUCCESS, TRANSFER, DEPOSIT
    detail  TEXT                   -- contexte lisible (montant, comptes...)
);

CREATE INDEX idx_audit_at ON audit_log (at DESC);
