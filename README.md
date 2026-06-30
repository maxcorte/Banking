# Banking — application bancaire de démonstration

[![CI](https://github.com/maxcorte/Banking/actions/workflows/ci.yml/badge.svg)](https://github.com/maxcorte/Banking/actions/workflows/ci.yml)

Application web complète simulant le cœur d'une banque : comptes, virements,
historique et statistiques, bâtie sur un **registre comptable en partie double**.
Backend Spring Boot, frontend React/TypeScript, le tout conteneurisé, testé en
continu et déployé automatiquement en production.

🔗 **Démo en ligne : [maximedelcorte.cloud](https://maximedelcorte.cloud)**

> ⚠️ **Projet pédagogique.** Comptes, soldes et opérations sont **fictifs**.
> Aucun service bancaire réel n'est fourni, aucune somme d'argent réelle n'est en
> jeu. Ce dépôt sert à démontrer des choix d'ingénierie, pas à exploiter une banque.

---

## Pourquoi ce projet est intéressant

Une vraie banque ne stocke pas un simple champ « solde ». Chaque mouvement d'argent
est écrit comme un ensemble d'**écritures immuables** (*postings*) dont la somme
vaut toujours **zéro** : l'argent ne se crée ni ne disparaît, et le solde se
*déduit* de ces écritures. Ce projet implémente ce modèle, avec les garde-fous qui
vont avec :

- **Partie double** — chaque virement génère des écritures équilibrées ; un
  **trigger PostgreSQL** refuse, en dernier recours, toute transaction déséquilibrée.
- **Montants en entiers (centimes)** — jamais de `float`/`double`, donc pas d'erreur
  d'arrondi sur l'argent.
- **Verrou pessimiste** (`SELECT … FOR UPDATE`) — empêche qu'un compte soit débité
  deux fois lors de virements concurrents (protection anti-double-spend).
- **Idempotence** — rejouer une requête (réseau coupé) ne débite pas deux fois.
- **Authentification par JWT en cookie httpOnly** + jetons de rafraîchissement, mots
  de passe hachés (bcrypt), endpoints cloisonnés par utilisateur.

Ces propriétés ne sont pas seulement affirmées : elles sont **vérifiées par une
suite de tests automatisés**, dont des tests de **concurrence** qui prouvent qu'un
seul de deux virements simultanés peut réussir.

## Fonctionnalités

- Inscription / connexion sécurisée
- Création de comptes, dépôts, virements entre comptes et vers des bénéficiaires
- Historique des opérations avec catégories
- Tableau de bord avec **statistiques de dépenses** (graphique en anneau par catégorie)
- Journal d'audit (côté admin)
- Thème clair / sombre, interface responsive
- Pages légales / RGPD

## Stack technique

| Couche           | Technologies                                                       |
|------------------|--------------------------------------------------------------------|
| Backend          | Java 21, Spring Boot 4.1, Spring Security, JPA/Hibernate, Flyway    |
| Base de données  | PostgreSQL 17 (registre en partie double, trigger d'intégrité)     |
| Frontend         | React 18, TypeScript, Vite                                         |
| Tests            | JUnit 5, Testcontainers (PostgreSQL réel et jetable)               |
| Conteneurisation | Docker (build multi-étapes), Docker Compose                       |
| Reverse proxy    | Caddy (HTTPS automatique via Let's Encrypt, en-têtes de sécurité)  |
| CI / CD          | GitHub Actions (tests à chaque push, déploiement auto sur VPS)     |

## Architecture du dépôt

```
.
├── banking-server/         # Backend Spring Boot (API + service du grand livre)
├── banking-frontend/       # Frontend React/TypeScript (Vite)
├── Dockerfile              # Build multi-étapes : frontend + backend → une image
├── docker-compose.prod.yml # Production : app + PostgreSQL + Caddy
├── Caddyfile               # HTTPS automatique + en-têtes de sécurité
├── backup-db.sh            # Sauvegarde PostgreSQL (pg_dump) avec rotation
└── .github/workflows/      # ci.yml (tests) + deploy.yml (déploiement auto)
```

## Lancer en local

Prérequis : Docker (pour PostgreSQL), Java 21, Node 20.

```bash
# 1) Base de données de développement
cd banking-server
docker compose up -d            # PostgreSQL sur localhost:5432

# 2) Backend (API sur http://localhost:8080)
mvn spring-boot:run

# 3) Frontend (dans un autre terminal)
cd ../banking-frontend
npm install
npm run dev                     # interface sur http://localhost:5173
```

Ou bien, tout d'un coup avec l'image de production (frontend servi par le backend) :

```bash
cp .env.example .env            # remplir les secrets ; COOKIE_SECURE=false en local
docker compose -f docker-compose.prod.yml up -d --build
# Application complète sur http://localhost:8080
```

## Tests

```bash
cd banking-server
mvn test
```

La suite démarre un vrai PostgreSQL via Testcontainers (Docker requis) et valide
notamment : conservation de la somme à zéro du grand livre, rejet des fonds
insuffisants, idempotence, et **résolution correcte de virements concurrents**.

## Déploiement (CI/CD)

À chaque `git push` sur `main`, **GitHub Actions** exécute les tests et compile le
frontend. **Si tout est vert**, un second workflow se connecte en SSH au VPS et met
la production à jour (`git pull` + reconstruction Docker). Aucune intervention
manuelle : pousser du code suffit à livrer.

L'hébergement repose sur un VPS, avec Caddy en façade (HTTPS automatique et
en-têtes de sécurité), PostgreSQL en volume persistant, et des sauvegardes
quotidiennes de la base.

## Licence

Projet publié à titre pédagogique. Les bibliothèques tierces restent la propriété
de leurs auteurs respectifs.
