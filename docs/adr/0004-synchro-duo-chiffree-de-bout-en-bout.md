# 0004. Mode duo : Firebase comme boîte aux lettres chiffrée de bout en bout

- Statut : proposé (hébergeur choisi par Bastien le 2026-11-08 : Firebase)
- Date : 2026-11-08

## Contexte

La v1.1 ajoute le mode duo (`docs/spec.md`, « v1.1 ») : le ou la partenaire installe l'app, rejoint le duo par un
lien ou un QR code, et les deux téléphones partagent la liste d'envies, les tâches du foyer et des dates communes.
Le carnet privé (fiche partenaire, idées, surprises, attentions) ne quitte jamais le téléphone.

Contraintes :

- La v1 n'a ni serveur ni permission `INTERNET` (ADR 0002). Le réseau est autorisé depuis le 2026-11-07, mais
  aucune donnée personnelle ne sort sans consentement explicite.
- Les données partagées restent intimes (envies, organisation du foyer, enfants) : le serveur ne doit rien pouvoir
  lire, ni nous, ni Google, ni quelqu'un qui volerait la base.
- Deux personnes, deux téléphones, des modifications chaque jour : un échange manuel de fichier (budget planner,
  ADR 0003 et 0028 du dépôt frère) serait trop lourd pour cocher une tâche.
- Développeur seul, pas de monétisation : le service doit coûter presque rien et demander peu d'exploitation.
- Séparation possible : sortir du duo doit être simple, immédiat et sans dépendre de l'autre.

## Décision

### 1. Firebase, utilisé comme simple boîte aux lettres

Bastien a choisi Firebase le 2026-11-08 (le moins de code serveur, et les notifications de synchro). On n'en prend
que quatre briques : **Authentication** (anonyme), **Firestore**, **Cloud Messaging** et **Cloud Functions**, plus
**App Check** (Play Integrity) pour que seule l'app publiée parle au projet. Ni Analytics, ni Crashlytics, ni Remote
Config, ni Performance : leurs dépendances sont interdites par une règle d'architecture.

- **Région** : `eur3` (multirégion Europe) pour Firestore, `europe-west1` pour les Functions.
- **Offre** : Blaze (paiement à l'usage), obligatoire pour les Cloud Functions, avec une **alerte de budget à 1 €**.
  À l'échelle de la bêta et des premiers milliers de duos, l'usage reste sous les quotas gratuits : coût attendu nul.
- **Comptes** : chaque installation reçoit un identifiant anonyme Firebase. Pas d'e-mail, pas de mot de passe.
- **Cache Firestore hors ligne désactivé** (cache mémoire seulement) : Room reste la seule source de vérité locale.

Structure Firestore, qui ne contient aucun texte lisible :

```
duos/{duoId}                      members: [uidA, uidB], createdAt, lastActivity
duos/{duoId}/blobs/{blobId}       ciphertext (octets), updatedAt (heure du serveur), author (uid)
duos/{duoId}/devices/{uid}        fcmToken
invites/{inviteId}                duoId, createdBy, publicKeyA, expiresAt (24 h)
invites/{inviteId}/join/{uid}     publicKeyB, proof
```

- `blobId` = `HMAC-SHA256(K_id, type ‖ uid)` tronqué à 16 octets : Firebase ne voit pas l'identifiant réel.
- `ciphertext` est complété à un multiple de 256 octets pour masquer la longueur des textes.
- Un bloc remplace le précédent de même `blobId` : pas d'historique côté serveur.

**Règles de sécurité** (versionnées dans `firebase/firestore.rules`, testées avec l'émulateur) : seuls les deux
membres lisent et écrivent leur duo ; `updatedAt` doit valoir `request.time` ; un bloc fait au plus 16 Ko ; la liste
des membres ne change qu'une fois, par le créateur, pour ajouter le membre invité ; une invitation n'est lisible que
par son identifiant (non listable).

**Cloud Functions** (TypeScript, `firebase/functions/`) :

- `onBlobWritten` : envoie à l'autre appareil un message FCM **de données, vide** (« synchronise-toi »). Aucune
  notification visible, aucun contenu ; l'app lance alors `DuoSyncWorker`.
- `leaveDuo` (appelable) : efface le duo, ses blocs, ses appareils et ses invitations.
- Effacement planifié : invitations expirées, et duos sans activité depuis 12 mois (durée de conservation de la
  politique de confidentialité). Politique TTL de Firestore sur `expiresAt` quand c'est possible.

### 2. Chiffrement de bout en bout avec une clé de couple

- **Clé de couple** `K` (256 bits), connue des deux téléphones seulement. On en dérive par HKDF-SHA256 :
  `K_data` (chiffrement), `K_id` (identifiants de blocs). Elle est stockée chiffrée par une clé Android Keystore,
  comme la clé SQLCipher (ADR 0002).
- **Chiffrement** : AES-256-GCM, nonce aléatoire de 12 octets, données associées = `duoId ‖ blobId ‖ version`
  (un bloc ne peut pas être déplacé d'un duo ou d'un enregistrement à un autre sans échouer au déchiffrement).
- **Bibliothèque** : Tink (Google, Apache 2.0) pour X25519, HKDF et AES-GCM. X25519 n'existe dans le JDK Android
  qu'à partir de l'API 33, or `minSdk` vaut 26.

### 3. Invitation par lien ou QR code

1. A crée l'invitation : paire de clés X25519 éphémère et `secret` aléatoire de 128 bits. Firestore reçoit
   l'identifiant d'invitation et la clé publique de A, jamais le secret.
2. Lien : `https://<domaine>/i/<invitation>#<secret>` (App Link). Le secret est après le `#` : un navigateur ne
   l'envoie jamais au serveur. Le QR code porte le même lien. L'invitation expire après 24 h et ne sert qu'une fois.
3. B ouvre le lien dans l'app, crée sa paire X25519 et écrit sa clé publique avec `proof = HMAC(secret,
   transcription)`.
4. A (à l'ouverture de l'app, ou tout de suite s'il montre le QR) vérifie la preuve : seul quelqu'un qui a reçu le
   lien peut rejoindre. A ajoute alors B aux membres. Les deux téléphones calculent
   `K = HKDF(X25519(a, B) ‖ secret, "duo-v1")`.
5. Les deux écrans affichent un **code de vérification** de 4 mots tiré de `K`. Le comparer est facultatif ; il
   protège du cas où la messagerie qui a transporté le lien et Firebase seraient complices. Le QR scanné en personne
   n'en a pas besoin.

### 4. Synchro : la dernière modification gagne, enregistrement par enregistrement

On reprend la règle du budget planner (ADR 0003 du dépôt frère) : chaque enregistrement partagé porte un
identifiant stable (UUID), une révision (horloge du téléphone, jamais en recul), l'auteur, et un marqueur de
suppression. La fusion est commutative et idempotente : rejouer un bloc ou recevoir deux blocs dans le désordre
donne le même état. Égalité de révision : l'auteur départage. Révision refusée au-delà de « maintenant + 24 h ».

Lecture : requête `updatedAt > last_seen − 5 min` (la marge rattrape les écritures validées dans le désordre ; la
fusion idempotente rend les doublons sans effet), plus un écouteur temps réel tant que l'app est ouverte.

Quand synchroniser : à l'ouverture de l'app, après chaque modification d'un élément partagé (WorkManager, contrainte
« réseau disponible »), à la réception d'un message FCM, et toutes les 6 heures en arrière-plan. Sans services
Google Play sur le téléphone, FCM ne marche pas : la synchro périodique prend le relais.

Le détail des données partagées et de leur cycle de vie est dans `docs/duo.md`.

### 5. Sortie du duo

L'un ou l'autre quitte le duo seul, sans accord de l'autre : `leaveDuo` efface tout côté Firebase, puis l'app
détruit `K` et supprime son identifiant anonyme. L'autre téléphone constate à sa prochaine synchro que le duo
n'existe plus et affiche « Le duo est terminé ». Ce que chacun garde est décrit dans `docs/duo.md`.

### 6. Permission `INTERNET` et SDK Firebase

Ils s'ajoutent avec la première PR qui synchronise. **Rien ne part tant que l'utilisateur n'a pas créé ou rejoint un
duo** : en solo, l'app n'initialise pas Firebase (initialisation manuelle, `FirebaseInitProvider` retiré du
manifeste, désinscription de FCM). Un test vérifie qu'en solo aucun composant Firebase n'est démarré. La politique de
confidentialité, sa copie dans l'app et le formulaire « Sécurité des données » (identifiants d'appareil collectés
pour le fonctionnement de l'app, en mode duo seulement) changent dans la même PR.

`google-services.json` n'est pas committé (comme les identifiants Google du budget planner) : il est fourni par
Bastien, posé en local et dans un secret GitHub `GOOGLE_SERVICES_JSON` pour la CI.

## Options écartées

- **Cloudflare Workers + D1** (recommandé dans la première version de cet ADR) : aucun SDK Google, 5 $/mois, et un
  socle réutilisable pour le proxy du coach IA ; mais un serveur à écrire et pas de notification de synchro.
  Écarté par Bastien au profit de Firebase. Le proxy du coach IA pourra être une Cloud Function.
- **Supabase** (prévu dans la spec) : l'offre gratuite se met en pause après 7 jours sans activité, donc Pro à
  25 $/mois dès la sortie, pour des fonctions (requêtes, temps réel sur le contenu) inutiles sur des blocs chiffrés.
- **Scaleway ou un VPS en France** : souveraineté maximale, mais 10 à 20 €/mois et un serveur à maintenir.
- **Échange par fichier ou QR sans serveur** (budget planner) : zéro coût et zéro réseau, mais il faut échanger à la
  main pour voir une tâche cochée. Acceptable pour un budget mensuel, pas pour le foyer au quotidien.
- **Firestore en clair, protégé par les seules règles** : plus simple (requêtes, temps réel natifs), mais Google et
  quiconque obtient l'accès au projet liraient envies et tâches. Contraire à la spec (point 6 de la v1.1).
- **Synchro pair à pair (Wi-Fi local, Bluetooth)** : ne marche que quand les deux téléphones sont proches.
- **Clé dérivée d'une phrase de passe** (comme le fichier d'échange du budget planner) : demande à deux personnes de
  retenir et saisir la même phrase ; X25519 + secret du lien donne une clé plus forte sans rien à retenir.
- **Protocole de groupe (MLS) ou bibliothèque Signal** : faits pour des groupes et des messages, trop de surface pour
  deux appareils qui partagent des listes.
- **Comptes e-mail ou Google** : une donnée personnelle de plus chez Firebase, pour aucun gain ici.

## Conséquences

- Google sait qu'un duo existe, combien de blocs il contient (taille arrondie), quand ils changent, et voit les
  identifiants anonymes, les jetons FCM et les adresses IP. Ce sont les métadonnées à déclarer dans la politique de
  confidentialité. Google agit comme sous-traitant (conditions de traitement des données de Firebase à accepter dans
  la console).
- **À faire par Bastien** : créer le projet Firebase (région `eur3`), passer en Blaze avec l'alerte de budget,
  activer l'authentification anonyme et App Check (Play Integrity), accepter les conditions de traitement des
  données, puis fournir `google-services.json` et le secret CI.
- **Changer de téléphone** : la clé de couple est dans le Keystore et ne se sauvegarde pas. Le nouveau téléphone
  rejoint le duo par une nouvelle invitation depuis le téléphone de l'autre, qui renvoie tout. Si les deux
  téléphones sont perdus, les données partagées le sont aussi (l'export JSON de « Mes données » reste le filet).
- **Un troisième appareil** (tablette) n'est pas prévu : le duo a exactement deux membres.
- **Taille de l'APK** : Auth, Firestore, Messaging et App Check ajoutent quelques Mo (gRPC compris) ; à mesurer
  dans la PR qui les ajoute.
- **Migration Room** : les tables partagées gagnent `uid`, révision, auteur, marqueur de suppression et `shared`
  (détail dans `docs/duo.md`). Test dans `MigrationTest` et schéma JSON à générer.
- Le code de chiffrement et de fusion va dans `:core:domain` : il se teste dans le cloud sans SDK Android, avec la
  couverture Kover. Firebase reste derrière une interface `DuoMailbox` dans `:app`, ce qui permet un faux en test et
  un changement d'hébergeur sans toucher au domaine.
- Relecture `privacy-guardian` obligatoire sur chaque PR du mode duo.
- À surveiller : horloges très décalées entre les deux téléphones (la borne de 24 h limite les dégâts), consommation
  réelle des quotas en bêta, et la tentation d'ajouter Analytics ou Crashlytics « puisque Firebase est là ».
