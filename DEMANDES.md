# Demandes de remboursement entre amis

Première fonctionnalité **avec état** : une demande a un statut suivi
(En attente → Accepté / Refusé / Annulé).

## Principe
- Tu demandes de l'argent à un autre utilisateur (par son nom d'utilisateur),
  vers l'un de tes comptes.
- Le destinataire voit la demande dans l'onglet **Reçues** et peut :
  - **Accepter et payer** → choisit le compte à débiter, le virement part
    (contrôles de solde/devise du moteur de virement habituel) ;
  - **Refuser**.
- Toi (l'auteur) suis le statut dans **Envoyées**, et peux **Annuler** tant
  que c'est en attente.

## Notifications (réutilise le système existant)
- Le destinataire reçoit « Demande de paiement » quand une demande arrive.
- À l'acceptation, le virement déclenche ta notif « Paiement reçu ».
- Au refus, tu reçois « Demande refusée ».
(in-app + push, en asynchrone après commit — aucun risque pour les virements.)

## Côté technique
- Migration **V12** : table `payment_requests` (créée automatiquement au
  redémarrage de l'app — rien à faire manuellement).
- Les tests du ledger ne sont pas impactés (le virement passe par le même
  `TransferService`, les notifs sont asynchrones après commit).

## Important
Applique d'abord `android-fixes.zip` s'il ne l'est pas déjà : ce paquet
contient les versions à jour de `styles.css`, `Dashboard.tsx`, `api.ts`,
`types.ts` (qui incluent aussi les correctifs scanner/push), mais **pas**
`QrScanner.tsx` / `push.ts` / `NotificationsBell.tsx` qui ne sont QUE dans
`android-fixes.zip`.
