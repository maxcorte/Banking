# Carnet de contacts

## Principe
- Un onglet **Contacts** (dans la barre du bas) pour gérer les personnes à qui
  tu demandes souvent de l'argent.
- Tu ajoutes un contact par son **nom d'utilisateur** ; il apparaît dans une
  liste (avec une pastille initiale), et tu peux le **retirer**.
- Dans **Demandes → Nouvelle demande**, le champ « Destinataire » devient une
  **liste déroulante** de tes contacts (fini la saisie du nom à la main).
  S'il n'y a pas encore de contact, on retombe sur la saisie libre + une astuce.

## Côté technique
- Migration **V13** : table `contacts` (owner, contact, unique + check
  anti-auto-référence). Créée automatiquement au redémarrage.
- Endpoints : `GET /api/contacts`, `POST /api/contacts {username}`,
  `DELETE /api/contacts/{userId}`.
- Aucun impact sur le ledger ni les migrations existantes.

À appliquer après les paquets `opti-ui` et `opti-fix`.
