@AGENTS.md

# Spécifique à Claude Code

## Workflow par défaut

Pour toute tâche non triviale, suivre le skill `ship-feature` : comprendre → planifier → tester d'abord →
implémenter → vérifier → relire → PR. Ne jamais déclarer une tâche finie sans avoir lancé `./gradlew check`
(ou expliqué pourquoi ce n'était pas possible, par exemple SDK Android absent).

- Explorer avant de modifier : lire le code voisin et reproduire ses patterns.
- Petits pas vérifiables : compiler et tester souvent plutôt qu'un gros diff à la fin.
- En cas de doute sur le produit, relire la spec et `AGENTS.md` ; si le doute persiste, demander.
- Ne jamais contourner un hook, un test ou un check (`--no-verify`, `@Ignore`, baseline Lint) pour avancer.

## Sous-agents (`.claude/agents/`)

| Agent | Quand l'utiliser |
|---|---|
| `android-architect` | Avant une feature : plan d'implémentation, découpage, impacts. Lecture seule. |
| `code-reviewer` | Après implémentation, avant la PR : bugs, architecture, Compose, coroutines. |
| `test-engineer` | Écrire ou compléter les tests (ViewModel, repository, Room, Compose). |
| `privacy-guardian` | Dès qu'une donnée personnelle, une notification, un stockage ou une dépendance change. |
| `ux-copy-reviewer` | Nouveaux textes, écrans ou notifications : ton, clarté, accessibilité. |

Lancer les relectures (`code-reviewer`, `privacy-guardian`, `ux-copy-reviewer`) en parallèle quand elles
s'appliquent toutes.

## Skills (`.claude/skills/`, invocables en `/nom`)

| Skill | Rôle |
|---|---|
| `ship-feature` | Workflow complet d'une issue jusqu'à la PR |
| `compose-screen` | Créer un écran Compose (Route/Screen/ViewModel/UiState + previews + tests) |
| `room-data` | Entités, DAO, migrations Room + SQLCipher, avec tests |
| `reminders` | Rappels et notifications discrètes via WorkManager |
| `add-dependency` | Ajouter une dépendance (catalogue, licence, vie privée, taille) |
| `add-module` | Extraire un module Gradle quand c'est justifié |
| `verify` | Porte de qualité locale complète et lecture des erreurs |
| `adr` | Rédiger une décision d'architecture |
| `release` | Préparer une version Play Store |

## Hooks (`.claude/settings.json`)

- **SessionStart** : en session cloud, installe JDK/SDK Android si le réseau le permet et affiche l'état du repo.
- **PreToolUse Bash** : bloque push sur `main`, `--force`, `--no-verify`, et la lecture des secrets.
- **PreToolUse Edit/Write** : bloque l'édition des secrets et des fichiers générés.
- **PostToolUse Edit/Write** : formate le fichier Kotlin modifié avec ktlint s'il est installé.

## Environnement

- Session cloud : le build Android a besoin de `dl.google.com` et `maven.google.com` dans la politique réseau
  de l'environnement. Sans eux, se limiter à l'édition et laisser la CI vérifier (le dire dans la PR).
- La CI GitHub (`.github/workflows/ci.yml`) est la référence : une PR n'est prête que si elle est verte.
