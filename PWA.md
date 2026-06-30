# PWA (app installable) + scanner QR caméra

Cette étape transforme ton site en **application installable** (« comme une vraie
app ») et ajoute un **scanner de QR code intégré** (la caméra dans l'app, sans
passer par l'appareil photo système).

Concrètement :
- **Installable** : icône sur l'écran d'accueil, démarrage en plein écran (sans
  barre du navigateur), via un *manifeste* + un *service worker* (généré
  automatiquement par `vite-plugin-pwa`).
- **Scanner intégré** : un bouton **« Scanner »** ouvre la caméra ; vise un QR de
  paiement et le **virement se pré-remplit** (même parcours que le lien `?pay=`).

**Le frontend a été compilé sans erreur** et la build de production génère bien
`manifest.webmanifest`, `sw.js` et les icônes.

---

## 1. Appliquer

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire"
Expand-Archive "$env:USERPROFILE\Downloads\pwa-scanner.zip" -DestinationPath . -Force
```

Le zip ajoute le dossier `banking-frontend/public/` (icônes), met à jour
`vite.config.ts`, `index.html`, `package.json` **et** `package-lock.json`
(nouvelles dépendances `vite-plugin-pwa` et `jsqr`), et ajoute le composant
`QrScanner`.

> La CI et le Dockerfile utilisent `npm ci` et installent les *devDependencies* :
> la génération du service worker se fait donc bien au build. Rien à changer côté
> déploiement.

## 2. Tester en local (optionnel)

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire\banking-frontend"
npm install
npm run build
npm run preview
```

> En `npm run dev`, la PWA n'est pas toujours active (le service worker est généré
> au *build*). Utilise `npm run build` + `npm run preview` pour tester
> l'installation et le hors-ligne. Le **scanner caméra** exige HTTPS **ou**
> localhost — donc il marche en preview local et en production, mais pas sur une
> IP réseau en http.

## 3. Déployer

```powershell
cd "C:\Users\Maxime\Desktop\apllication bancaire"
git add .
git commit -m "PWA installable + scanner QR camera integre"
git push
```

CI compile, CD déploie. Une fois en ligne :

**Installer l'app**
- **Android (Chrome)** : menu ⋮ → « Installer l'application » (ou la bannière
  d'installation qui apparaît). L'icône € apparaît sur l'écran d'accueil.
- **iPhone (Safari)** : bouton Partager → « Sur l'écran d'accueil ».
- **Desktop (Chrome/Edge)** : icône d'installation dans la barre d'adresse.

**Scanner un QR**
Ouvre l'app → bouton **« Scanner »** → autorise la caméra → vise un QR de paiement
(généré par l'onglet « Recevoir » sur un autre appareil) → le virement se
pré-remplit. Choisis le compte à débiter et confirme.

---

## Détails techniques (pour un entretien)

- **Service worker / offline** : `vite-plugin-pwa` (Workbox) précharge la coquille
  de l'app. J'ai exclu `/api` et `/actuator` du *navigateFallback* : ces requêtes
  partent toujours au réseau, jamais servies depuis le cache. Mode `autoUpdate` :
  les nouvelles versions s'appliquent au chargement suivant.
- **Scanner cross-navigateur** : décodage logiciel via **jsQR** sur les images de
  la caméra (`getUserMedia`), donc compatible y compris iOS Safari — contrairement
  à l'API `BarcodeDetector` qui n'est pas partout. La caméra exige un contexte
  sécurisé (HTTPS), ce que fournit déjà ton Caddy.
- **Icônes** : un jeu complet (192, 512, *maskable*, apple-touch, favicon) pour un
  rendu net sur Android, iOS et desktop.

## Bon à savoir

- Après un déploiement, à cause du cache du service worker, une mise à jour peut
  n'apparaître qu'au **deuxième** chargement. Pour forcer : fermer/rouvrir l'app
  installée, ou un rechargement forcé (Ctrl+Maj+R) dans le navigateur.
- Le bundle JS grossit un peu (jsQR + qrcode) : c'est normal et sans impact réel
  sur un usage courant.

## Suite logique

Avec le scanner et l'installation en place, deux prolongements naturels : les
**notifications push** (web push : alerte de paiement reçu, même app fermée — la
brique PWA est déjà là), et la **demande de remboursement entre amis** avec suivi.
