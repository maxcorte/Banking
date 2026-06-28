# GitHub + CI/CD — pas à pas

Ces fichiers s'extraient **à la racine du projet** (`apllication bancaire`). Ils
ajoutent `.gitignore` et le dossier `.github/workflows/`.

L'objectif de cette étape :
1. mettre ton code sur **GitHub** (proprement, sans jamais y envoyer tes secrets) ;
2. faire tourner tes **tests automatiquement** à chaque modification (CI) ;
3. (étape suivante) déployer automatiquement sur le VPS (CD).

---

## 0. Pré-requis

- Un **compte GitHub** (gratuit, sur github.com).
- **Git** installé sur ton PC. Vérifie dans PowerShell :
  ```powershell
  git --version
  ```
  S'il manque : `winget install Git.Git` (puis rouvre PowerShell).

---

## 1. Crée un dépôt vide sur GitHub

Sur github.com → bouton **New** (nouveau dépôt) :
- Nom : par exemple `banking-app`
- **Ne coche RIEN** (pas de README, pas de .gitignore, pas de licence) — le dépôt
  doit être vide pour éviter les conflits au premier push.
- Public (idéal pour un portfolio) ou Privé, à ton choix.
- Clique **Create repository**.

GitHub affiche alors une URL du type :
`https://github.com/TON-PSEUDO/banking-app.git` — garde-la sous la main.

---

## 2. Vérifie que tes secrets sont bien ignorés

**Crucial.** Avant tout commit, on s'assure que le `.env` (qui contient tes mots
de passe et le JWT_SECRET) ne partira PAS sur GitHub. Dans PowerShell :

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire"
git init
git add .
git status
```

Regarde la liste affichée par `git status` : **`.env` ne doit PAS y apparaître**
(grâce au `.gitignore`). Tu dois voir `.env.example`, `Dockerfile`,
`docker-compose.prod.yml`, `banking-server/`, `banking-frontend/`, etc. — mais
jamais `.env`, ni `node_modules`, ni `target`.

> Si `.env` apparaît quand même, ARRÊTE-TOI et dis-le-moi avant de continuer.

---

## 3. Premier commit + push

```powershell
git config user.name "Maxime Delcorte"
git config user.email "ton-email@example.com"

git commit -m "Application bancaire : backend Spring + frontend React, tests, déploiement Docker"
git branch -M main
git remote add origin https://github.com/TON-PSEUDO/banking-app.git
git push -u origin main
```

Au `push`, une fenêtre de connexion GitHub s'ouvre dans ton navigateur
(Git Credential Manager) : connecte-toi, et l'autorisation est mémorisée pour la
suite. C'est tout.

---

## 4. Regarde la CI tourner

Va sur ton dépôt GitHub → onglet **Actions**. Tu verras le workflow « CI » se
lancer automatiquement, avec deux travaux :
- **Tests du grand livre (backend)** — démarre un PostgreSQL et exécute tes 8 tests.
- **Build du frontend** — compile React et vérifie le typage TypeScript.

Quand les deux affichent une coche verte ✅, ta comptabilité est validée par des
machines neutres, à chaque modification. Tu peux même ajouter le badge de statut
en haut de ton futur README (`![CI](https://github.com/TON-PSEUDO/banking-app/actions/workflows/ci.yml/badge.svg)`).

---

## 5. Au quotidien

Désormais, pour publier une modification :

```powershell
git add .
git commit -m "Description de la modif"
git push
```

La CI relance les tests à chaque push. Si un test casse, GitHub te prévient (mail
+ croix rouge) — tu sais immédiatement que quelque chose ne va pas, avant même de
déployer.

---

## Et ensuite : le déploiement automatique (CD)

Une fois la CI au vert, on ajoutera un second workflow qui, après des tests
réussis sur `main`, se connecte en SSH à ton VPS et fait `git pull` +
`docker compose up -d --build`. Plus de `scp` manuel : tu pousses, et la prod se
met à jour toute seule si (et seulement si) les tests passent. Ça demandera de
créer une clé SSH dédiée et d'enregistrer quelques « secrets » dans GitHub — je
te guiderai.
