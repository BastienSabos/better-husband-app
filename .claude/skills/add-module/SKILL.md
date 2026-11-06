---
name: add-module
description: Extrait ou crée un module Gradle (core ou feature) quand c'est justifié, avec convention plugins dans build-logic. À utiliser seulement quand :app devient trop gros ou qu'une frontière de dépendance doit être imposée.
---

# Ajouter un module Gradle

## Est-ce justifié ?

Oui si au moins un : le module `:app` dépasse ~15 000 lignes ou ~60 s de build incrémental, une couche doit
être isolée (ex. `:core:database` pour que l'UI ne voie pas Room), ou du code doit être partagé (widget, v2).
Sinon, reste dans `:app` avec des packages par feature.

## Cible

```
:app
:core:model        (Kotlin pur, modèles domaine)
:core:database     (Room + SQLCipher)
:core:data         (repositories)
:core:designsystem (thème, composants)
:feature:<nom>     (écrans d'une feature, dépend de :core:*)
```

## Étapes

1. Première extraction seulement : crée `build-logic/convention` (inclus via `includeBuild("build-logic")` dans
   `pluginManagement`), avec des convention plugins `betterhusband.android.library`,
   `betterhusband.android.library.compose`, `betterhusband.android.feature`, `betterhusband.hilt` qui
   centralisent compileSdk, minSdk, Lint, Compose et Hilt. Modèle : Now in Android.
2. Crée le module avec son `build.gradle.kts` minimal appliquant le convention plugin, `namespace`
   `com.bastiensabos.betterhusband.<chemin>`.
3. `include(":core:xxx")` dans `settings.gradle.kts`.
4. Déplace le code avec `git mv` pour garder l'historique, ajuste les visibilités (`internal` par défaut).
5. Les modules `feature` ne dépendent jamais les uns des autres.
6. `./gradlew check` vert, puis mets à jour `docs/architecture.md` et écris un ADR (skill `adr`).
