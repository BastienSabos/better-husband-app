# 0004. Mode duo : boîte aux lettres chiffrée de bout en bout

- Statut : proposé (hébergement à choisir par Bastien)
- Date : 2026-11-08

## Contexte

La v1.1 ajoute le mode duo (`docs/spec.md`, « v1.1 ») : le ou la partenaire installe l'app, rejoint le duo par un
lien ou un QR code, et les deux téléphones partagent la liste d'envies, les tâches du foyer et des dates communes.
Le carnet privé (fiche partenaire, idées, surprises, attentions) ne quitte jamais le téléphone.

Contraintes :

- La v1 n'a ni serveur ni permission `INTERNET` (ADR 0002). Le réseau est autorisé depuis le 2026-11-07, mais
  aucune donnée personnelle ne sort sans consentement explicite.
- Les données partagées restent intimes (envies, organisation du foyer, enfants) : le serveur ne doit rien pouvoir
  lire, ni nous, ni l'hébergeur, ni quelqu'un qui volerait la base.
- Deux personnes, deux téléphones, des modifications chaque jour : un échange manuel de fichier (budget planner,
  ADR 0003 et 0028 du dépôt frère) serait trop lourd pour cocher une tâche.
- Développeur seul, pas de monétisation : le serveur doit coûter presque rien et demander peu d'exploitation.
- Séparation possible : sortir du duo doit être simple, immédiat et sans dépendre de l'autre.

## Décision

### 1. Un serveur « boîte aux lettres » qui ne voit que des blocs chiffrés

Le serveur ne connaît ni prénom, ni e-mail, ni contenu. Il stocke, par duo, des blocs opaques :

| Colonne | Contenu |
|---|---|
| `duo_id` | identifiant aléatoire (128 bits) |
| `blob_id` | `HMAC-SHA256(K_id, type ‖ uid)` tronqué à 16 octets : le serveur ne voit pas l'identifiant réel |
| `seq` | compteur du duo, attribué par le serveur à chaque écriture |
| `ciphertext` | l'enregistrement chiffré, complété à un multiple de 256 octets pour masquer sa longueur |

L'API tient en quatre appels : créer une invitation, la rejoindre, `PUT` d'un lot de blocs, `GET` des blocs depuis
un `seq`. Plus `DELETE /duo` pour la sortie. Chaque bloc remplace le précédent de même `blob_id` : le serveur ne
garde que la dernière version, pas d'historique.

**Hébergement recommandé** : Cloudflare Workers + D1 (base en Europe de l'Ouest, indication `weur`). Gratuit au
début (100 000 requêtes et 100 000 lignes écrites par jour), puis l'offre payante à 5 $/mois. Le code du serveur vit
dans ce dépôt (`server/`, TypeScript), avec ses tests et son job CI. Le même socle servira au proxy du coach IA
(option C de l'étude du 2026-11-07). **Choix final de l'hébergeur : Bastien** (alternatives ci-dessous).

### 2. Chiffrement de bout en bout avec une clé de couple

- **Clé de couple** `K` (256 bits), connue des deux téléphones seulement. On en dérive par HKDF-SHA256 :
  `K_data` (chiffrement), `K_id` (identifiants de blocs). Elle est stockée chiffrée par une clé Android Keystore,
  comme la clé SQLCipher (ADR 0002).
- **Chiffrement** : AES-256-GCM, nonce aléatoire de 12 octets, données associées = `duo_id ‖ blob_id ‖ version`
  (un bloc ne peut pas être déplacé d'un duo ou d'un enregistrement à un autre sans échouer).
- **Bibliothèque** : Tink (Google, Apache 2.0) pour X25519, HKDF et AES-GCM. X25519 n'existe dans le JDK Android
  qu'à partir de l'API 33, or `minSdk` vaut 26. Le reste (AES-GCM) suit la même forme que le format d'échange du
  budget planner (en-tête versionné et authentifié).

### 3. Invitation par lien ou QR code, sans compte

1. A crée l'invitation : paire de clés X25519 éphémère, `secret` aléatoire de 128 bits. Le serveur reçoit
   l'identifiant d'invitation et la clé publique de A, jamais le secret.
2. Lien : `https://<domaine>/i/<invitation>#<secret>`. Le secret est après le `#` : un navigateur ne l'envoie jamais
   au serveur. Le QR code porte le même lien. L'invitation expire après 24 h et ne sert qu'une fois.
3. B ouvre le lien dans l'app, crée sa paire X25519, envoie sa clé publique et un `HMAC(secret, transcription)`.
4. A vérifie le HMAC : seul quelqu'un qui a reçu le lien peut rejoindre. Les deux téléphones calculent
   `K = HKDF(X25519(a, B) ‖ secret, "duo-v1")`.
5. Les deux écrans affichent un **code de vérification** de 4 mots tiré de `K`. Le comparer est facultatif (à voix
   haute ou côte à côte) ; il protège du cas où la messagerie qui a transporté le lien et le serveur seraient
   complices. Le QR scanné en personne n'en a pas besoin.

Pas d'e-mail, pas de mot de passe : l'appareil s'authentifie par un **jeton d'appareil** aléatoire reçu à
l'invitation (le serveur n'en garde que l'empreinte SHA-256), envoyé en en-tête sur HTTPS.

### 4. Synchro : la dernière modification gagne, enregistrement par enregistrement

On reprend la règle du budget planner (ADR 0003 du dépôt frère) : chaque enregistrement partagé porte un
identifiant stable (UUID), une révision (horloge du téléphone, jamais en recul), l'auteur, et un marqueur de
suppression. La fusion est commutative et idempotente : rejouer un bloc ou recevoir deux blocs dans le désordre
donne le même état. Égalité de révision : l'auteur départage. Révision refusée au-delà de « maintenant + 24 h ».

Quand synchroniser : à l'ouverture de l'app, après chaque modification d'un élément partagé (WorkManager, contrainte
« réseau disponible », regroupée sur quelques secondes), et toutes les 6 heures en arrière-plan. Pas de notification
push en v1.1 (Firebase Cloud Messaging ajouterait un SDK Google) ; à reconsidérer avec un message vide si le délai
gêne en bêta.

Le détail des données partagées et de leur cycle de vie est dans `docs/duo.md`.

### 5. Sortie du duo

L'un ou l'autre quitte le duo seul, sans accord de l'autre : `DELETE /duo` efface sur le serveur tous les blocs, les
jetons et le duo, puis l'app détruit `K`. L'autre téléphone reçoit `410 Gone` à sa prochaine synchro et affiche
« Le duo est terminé ». Ce que chacun garde est décrit dans `docs/duo.md`. Un duo sans aucune synchro pendant
12 mois est effacé par le serveur (durée de conservation de la politique de confidentialité).

### 6. Permission `INTERNET`

Elle s'ajoute avec la première PR qui appelle le serveur. Rien ne part tant que l'utilisateur n'a pas créé ou
rejoint un duo : en solo, l'app ne fait aucun appel réseau (vérifié par un test sur le client HTTP injecté). La
politique de confidentialité, sa copie dans l'app et le formulaire « Sécurité des données » du Play Store changent
dans la même PR.

## Options écartées

- **Supabase** (prévu dans la spec) : Postgres, authentification et temps réel clés en main, mais avec le
  chiffrement de bout en bout on n'utilise ni les requêtes, ni le temps réel sur le contenu, ni les comptes. L'offre
  gratuite se met en pause après 7 jours sans activité : Pro à 25 $/mois dès la sortie. Reste la meilleure option si
  on abandonne le chiffrement de bout en bout, ce qui n'est pas envisagé.
- **Firebase Firestore** : synchro hors ligne native, mais SDK Google lourd dans l'app (identifiant d'installation,
  dépendance aux services Google Play), et les règles de sécurité deviennent inutiles sur des blocs chiffrés.
- **Scaleway ou un VPS en France** : souveraineté maximale, mais 10 à 20 €/mois et un serveur à maintenir (mises à
  jour, sauvegardes, certificats). Possible plus tard : l'API est assez petite pour être portée.
- **Échange par fichier ou QR sans serveur** (budget planner) : zéro coût et zéro réseau, mais il faut échanger à la
  main pour voir une tâche cochée. Acceptable pour un budget mensuel, pas pour le foyer au quotidien.
- **Synchro pair à pair (Wi-Fi local, Bluetooth)** : ne marche que quand les deux téléphones sont proches.
- **Clé dérivée d'une phrase de passe** (comme le fichier d'échange du budget planner) : demande à deux personnes de
  retenir et saisir la même phrase ; l'échange X25519 + secret du lien donne une clé plus forte sans rien à retenir.
- **Protocole de groupe (MLS) ou bibliothèque Signal** : faits pour des groupes et des messages, pas pour deux
  appareils qui partagent des listes ; trop de surface pour le besoin.
- **Comptes e-mail** : une donnée personnelle de plus côté serveur, une récupération de mot de passe à gérer, pour
  aucun gain ici.

## Conséquences

- Le serveur sait seulement qu'un duo existe, combien de blocs il contient (taille arrondie), quand ils changent et
  depuis quelles adresses IP. Ce sont les métadonnées à déclarer dans la politique de confidentialité ; le Worker
  ne journalise pas les IP au-delà de ce que Cloudflare garde par défaut (à vérifier dans ses réglages).
- **Changer de téléphone** : la clé de couple est dans le Keystore et ne se sauvegarde pas. Le nouveau téléphone
  rejoint le duo par une nouvelle invitation depuis le téléphone de l'autre, qui renvoie tout. Si les deux
  téléphones sont perdus, les données partagées le sont aussi (l'export JSON de « Mes données » reste le filet).
- **Un troisième appareil** (tablette) n'est pas prévu : le duo a exactement deux membres.
- **Migration Room** : les tables partagées gagnent `uid`, révision, auteur, marqueur de suppression et `shared`
  (détail dans `docs/duo.md`). Test dans `MigrationTest` et schéma JSON à générer.
- Le code de chiffrement et de fusion va dans `:core:domain` ou un futur module Kotlin pur : il se teste dans le
  cloud sans SDK Android, avec la couverture Kover.
- Relecture `privacy-guardian` obligatoire sur chaque PR du mode duo.
- À surveiller : horloges très décalées entre les deux téléphones (la borne de 24 h limite les dégâts), coût réel
  des requêtes en bêta, et la tentation d'ajouter des données au serveur « pour déboguer ».
