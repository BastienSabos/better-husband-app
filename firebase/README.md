# Firebase du mode duo

Boîte aux lettres chiffrée de bout en bout du mode duo : voir `docs/adr/0004-synchro-duo-chiffree-de-bout-en-bout.md`
(décisions) et `docs/duo.md` (données). Firestore ne stocke que des blocs illisibles ; rien ici ne connaît le contenu.

| Fichier | Rôle |
|---|---|
| `firestore.rules` | Qui lit et écrit quoi (duos, blocs, appareils, invitations) |
| `tests/rules.test.ts` | Tests des règles sur l'émulateur Firestore |
| `functions/` | Cloud Functions : `onBlobWritten` (message FCM de synchro), `leaveDuo` (quitter le duo), `purgeExpired` (nettoyage nocturne) |

## Tester

```bash
npm ci && npm test                       # règles, sur l'émulateur (JDK 21 requis)
npm --prefix functions ci && npm run test:functions   # logique des Functions (sans émulateur)
```

## Mise en place du projet (une fois, par Bastien)

1. Console Firebase : nouveau projet **sans Google Analytics**.
2. Firestore en mode production, région **`eur3`** (multirégion Europe).
3. Offre **Blaze** (les Functions l'exigent) et une **alerte de budget à 1 €**.
4. Authentication : activer le fournisseur **anonyme**.
5. App Check : enregistrer l'app Android avec **Play Integrity**, puis passer en **application** (enforcement) pour
   Firestore et pour les Functions (`leaveDuo` l'exige déjà).
6. Accepter les conditions de traitement des données de Firebase.
7. Ajouter l'app Android (`com.bastiensabos.betterhusband`) et télécharger `google-services.json` : il ne se commite
   pas (déjà dans `.gitignore`) ; le fournir en local et en secret GitHub `GOOGLE_SERVICES_JSON`.
8. Écrire `firebase/.firebaserc` avec l'identifiant du projet (`{"projects":{"default":"<id>"}}`), puis déployer :
   `npx firebase deploy --only firestore:rules,functions`.

Ne jamais ajouter Analytics, Crashlytics, Remote Config ni Performance : le manifeste et les dépendances les excluent.
