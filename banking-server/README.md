# banking-server

Squelette de **serveur bancaire pédagogique** : registre comptable en **partie double**,
verrouillage anti-double-spend, idempotence des virements. Montants **factices**, aucun
lien avec un vrai système de paiement.

> Stack : **Java 21**, **Spring Boot 4.1**, **PostgreSQL 17**, Flyway, Testcontainers.

---

## 1. Idée directrice

Une vraie banque ne stocke pas un simple champ `solde`. Chaque mouvement d'argent est
écrit comme un ensemble d'**écritures** (`postings`) **immuables** dont la somme vaut
toujours **zéro** : l'argent ne se crée ni ne disparaît. Le solde se *déduit* de ces
écritures.

Trois garde-fous d'ingénierie :

1. **Argent en entiers (centimes)** — jamais de `float`/`double`. (`Money` + `BIGINT`)
2. **Transactions ACID + verrou pessimiste** (`SELECT … FOR UPDATE`) — empêche qu'un
   compte soit débité deux fois en parallèle.
3. **Clé d'idempotence** — rejouer une requête (réseau coupé) ne débite pas deux fois.

En dernier recours, un **trigger PostgreSQL** refuse toute transaction déséquilibrée,
même si du code bogué essayait de tricher.

## 2. Architecture des dossiers

```
domain/       Account, BankTransaction, Posting   (entités JPA)
repository/   accès données + verrou FOR UPDATE
service/      AccountService, TransferService      (logique métier, @Transactional)
web/          contrôleurs REST + DTO
exception/    exceptions métier + handler global (ProblemDetail)
resources/db/migration/  schéma SQL versionné (Flyway)
```

## 3. Lancer en local

Prérequis : **JDK 21**, **Maven**, **Docker**.

```bash
# 1. Démarrer PostgreSQL
docker compose up -d

# 2. Lancer le serveur (Flyway applique le schéma au démarrage)
mvn spring-boot:run
```

Le serveur écoute sur `http://localhost:8080`.

## 4. Essayer l'API

```bash
# Créer deux comptes
curl -s -X POST localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Alice","currency":"EUR"}'

curl -s -X POST localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Bob","currency":"EUR"}'

# Virement de 25,00 EUR (2500 centimes) avec clé d'idempotence
curl -s -X POST localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{"fromAccountId":"<UUID_ALICE>","toAccountId":"<UUID_BOB>","amountMinor":2500,"description":"test"}'

# Consulter un solde
curl -s localhost:8080/api/accounts/<UUID_ALICE>

# Historique des écritures d'un compte
curl -s localhost:8080/api/accounts/<UUID_ALICE>/history
```

> Note : les nouveaux comptes démarrent à 0. Pour tester un virement, il faut d'abord
> alimenter un compte — la prochaine étape naturelle est un endpoint de
> dépôt/approvisionnement (voir pistes ci-dessous).

## 5. Tests

```bash
mvn test
```

Les tests d'intégration tournent sur une **vraie** base PostgreSQL jetable
(Testcontainers) — Docker doit être disponible. Ils vérifient : virement nominal,
équilibre des écritures, refus pour solde insuffisant, et idempotence.

## 6. Endpoints

| Méthode | Chemin                          | Rôle                          |
|---------|---------------------------------|-------------------------------|
| POST    | `/api/accounts`                 | Créer un compte               |
| GET     | `/api/accounts/{id}`            | Consulter un compte + solde   |
| GET     | `/api/accounts/{id}/history`    | Lister les écritures          |
| POST    | `/api/transfers`                | Virement (en-tête `Idempotency-Key`) |

## 7. Pistes pour la suite

- **Dépôt / approvisionnement** : virement depuis un compte "monde extérieur" autorisé
  à passer en négatif (modélise l'entrée d'argent dans le système).
- **Authentification** : Spring Security + JWT/OAuth2 (Keycloak en entreprise).
- **Audit log** immuable de toutes les opérations.
- **Frontend React + TypeScript**.
- **Kafka** : publier un événement par transaction (audit / event sourcing).
- **Multi-devises** avec table de taux de change.
```
