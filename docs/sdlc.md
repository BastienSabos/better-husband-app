# Cycle de développement

Un développeur (Bastien) et des agents IA (Claude Code en local, dans le cloud et dans GitHub). Le processus est
le même pour tous : rien n'arrive sur `main` sans PR verte et relue.

## Le flux

```
Issue (template feature/bug, critères d'acceptation)
  → branche feat/… ou fix/…           (Claude : skill ship-feature)
  → plan                              (agent android-architect si non trivial)
  → tests d'abord, puis code          (agent test-engineer, skills compose-screen / room-data / reminders)
  → ./gradlew check en local           (skill verify, hooks de format)
  → relectures IA                     (agents code-reviewer, privacy-guardian, ux-copy-reviewer)
  → PR (template, Conventional Commits)
  → CI : format, Lint, tests, build    (.github/workflows/ci.yml)
  → revue Claude automatique           (.github/workflows/claude-code-review.yml)
  → revue humaine et merge (squash)   (Bastien)
  → release taggée                    (skill release, .github/workflows/release.yml → piste interne Play)
```

## Branches et commits

- `main` est protégée et toujours publiable. Branches courtes (< 3 jours), une PR par sujet.
- Conventional Commits ; le titre de la PR devient le message du squash.
- Merge en **squash** uniquement, branche supprimée après merge.

## Protection de `main` (à configurer dans GitHub)

Settings → Branches → règle sur `main` :

- Exiger une PR avant merge, 0 approbation requise (dev solo) mais conversations résolues.
- Exiger les checks : `Build, lint & test` (workflow CI).
- Interdire les force push et la suppression.
- Settings → General : n'autoriser que le squash merge, supprimer les branches après merge.

## Travailler avec Claude

| Où | Comment |
|---|---|
| Terminal / IDE | `claude` à la racine. `/ship-feature #12` pour une issue complète. |
| Cloud (claude.ai/code) | Même config ; le hook SessionStart installe ktlint et le SDK si le réseau le permet. |
| GitHub | Mentionner `@claude` dans une issue ou une PR (workflow `claude.yml`). Revue auto sur chaque PR. |

Bonnes pratiques :

- Une issue bien écrite (contexte, critères d'acceptation, hors périmètre) vaut dix prompts.
- Demander un plan avant le code pour tout ce qui touche aux données.
- Relire les diffs de l'IA comme ceux d'un collègue : la responsabilité du merge reste humaine.
- Quand Claude se trompe de façon récurrente, corriger la source (`AGENTS.md`, un skill, un hook), pas le prompt.

## Qualité

| Contrôle | Outil | Où |
|---|---|---|
| Format | Spotless + ktlint (+ règles Compose) | hook PostToolUse, CI |
| Analyse statique | Android Lint, warnings = erreurs | `./gradlew check`, CI |
| Tests unitaires | JUnit4, Truth, Turbine, coroutines-test | `./gradlew check`, CI |
| Dépendances | Dependabot (Gradle + Actions), groupées | hebdomadaire |
| Secrets | `.gitignore`, hooks, secret scanning GitHub | local, GitHub |
| Revue | agents de relecture + Claude GitHub Action + humain | PR |

À ajouter quand le code le justifiera : tests de captures (Roborazzi), tests instrumentés sur Gradle Managed
Devices, couverture (Kover), Baseline Profiles.
