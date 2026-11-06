# 0001. Kotlin natif, Jetpack Compose, Hilt, WorkManager

- Statut : proposé
- Date : 2026-11-06

## Contexte

L'appli de couple vise uniquement Android au lancement (Play Store, grand public francophone). Elle repose sur des briques
très natives : notifications fiables, widget d'écran d'accueil, partage Android vers le carnet d'idées,
biométrie. Un seul développeur, aidé par des agents IA.

## Décision

Kotlin, Jetpack Compose + Material 3, Android Gradle Plugin 9 avec Kotlin intégré, Hilt pour l'injection,
WorkManager pour les rappels, coroutines/Flow, catalogue de versions Gradle. minSdk 26, target/compileSdk 36.

## Options écartées

- **Flutter / React Native** : intéressant pour iOS plus tard, mais widgets, notifications et partage passent par
  des ponts natifs ; écosystème moins aligné avec les guides Android officiels sur lesquels l'IA est la plus fiable.
- **Kotlin Multiplatform + Compose Multiplatform** : à reconsidérer si iOS devient une cible ; l'architecture en
  couches le permet.
- **Koin** : plus simple, mais Hilt est le standard Android, vérifié à la compilation, et documenté partout.
- **AlarmManager exact** : demande une permission sensible ; WorkManager suffit pour des rappels à la journée.

## Conséquences

- Patterns standards (Now in Android) : bonne productivité des agents IA et docs abondantes.
- Portage iOS futur = réécriture de l'UI ou migration KMP.
