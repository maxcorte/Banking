# Opti UI : audit + PDF mobile + footer de navigation

## 1. Journal d'audit
- **Responsive mobile** : chaque ligne devient une carte lisible (plus de
  table qui déborde).
- **Pagination** : 25 entrées par page, boutons Précédent / Suivant + compteur
  « x–y sur N » (le journal ne défile plus à l'infini).
- **Recherche** : champ qui filtre sur l'acteur, l'action ou le détail
  (côté serveur, donc rapide même avec beaucoup d'entrées).
- Backend : `/api/audit?q=&page=&size=` renvoie désormais une page
  `{ items, page, size, total, hasMore }`.

## 2. Relevé PDF (mobile)
- Génère un **vrai fichier PDF téléchargeable** (`releve-XXXX.pdf`) via jsPDF.
- Le navigateur mobile propose donc de l'enregistrer / partager : fini
  l'onglet imprimable bloqué sans retour possible.
- La lib n'est chargée que si on clique sur « Relevé PDF » (import dynamique).

## 3. Header allégé → footer de navigation
- Le header ne garde que : marque, cloche, thème, déconnexion.
- Une **barre du bas** (collante, safe-area iOS) regroupe la navigation :
  Comptes · Recevoir · Scanner · Demandes · Stats · Audit (admin).
- Règle aussi la visibilité des boutons sur petit écran.

## Important
- `package-lock.json` est inclus (nouvelles dépendances jspdf) : nécessaire
  pour `npm ci` sur le CI et dans le Docker build.
- La migration de la base n'est pas touchée ici.
