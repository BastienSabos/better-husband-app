# Configuration IA du dépôt

Ce dépôt est configuré pour être développé avec des agents IA, Claude Code en premier.

## Ce qui est en place

| Élément | Fichier | Rôle |
|---|---|---|
| Instructions partagées | `AGENTS.md` | Produit, règles vie privée, stack, conventions, Definition of Done. Lu par tous les agents. |
| Instructions Claude | `CLAUDE.md` | Importe `AGENTS.md`, décrit agents, skills, hooks et workflow. |
| Réglages d'équipe | `.claude/settings.json` | Permissions (autorisées et interdites) et hooks. |
| Hooks | `.claude/hooks/` | Garde-fous git et secrets, formatage auto, préparation des sessions cloud. |
| Sous-agents | `.claude/agents/` | Architecte, reviewer, testeur, gardien vie privée, relecteur UX. |
| Skills | `.claude/skills/` | Procédures : livrer une feature, écran Compose, Room, rappels, dépendances, release… |
| CI | `.github/workflows/ci.yml` | Format, Lint, tests, build sur chaque PR. |
| Claude dans GitHub | `.github/workflows/claude.yml`, `claude-code-review.yml` | `@claude` dans issues/PR, revue auto. |
| Dépendances | `.github/dependabot.yml` | Mises à jour hebdomadaires groupées. |
| Templates | `.github/ISSUE_TEMPLATE/`, `pull_request_template.md` | Issues exploitables par un agent, PR normalisées. |

Les réglages personnels vont dans `.claude/settings.local.json` et `CLAUDE.local.md` (ignorés par git).

## À faire une fois par Bastien

1. **Claude dans GitHub** : dans Claude Code, lancer `/install-github-app` et suivre les étapes. Cela installe
   l'app GitHub Claude et ajoute le secret `CLAUDE_CODE_OAUTH_TOKEN` (abonnement) ou `ANTHROPIC_API_KEY` au dépôt.
   Sans ce secret, les workflows `claude*.yml` sont ignorés proprement.
2. **Protection de `main`** : voir `docs/sdlc.md`.
3. **Réseau des sessions cloud** : pour que Claude puisse builder dans le cloud, autoriser `dl.google.com`,
   `maven.google.com`, `services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org` et
   `repo.maven.apache.org` dans la politique réseau de l'environnement claude.ai/code.
4. **Release** (plus tard) : créer la clé d'upload, puis les secrets `ANDROID_KEYSTORE_BASE64`,
   `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` et `PLAY_SERVICE_ACCOUNT_JSON`.

## Faire évoluer la config

- Une erreur récurrente de l'IA se corrige à la source : une règle dans `AGENTS.md`, une étape dans un skill ou un
  hook qui la rend impossible.
- `AGENTS.md` reste court (il est chargé à chaque session) ; le détail va dans `docs/` ou dans un skill.
- Toute modification de cette config passe par une PR comme le reste du code.
