# AGENTS.md

Instructions pour tout agent de code (Claude Code, Codex, Copilot, Cursor…) qui travaille sur ce dépôt.
Claude Code les charge via `CLAUDE.md`. Garder ce fichier court : le détail vit dans `docs/`.

## Le produit

**Appli de couple** Android grand public : elle aide une personne en couple à être un ou une partenaire (et un
parent) plus présent, en moins de 5 minutes par jour. Ce n'est pas une appli « husband » : elle s'adresse à
tous les couples, mariés ou non, quel que soit le genre. « Better husband » n'est qu'un nom de travail. Rôle double : **assistant** (retenir dates, goûts, envies, tâches) et
**coach** (suggérer des gestes concrets). Francophone d'abord, publiée sur le Play Store.

- Spec produit (source de vérité fonctionnelle) : https://claude.ai/artifact/NW89XjhsbHK3ChuPxDXFZu
- MVP : mono-utilisateur, **local d'abord, sans serveur, sans IA, sans monétisation**. Cinq briques :
  fiche partenaire, dates et rappels, carnet d'idées, flux du jour, tâches du foyer.
- Hors MVP : coach IA (v2, API Claude via proxy), mode duo, synchro cloud, monétisation.

Si une demande sort du MVP, le signaler avant de coder.

## Principes produit non négociables

1. **Actionnable** : chaque écran propose une action faisable aujourd'hui.
2. **5 minutes par jour** : pas de formulaire long, pas de tableau de bord à remplir.
3. **Sincérité** : jamais de message envoyé à la place de l'utilisateur sans validation explicite.
4. **Discrétion par défaut** : notifications neutres sur l'écran verrouillé, verrouillage biométrique possible.
5. **Valoriser sans infantiliser** : séries et bilans, pas de points ni de classement.
6. **Coach, pas thérapeute** : face à un mal-être ou des violences, orienter vers des professionnels.

Ton de l'UI : direct, concret, bienveillant, jamais moralisateur. Tutoiement. Langage **neutre et inclusif** :
pas de « mari », « femme » ou « elle » codés en dur ; on utilise le prénom saisi ou « ton/ta partenaire »
selon le genre choisi à l'onboarding. Voir `docs/ux-writing.md`.

## Vie privée (règles dures)

Les données sont intimes et concernent un tiers (le ou la partenaire), parfois de santé.

- Stockage local chiffré uniquement (Room + SQLCipher, clé dans Android Keystore). Aucun appel réseau au MVP.
- `allowBackup=false`, aucune sauvegarde cloud ni transfert d'appareil.
- Aucun SDK d'analytics, de pub, de crash reporting tiers ou de tracking sans ADR validé.
- Jamais de donnée personnelle dans les logs, les noms de notification visibles, les crash reports ou les tests
  committés (utiliser des données fictives).
- RGPD : consentement explicite, export et suppression complète des données depuis l'app.

## Stack

Kotlin · Jetpack Compose + Material 3 · AGP 9 (Kotlin intégré) · Hilt · Room + SQLCipher ·
WorkManager pour les rappels · Coroutines/Flow · JUnit4 + Truth + Turbine · Spotless/ktlint (+ règles Compose).
Versions dans `gradle/libs.versions.toml` uniquement. Décisions dans `docs/adr/`.

## Architecture

Voir `docs/architecture.md`. En bref :

- Couches : `ui` (Compose, ViewModel) → `domain` (cas d'usage, optionnel) → `data` (repositories, Room, WorkManager).
- UDF : le ViewModel expose un `StateFlow<XxxUiState>` immuable ; l'UI envoie des événements via des callbacks.
- Chaque écran = `XxxRoute` (branche le ViewModel) + `XxxScreen` stateless (prévisualisable, testable).
- Les repositories exposent des `Flow` et sont la seule source de vérité. Pas d'accès DAO depuis un ViewModel.
- Un seul module `:app` pour l'instant, organisé par feature (`feature/today`, `feature/partner`…). On
  modularise quand un module se justifie (voir le skill `add-module`).

## Commandes

```bash
./gradlew spotlessApply          # formate tout (à lancer avant chaque commit)
./gradlew spotlessCheck          # vérifie le format (CI)
./gradlew testDebugUnitTest      # tests unitaires
./gradlew lintDebug              # Android Lint (warnings = erreurs)
./gradlew assembleDebug          # APK debug
./gradlew check                  # tout ce qui précède, la porte de qualité locale
```

Un test unique : `./gradlew testDebugUnitTest --tests "*.TodayViewModelTest"`.

## Conventions

- Code, identifiants, commits et PR en **anglais** ; docs et textes UI en **français**.
- Textes UI uniquement dans `res/values/strings.xml` (aucune chaîne en dur dans le code Compose).
- Commits : [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `refactor:`, `test:`,
  `docs:`, `build:`, `ci:`, `chore:`), un sujet ≤ 72 caractères.
- Branches : `feat/<sujet>`, `fix/<sujet>`, `chore/<sujet>`. Jamais de commit direct sur `main`.
- Une PR = un sujet, idéalement < 400 lignes de diff hors fichiers générés.
- Tests : tout ViewModel, repository et cas d'usage a des tests unitaires. Les bugs sont corrigés test d'abord.
- Pas de `!!`, pas de `GlobalScope`, pas de `runBlocking` hors tests. Dispatchers injectés.

## Definition of Done

- [ ] `./gradlew check` passe en local
- [ ] Tests ajoutés ou mis à jour pour le comportement modifié
- [ ] Previews Compose pour les nouveaux écrans (états vide, chargé, erreur)
- [ ] Textes en français dans `strings.xml`, relus avec `docs/ux-writing.md`
- [ ] Aucune donnée personnelle dans les logs ; impact vie privée évalué
- [ ] Accessibilité : `contentDescription`, cibles ≥ 48 dp, contraste, police dynamique
- [ ] Doc ou ADR mis à jour si une décision d'architecture change
