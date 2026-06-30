# Correctifs : audit 500, PDF, footer

## Audit (erreur 500)
La requête utilisait `:q is null` ; avec Hibernate 7, un paramètre null dans
une comparaison de ce type fait échouer la requête. Corrigé :
- pas de recherche → `findAll(pageable)` trié par date (desc) ;
- recherche → requête `LIKE` avec `q` toujours non-null.

## PDF (colonnes coupées / chiffres éclatés)
`Intl.NumberFormat` insère des espaces fines insécables (U+202F) et un moins
typographique (U+2212) que la police PDF standard ne sait pas dessiner →
chiffres décalés et colonnes coupées. Formatage repassé en **ASCII pur**
(espace normale, vrai `-`, « EUR ») + largeurs de colonnes fixes.

## Footer
`position: sticky` ne colle pas quand la page ne défile pas. Passé en
`position: fixed` en bas de l'écran, avec une marge basse sur la coquille
pour que le pied légal reste visible au-dessus.
