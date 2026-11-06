---
name: verify
description: Lance la porte de qualité locale (format, Lint, tests, build) et interprète les erreurs jusqu'au vert. À utiliser avant chaque commit significatif, avant une PR, et quand la CI échoue.
---

# Vérifier

```bash
./gradlew spotlessApply                     # corrige le format
./gradlew check assembleDebug --continue    # spotlessCheck + lint + tests unitaires + build
```

## Lire les échecs

- **Spotless/ktlint** : relance `spotlessApply` ; s'il reste une erreur, elle n'est pas auto-corrigeable
  (nommage, ligne trop longue, règle Compose) : corrige à la main. Ne désactive pas la règle.
- **Lint** : rapport dans `app/build/reports/lint-results-debug.html` (ou `.txt`). Corrige la cause.
  Pas de `@SuppressLint` ni de baseline sans justification écrite dans le code et accord de l'utilisateur.
- **Tests** : rapport dans `app/build/reports/tests/testDebugUnitTest/`. Relance le test seul avec `--tests`.
  Un test instable se corrige (temps, ordre, dispatcher), il ne s'ignore pas.
- **Compilation Kotlin/KSP** : lis la première erreur, pas la dernière.

## Échec en CI seulement

1. Récupère les logs du job (GitHub MCP `get_job_logs`).
2. Reproduis la même commande en local. Différences fréquentes : cache, locale, fuseau (`TZ=UTC`), JDK.
3. Corrige par un nouveau commit, jamais en relançant la CI à vide.

## SDK Android absent

Si `./gradlew` échoue faute de SDK ou de réseau (`dl.google.com`), dis-le clairement dans ta réponse et dans la PR.
Ne prétends jamais qu'une vérification a réussi sans l'avoir vue réussir.
