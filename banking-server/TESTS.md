# Tests automatisés du grand livre

Ces fichiers s'extraient dans le dossier **`banking-server`** (ils ajoutent le
dossier `src/test/...` et mettent à jour `pom.xml`).

## Lancer les tests

Pré-requis : **Docker doit être démarré** (Docker Desktop sur Windows). Les tests
lancent un vrai PostgreSQL jetable via Testcontainers — identique à ta prod.

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire\banking-server"
mvn test
```

Le premier lancement télécharge l'image `postgres:17` et les dépendances de test
(un peu long) ; les suivants sont rapides. À la fin, un rapport indique le nombre
de tests passés.

## Ce qui est couvert

**`LedgerIntegrationTest`** — les invariants comptables :
- un virement déplace **exactement** le montant (et le solde reste cohérent avec
  la somme des écritures) ;
- un virement est en **partie double** : deux écritures opposées dont la somme
  vaut zéro ;
- un virement **supérieur au solde est refusé**, sans déplacer d'argent ;
- la **clé d'idempotence** empêche de rejouer (donc de débiter deux fois) ;
- virement vers soi-même et montant nul/négatif sont refusés ;
- le **grand livre entier somme toujours à zéro** (compte « monde extérieur »
  inclus) — l'invariant fondamental de la partie double.

**`ConcurrentTransferTest`** — la robustesse sous charge :
- deux virements **simultanés** vidant le même compte : **un seul réussit**
  (preuve que le verrou `SELECT ... FOR UPDATE` empêche le double-spend) ;
- vingt virements simultanés sur un solde qui n'en couvre que dix :
  **exactement dix passent**, le solde finit à zéro et reste cohérent.

## Pourquoi Testcontainers plutôt qu'une base en mémoire

Une base H2 en mémoire ne reproduit pas fidèlement PostgreSQL : ni le
verrouillage `FOR UPDATE` réel, ni le comportement exact des migrations Flyway.
En testant contre un vrai PostgreSQL, on valide le code **tel qu'il tournera en
production** — c'est la pratique professionnelle, et un bon point à montrer.

## Et ensuite (étape 4)

Ces tests sont la fondation de la CI : à l'étape suivante, GitHub Actions les
lancera automatiquement à chaque `push`, et bloquera tout déploiement si l'un
d'eux échoue. Personne ne pourra casser la comptabilité sans que la CI ne le
voie.
