# Mise en ligne sur le VPS (Hostinger KVM 2, Ubuntu 24.04)

Ces fichiers **remplacent/complètent** ceux déjà à la racine de ton projet
(`Dockerfile` et `.dockerignore` restent valables et ne changent pas).

Remplace l'ancien `docker-compose.prod.yml` par celui-ci (il ajoute Caddy), et
ajoute `Caddyfile`, `backup-db.sh`, et le nouveau `.env.example`.

---

## 0. Avant tout : faire pointer le domaine vers le serveur

Dans le panneau Hostinger (hPanel → Domaines → Zone DNS de ton domaine), crée un
enregistrement **A** :

```
Type: A   Nom: @     Valeur: <IP_DE_TON_VPS>
Type: A   Nom: www   Valeur: <IP_DE_TON_VPS>   (optionnel)
```

L'IP du VPS est affichée dans hPanel (section VPS). La propagation DNS prend de
quelques minutes à ~1 h. **Fais ça en premier** : Caddy a besoin que le domaine
pointe déjà sur le serveur pour obtenir le certificat HTTPS.

---

## 1. Se connecter au serveur

Depuis PowerShell sur ton PC :

```powershell
ssh root@<IP_DE_TON_VPS>
```

(mot de passe root défini à la commande, ou clé SSH si tu en as configuré une)

---

## 2. Installer Docker (si l'image VPS ne l'a pas déjà)

```bash
docker --version 2>/dev/null || curl -fsSL https://get.docker.com | sh
docker compose version
# Mises à jour de sécurité automatiques :
apt-get update && apt-get install -y unattended-upgrades
```

> Astuce : à la prochaine commande VPS, Hostinger propose souvent un modèle
> « Ubuntu 24.04 with Docker » qui évite cette étape.

---

## 3. Envoyer le projet sur le serveur

### Option rapide — copie directe depuis ton PC (sans GitHub)

D'abord, allège le dossier localement (Docker reconstruit tout, inutile de copier
ces dossiers lourds) — dans **PowerShell** sur ton PC :

```powershell
$proj = "C:\Users\Maxime\Desktop\apllication bancaire"
Remove-Item -Recurse -Force "$proj\banking-frontend\node_modules" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$proj\banking-server\target" -ErrorAction SilentlyContinue
scp -r "$proj" root@<IP_DE_TON_VPS>:/root/banking
```

> On copie vers `/root/banking` (sans espace dans le nom, plus pratique sous Linux).

### Option recommandée à terme — via GitHub

On la mettra en place à l'étape 4 (CI/CD) : tu pousseras ton code sur GitHub, et
le serveur fera `git clone`/`git pull`. Plus propre pour les mises à jour.

---

## 4. Créer le fichier de secrets sur le serveur

Sur le serveur (via SSH) :

```bash
cd /root/banking
cp .env.example .env
# Génère un secret JWT tout neuf pour la prod :
head -c 48 /dev/urandom | base64
nano .env
```

Dans `.env`, remplis : `DOMAIN` (ton domaine), `DB_PASSWORD`, le `JWT_SECRET`
généré ci-dessus, et `ADMIN_PASSWORD`. Enregistre (Ctrl+O, Entrée) et quitte
(Ctrl+X).

> Si un `.env` de test a été copié depuis ton PC, écrase-le bien avec ces valeurs
> de prod (et surtout un vrai domaine).

---

## 5. Lancer l'application

```bash
cd /root/banking
docker compose -f docker-compose.prod.yml up -d --build
```

Le premier build prend quelques minutes (compilation frontend + backend).
Suis les logs :

```bash
docker compose -f docker-compose.prod.yml logs -f app
docker compose -f docker-compose.prod.yml logs -f caddy   # pour voir l'obtention du certificat
```

Quand c'est prêt, ouvre **https://ton-domaine** dans ton navigateur. Caddy a
posé le certificat HTTPS tout seul. 🎉

---

## 6. Sauvegarde automatique de la base (gratuite)

Teste d'abord le script à la main :

```bash
cd /root/banking
./backup-db.sh
ls -lh backups/
```

Puis programme-le chaque nuit à 3 h via cron :

```bash
crontab -e
# Ajoute cette ligne :
0 3 * * * /root/banking/backup-db.sh >> /root/banking/backups/backup.log 2>&1
```

**Restaurer** une sauvegarde si besoin :

```bash
gunzip -c backups/banking-AAAAMMJJ-HHMMSS.sql.gz | docker exec -i banking-db psql -U banking -d banking
```

---

## 7. Pare-feu

Vérifie que les ports 80 et 443 sont ouverts. Si `ufw` est actif :

```bash
ufw status
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp
```

(Sur Hostinger, le pare-feu se gère aussi depuis hPanel ; laisse passer 22, 80, 443.)

---

## Commandes utiles au quotidien

```bash
cd /root/banking
docker compose -f docker-compose.prod.yml ps          # état des conteneurs
docker compose -f docker-compose.prod.yml logs -f app  # logs de l'app
docker compose -f docker-compose.prod.yml down         # tout arrêter
docker compose -f docker-compose.prod.yml up -d --build # rebuild après modif
```

## Mettre à jour l'app après un changement de code

Recopie le code (étape 3), puis :

```bash
cd /root/banking
docker compose -f docker-compose.prod.yml up -d --build
```

Les volumes (`banking_pgdata`, `caddy_data`) sont conservés : ni la base ni le
certificat ne sont perdus au redéploiement.
