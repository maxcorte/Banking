-- =====================================================================
--  Proprietaire des comptes
--  Chaque compte client appartient desormais a un utilisateur.
--  Le compte "monde exterieur" reste sans proprietaire (owner_id NULL).
--  Les comptes existants seront rattaches a l'admin au demarrage
--  (voir DataSeeder), une fois l'admin cree.
-- =====================================================================

ALTER TABLE accounts
    ADD COLUMN owner_id UUID REFERENCES users(id);
