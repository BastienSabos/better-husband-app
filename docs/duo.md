# Mode duo : données partagées

Complète l'ADR 0004 (Firebase et chiffrement). Ce document dit **ce qui est partagé, ce qui ne l'est jamais**, comment
les tables évoluent et ce que chacun garde à la sortie du duo. Spec : `docs/spec.md`, « v1.1 ».

## Principe

Le mode duo relie deux installations, chacune avec sa propre base. Rien n'est fusionné en bloc : seuls certains
enregistrements portent le drapeau `shared`, et seuls ceux-là partent, chiffrés, vers la boîte aux lettres du duo.
Comme pour les comptes partagés du budget planner (ADR 0028 du dépôt frère), **la portée partagée se calcule dans le
domaine par une seule fonction pure**, `sharedScope`, qui sert à l'envoi, à la vérification de ce qui arrive et au
compte des changements en attente.

## Ce qui est partagé, ce qui reste privé

| Donnée | Duo | Détail |
|---|---|---|
| Envies (`wish`, nouvelle table) | Partagée, toujours | Chacun note ses propres envies ; l'autre les voit en lecture |
| Tâches du foyer | Partagée par défaut | « Seulement pour moi » possible tâche par tâche |
| Dates | Au choix, date par date | Interrupteur « Partager avec… » dans l'éditeur d'une date, en duo seulement ; décoché par défaut. Ne plus partager garde la date ici et la retire chez l'autre (marqueur de suppression de l'ancien `uid`, nouvel `uid` local) |
| Fiche partenaire (goûts, tailles, proches) | **Jamais** | Chacun a sa fiche sur l'autre |
| Carnet d'idées (cadeaux, sorties, surprises) | **Jamais** | Y compris « je m'en occupe » sur une envie de l'autre |
| Journal des attentions, séries | **Jamais** | |
| Profil « Notre couple » | **Jamais** en v1.1 | Chacun le remplit à sa façon |
| Réglages, verrou, rappels | **Jamais** | Les rappels d'une date partagée se programment de chaque côté |

Règle de sécurité : un bloc reçu qui n'entre pas dans la portée (un type inconnu, une idée, une fiche) est refusé et
rien n'est écrit, comme un fichier de proche hors portée dans le budget planner.

### Envies et surprise

Le cas qui fait l'intérêt du duo : Camille note « Le dernier album de… » dans ses envies ; Alex le voit dans l'onglet
Idées, section « Ses envies », et touche « Je m'en occupe ». Cela crée une **idée privée** dans le carnet d'Alex,
liée à l'envie (`idea.wish_uid`). Rien ne repart vers Camille : la surprise reste une surprise. Si Camille retire
son envie, l'idée d'Alex reste, sans lien.

### Qui fait quoi

Une tâche partagée peut porter un responsable (`assignee` = membre du duo, ou personne) et retient qui l'a faite en
dernier (`last_done_by`). Pas de compteur ni de classement (principe « valoriser sans infantiliser ») : l'écran du
foyer affiche seulement « Toi », le prénom de l'autre (pris dans la fiche partenaire locale) ou « L'un ou l'autre ».

## Évolution des tables (migration Room v8 → v9)

Colonnes ajoutées aux tables partageables (`household_task`, `important_date`) et présentes dès la création dans
`wish` :

| Colonne | Type | Rôle |
|---|---|---|
| `uid` | `TEXT NOT NULL UNIQUE` | UUID stable, généré par la migration pour l'existant ; l'`id` local reste la clé Room |
| `shared` | `INTEGER NOT NULL` | 0 en solo ; changé au passage en duo |
| `revision` | `INTEGER NOT NULL` | instant de dernière modification (ms), jamais en recul (`max(maintenant, révision + 1)`) |
| `author` | `TEXT` | identifiant de membre qui a écrit la révision ; `null` en solo |
| `deleted` | `INTEGER NOT NULL` | marqueur de suppression d'un élément partagé (pas d'effacement physique avant envoi) |
| `local_changed_at` | `INTEGER NOT NULL` | instant de l'écriture locale, sert à savoir ce qui reste à envoyer |

Colonnes propres à une table : `household_task.assignee` et `household_task.last_done_by`, ajoutées avec l'écran du foyer (PR 8) ; `idea.wish_uid`, ajoutée avec « Je m'en occupe » (migration v10). La migration v9 ne porte que ce qui est commun (PR 4).

Nouvelles tables :

- `wish` : `id`, `uid`, `owner` (membre), `text`, `link` (facultatif), `created_at`, plus les colonnes ci-dessus.
- `duo_member` : `member_id` (UUID), `is_me`. Deux lignes au plus. Le prénom affiché vient de la fiche partenaire,
  il n'est pas synchronisé.
- `duo_state` (une ligne) : `duo_id`, `last_seen` (horodatage serveur du dernier bloc reçu), `pushed_until` (dernier
  `local_changed_at` envoyé), `ended` (le document du duo a disparu). La clé de couple n'est **pas** dans Room :
  elle est chiffrée par le Keystore dans les préférences privées, comme la clé SQLCipher.

Une suppression d'un élément non partagé reste un effacement immédiat, comme en v1. Les marqueurs d'un élément
partagé s'effacent 90 jours après avoir été envoyés (règle du budget planner, ADR 0018 du dépôt frère, simplifiée :
il n'y a qu'un pair et le serveur garde la dernière version).

## Synchro (`DuoSyncEngine`)

Une synchro lit les blocs écrits depuis la dernière lecture (avec 5 minutes de marge, la fusion étant idempotente), les déchiffre, vérifie que chaque bloc porte l'identifiant que sa clé en déduit, applique les bornes ci-dessous à **tout** le lot, fusionne, puis envoie ce qui a changé ici. Un bloc illisible (autre clé, octet modifié) est ignoré et compté ; un bloc hors bornes fait refuser tout le lot sans rien écrire. Tant qu'un bloc reste refusé, la synchro bute dessus : l'app devra afficher « synchro en attente » plutôt que d'ignorer en silence. Ce qui arrive de l'autre téléphone n'est pas renvoyé. Une écriture faite dans la même milliseconde que le début d'une synchro peut repartir une fois (sans effet, par idempotence).

## Format d'un bloc

Avant chiffrement, un bloc est un JSON versionné, sans champ superflu :

```json
{ "v": 1, "type": "task", "uid": "…", "revision": 1793000000000, "author": "…", "deleted": false,
  "data": { "title": "Sortir les poubelles", "repeat": "WEEKLY", "createdOn": "2026-11-08",
            "lastDoneOn": null, "lastDoneBy": null, "assignee": null } }
```

Types de la v1.1 : `wish`, `task`, `date`. Un type inconnu est refusé ; un champ inconnu d'une version future est
ignoré. Bornes à la lecture (bloc hostile possible) : textes ≤ 500 caractères, années 1900 à 2200, révision ≤
maintenant + 24 h, 10 000 blocs au plus par duo. La fusion (`DuoMerge`, domaine) prend l'état local et un bloc et rend
l'état fusionné : la révision la plus grande gagne, l'auteur départage.

## Passage du solo au duo

1. Alex touche « Inviter mon ou ma partenaire » (Réglages, puis une carte discrète sur l'accueil après quelques
   semaines d'usage, jamais avant).
2. Un écran explique ce qui sera partagé et ce qui ne le sera jamais (le tableau ci-dessus, en clair), puis montre le
   QR code et le bouton « Envoyer le lien ».
3. Camille installe l'app, ouvre le lien. Son onboarding est le même, avec la fiche partenaire préremplie du prénom
   d'Alex si Alex l'accepte (seul champ de fiche transmis, une fois, à l'invitation).
4. Chacun choisit ce qu'il partage de l'existant : tâches cochées par défaut, dates décochées sauf les anniversaires
   de couple. Rien n'est perdu ni écrasé : ce qui n'est pas partagé reste en solo.
5. Doublons possibles (« Sortir les poubelles » des deux côtés) : signalés par titre identique à l'adhésion, avec
   « Garder les deux » ou « Fusionner ». Pas de détection floue.

## Sortie du duo

Depuis Réglages › Mode duo › Quitter le duo, avec une confirmation qui dit ce qui se passe. Aucune justification
demandée, aucune notification envoyée à l'autre au-delà de « Le duo est terminé » à sa prochaine ouverture.

| Donnée | Chez celui qui part | Chez l'autre |
|---|---|---|
| Toutes les envies (les siennes et celles de l'autre) | Supprimées | Supprimées |
| Tâches partagées | Copiées en solo | Copiées en solo |
| Dates partagées | Copiées en solo | Copiées en solo |
| Idées liées à une envie de l'autre | Gardées, lien coupé | Gardées, lien coupé |
| Carnet, fiche, attentions | Inchangés (jamais partagés) | Inchangés |

« Copiées en solo » = `shared` repasse à 0, `author`, `assignee` et `last_done_by` sont effacés, les marqueurs de
suppression disparaissent. Les envies ne servent qu'en duo (aucun écran ne les montre en solo) et portent l'identifiant
de membre de ce duo : les garder ferait des données invisibles, puis des envies attribuées à l'autre dans un duo
suivant. Elles partent donc des deux côtés (décision du 2026-11-13, PR 9).

Sur le téléphone, la sortie est une transaction Room (`DuoExitDao.leave`), puis la clé de couple est effacée du
Keystore ; un échec de la transaction laisse le duo entier. L'effacement côté serveur (`leaveDuo`) et le « Le duo est
terminé » chez l'autre viendront avec le vrai client (PR 6b). Chacun peut ensuite tout effacer depuis « Mes données », comme en v1.

## Questions ouvertes

- Date de début de relation : elle vit dans chaque fiche partenaire et peut différer d'un côté à l'autre. Proposer
  d'aligner à l'adhésion, ou en faire une date partagée ?
- Profil « Notre couple » partagé (enfants, logement, rythme) : utile pour que les attentions suggérées soient
  cohérentes des deux côtés, mais demande de fusionner deux profils. Reporté après la bêta du duo.
- Délai de synchro sans notification push : à mesurer en bêta.

## Découpage en PR

1. Docs : ADR 0004 et ce document (cette PR).
2. Domaine : `sharedScope`, `DuoMerge`, format de bloc et bornes, avec tests (Kotlin pur, vérifiable dans le cloud).
3. Domaine : chiffrement (Tink), dérivation de clés, invitation et code de vérification, avec vecteurs de test.
4. Room v9 : colonnes de synchro, tables `wish`, `duo_member`, `duo_state`, `MigrationTest`.
5. Firebase `firebase/` : règles Firestore et leurs tests (émulateur), Cloud Functions (notification de synchro,
   sortie du duo), job CI, déploiement manuel.
6a. Domaine : moteur de synchro (`DuoSyncEngine`) derrière deux ports, `DuoMailbox` (le serveur) et `DuoStore` (la base), avec leurs faux en test : lecture, déchiffrement, bornes, fusion, envoi.
6b. App : SDK Firebase (Auth, Firestore, Messaging, App Check), `DuoSyncWorker`, politique de confidentialité et
   formulaire « Sécurité des données » mis à jour.
7a. Domaine : lien d'invitation, `DuoInviteHost` et `DuoInviteGuest` derrière le port `DuoInviteServer`, avec un faux serveur en mémoire (tant que le projet Firebase n'existe pas).
7b. Écrans : inviter et rejoindre (lien à partager ou à coller, code de vérification en 4 mots), derrière le faux serveur et visibles seulement en debug. Le duo rejoint est gardé sur le téléphone (membre, état, clé de couple chiffrée par le Keystore ; la clé se teste sur appareil seulement) et l'effacement RGPD local le vide ; l'effacement côté serveur viendra avec la PR 9. Reste pour une PR suivante : QR code (dépendance à valider), lien d'application (domaine à choisir), choix de l'existant à partager.
8. Écrans : envies (« Mes envies », « Ses envies », « Je m'en occupe »), qui fait quoi dans le foyer, dates
   partagées.
9. Sortie du duo, effacement des marqueurs, captures et relecture vie privée finale.
