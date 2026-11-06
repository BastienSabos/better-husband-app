---
name: ship-feature
description: Workflow complet pour livrer une feature, un fix ou une issue GitHub, de la compréhension jusqu'à la PR verte. À utiliser pour toute tâche de code non triviale sur ce dépôt.
argument-hint: "[numéro d'issue ou description]"
---

# Livrer une feature

Objectif : une PR petite, testée, relue, verte en CI, conforme à la Definition of Done d'`AGENTS.md`.

## 1. Comprendre

- Si un numéro d'issue est fourni, lis l'issue et ses commentaires (GitHub MCP ou `gh issue view`).
- Relis la section concernée de la spec produit et `AGENTS.md`. Vérifie que c'est dans le MVP.
- Reformule en une phrase l'objectif et liste les critères d'acceptation. S'ils manquent ou sont ambigus,
  pose la question avant de coder (une seule fois, toutes les questions ensemble).

## 2. Planifier

- Tâche de plus de ~3 fichiers ou touchant aux données : délègue le plan à l'agent `android-architect`.
- Crée la branche depuis `main` à jour : `git switch -c feat/<sujet>` (ou `fix/`, `chore/`).
- Découpe en incréments compilables. Chaque incrément = un commit.

## 3. Tester d'abord

- Écris d'abord les tests du comportement attendu (agent `test-engineer` si utile) et vérifie qu'ils échouent.
- Bug : un test qui reproduit le bug avant tout correctif.

## 4. Implémenter

- Utilise les skills dédiés : `compose-screen`, `room-data`, `reminders`, `add-dependency`.
- Reproduis les patterns existants. Textes dans `strings.xml`. Pas de donnée perso dans les logs.
- Après chaque incrément : `./gradlew testDebugUnitTest` puis commit Conventional Commits.

## 5. Vérifier

- Lance le skill `verify` (`./gradlew spotlessApply check`). Corrige jusqu'au vert.
- Si le SDK Android n'est pas disponible dans l'environnement, dis-le explicitement : la CI vérifiera.

## 6. Relire

Lance en parallèle les relectures qui s'appliquent :

- `code-reviewer` : toujours.
- `privacy-guardian` : données, stockage, notifications, permissions, dépendances.
- `ux-copy-reviewer` : nouveaux textes ou écrans.

Corrige les constats bloquants et « à corriger ». Justifie dans la PR ceux que tu écartes.

## 7. Ouvrir la PR

- `git push -u origin <branche>` puis PR vers `main` en remplissant `.github/pull_request_template.md`.
- Titre au format Conventional Commits. Lie l'issue (`Closes #N`).
- Joins les captures d'écran des previews si l'UI change.
- Surveille la CI ; en cas d'échec, lis les logs, reproduis en local, corrige par un nouveau commit.

## Ne jamais

- Pousser sur `main`, forcer un push, utiliser `--no-verify`, désactiver un test ou une règle Lint pour passer.
- Annoncer « terminé » sans résultat de `./gradlew check` ou de la CI.
