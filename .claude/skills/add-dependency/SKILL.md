---
name: add-dependency
description: Ajoute une bibliothèque ou un plugin Gradle de façon contrôlée (catalogue de versions, dernière version stable, licence, vie privée, taille). À utiliser avant toute nouvelle dépendance.
argument-hint: "[groupe:artefact]"
---

# Ajouter une dépendance

1. **Besoin** : vérifie que la plateforme ou une dépendance existante ne couvre pas déjà le besoin.
2. **Vie privée** : refuse tout SDK d'analytics, publicité, attribution, crash reporting tiers ou qui fait du
   réseau, sauf ADR validé par l'utilisateur. Regarde ses permissions et son manifeste fusionné.
3. **Licence** : Apache 2.0, MIT, BSD acceptées. Autre licence : demander.
4. **Santé** : maintenu (release < 12 mois), largement utilisé, compatible avec nos versions de Kotlin et d'AGP.
5. **Version** : dernière version **stable** (pas d'alpha/beta sauf AndroidX justifié), vérifiée sur Maven Central
   ou Google Maven, jamais devinée.
6. **Catalogue** : ajoute la version dans `[versions]` et l'entrée dans `[libraries]` ou `[plugins]` de
   `gradle/libs.versions.toml`, nommage kebab-case (`androidx-room-runtime`). Jamais de version dans un
   `build.gradle.kts`. Les artefacts d'un BOM n'ont pas de version.
7. **Utilisation** : `implementation(libs.xxx)` dans le module concerné, le scope le plus restreint possible
   (`testImplementation`, `debugImplementation`…).
8. **Vérifie** : `./gradlew assembleRelease` passe (R8), taille de l'APK raisonnable, `lintDebug` vert.
9. Commit séparé : `build(deps): add <lib> for <raison>`.
