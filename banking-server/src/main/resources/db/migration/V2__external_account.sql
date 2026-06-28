-- =====================================================================
--  Compte "monde exterieur" et depots
--  L'argent entre dans le systeme depuis ce compte special, autorise a
--  passer en negatif. Un depot = un virement de ce compte vers le client.
--  Son solde negatif represente la masse d'argent en circulation.
-- =====================================================================

ALTER TABLE accounts
    ADD COLUMN allow_negative_balance BOOLEAN NOT NULL DEFAULT false;

-- Compte systeme a identifiant fixe et connu (reference par le code).
INSERT INTO accounts
    (id, account_number, owner_name, currency, status, balance_minor, version, allow_negative_balance)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'EXTERNAL', 'MONDE EXTERIEUR',
     'EUR', 'ACTIVE', 0, 0, true);
