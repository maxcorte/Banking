-- =====================================================================
--  Categorie de transaction (choisie au moment du virement)
--  Categories predefinies : COURSES, LOYER, SALAIRE, etc.
--  Les transactions existantes restent NULL (traitees comme "AUTRES").
-- =====================================================================

ALTER TABLE transactions ADD COLUMN category VARCHAR(40);
