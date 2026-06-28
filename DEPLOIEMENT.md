# Déploiement de l'application bancaire

Ces fichiers se placent **à la racine du projet**, c'est-à-dire dans le dossier
`apllication bancaire`, à côté des dossiers `banking-server` et `banking-frontend` :

```
apllication bancaire/
├── Dockerfile                ← build complet (frontend + backend) en une image
├── .dockerignore
├── docker-compose.prod.yml   ← lance l'app + la base
├── .env.example              ← modèle de secrets (à copier en .env)
├── .gitignore
├── banking-server/
└── banking-frontend/
```

## 1. Tester l'image en local

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire"

# Crée ton fichier de secrets
Copy-Item .env.example .env
# → ouvre .env, mets un vrai mot de passe DB, un JWT_SECRET aléatoire,
#   un ADMIN_PASSWORD fort, et COOKIE_SECURE=false pour un test http://localhost

# Build + démarrage (la première fois, le build prend quelques minutes)
docker compose -f docker-compose.prod.yml up -d --build

# Suivre les logs de l'app
docker compose -f docker-compose.prod.yml logs -f app
```

L'app est sur http://localhost:8080. Pour tout arrêter : `docker compose -f docker-compose.prod.yml down`
(ajoute `-v` pour effacer aussi la base).

> Important : en local sur `http://localhost` (sans HTTPS), mets `COOKIE_SECURE=false`
> dans le `.env`, sinon le navigateur refuse les cookies d'authentification et la
> connexion échoue. En production derrière HTTPS, laisse `COOKIE_SECURE=true`.

## 2. Ce que fait l'image

- **Étape 1** : compile le frontend React (`npm run build`).
- **Étape 2** : compile le backend et **injecte le frontend** dans les ressources
  statiques de Spring → un seul service sert l'API et l'interface sur le port 8080
  (même origine, donc ni proxy ni CORS).
- **Étape 3** : image d'exécution minimale (JRE seul), lancée en utilisateur non-root.

Tu n'as plus besoin de copier `dist` à la main ni de lancer `mvn` puis `npm`
séparément : tout est reproductible et identique en local et en prod.

## 3. Générer un bon JWT_SECRET

```powershell
docker run --rm eclipse-temurin:21-jre sh -c "head -c 48 /dev/urandom | base64"
```

## 4. Variables d'environnement

| Variable          | Rôle                                              | Prod                          |
|-------------------|---------------------------------------------------|-------------------------------|
| `DB_PASSWORD`     | Mot de passe Postgres                             | obligatoire, fort             |
| `JWT_SECRET`      | Clé de signature des jetons (≥ 32 caractères)     | obligatoire, aléatoire        |
| `ADMIN_PASSWORD`  | Mot de passe du compte admin initial              | obligatoire, fort             |
| `ENFORCE_SECRETS` | Refuse de démarrer si un secret est par défaut    | `true` (déjà fixé)            |
| `COOKIE_SECURE`   | Cookies d'auth en HTTPS uniquement                | `true` (prod) / `false` (test local) |

## 5. Et ensuite ?

Cette image tourne partout : un VPS (avec Caddy pour le HTTPS automatique), ou une
plateforme type Railway / Render (HTTPS + Postgres managé fournis). Le choix de la
plateforme détermine les fichiers de l'étape suivante.
