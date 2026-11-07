---
name: compose-screen
description: Crée ou refond un écran Jetpack Compose selon l'architecture du projet (Route + Screen stateless + ViewModel Hilt + UiState), avec previews, textes FR et tests. À utiliser pour tout nouvel écran ou composant d'écran.
argument-hint: "[nom de l'écran, ex. PartnerProfile]"
---

# Créer un écran Compose

Référence vivante : `app/src/main/kotlin/com/bastiensabos/betterhusband/feature/today/`.

## Fichiers (dans `feature/<feature>/`)

| Fichier | Contenu |
|---|---|
| `<Name>UiState.kt` | `sealed interface` (Loading, Empty, Ready, Error…) ou `data class` immuable |
| `<Name>ViewModel.kt` | `@HiltViewModel`, expose `StateFlow<<Name>UiState>`, méthodes `onXxx()` pour les événements |
| `<Name>Screen.kt` | `<Name>Route` (stateful, `hiltViewModel()`, `collectAsStateWithLifecycle`) + `<Name>Screen` (stateless) + previews |
| `test/.../<Name>ViewModelTest.kt` | Tests unitaires du ViewModel avec un fake repository |

## Règles

- `ViewModel` : état dérivé des `Flow` du repository avec `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
  Pas de `Context`, pas de `NavController`, pas de `LiveData`.
- `Screen` stateless : paramètres `uiState`, callbacks `onXxx`, puis `modifier: Modifier = Modifier` en premier
  paramètre optionnel, appliqué à la racine.
- Listes : `LazyColumn` avec `key`, collections immuables dans l'état.
- Previews privées pour chaque état significatif (vide, rempli, erreur), avec `BetterHusbandTheme(dynamicColor = false)`.
- Textes : `stringResource(R.string.xxx)` ; clés préfixées par l'écran (`partner_profile_title`).
- Accessibilité : `contentDescription`, cibles ≥ 48 dp, pas de taille fixe sur du texte.
- Ajoute la destination à la navigation quand elle existera (Navigation Compose type-safe).

## Tests

- ViewModel : états initiaux, transitions, événements, erreurs du repository (Turbine).
- Si `viewModelScope` est utilisé, ajoute une `MainDispatcherRule` (`Dispatchers.setMain(StandardTestDispatcher())`).
- UI critique : test Compose dans `androidTest` sur `<Name>Screen` avec un état fixe.

Ajoute l'écran à `ScreensCaptureTest` (`captureScreen`), lance `./gradlew recordRoborazziDebug`, **regarde les PNG** de
`app/src/test/screenshots/` (surtout `_police_200`) et commite-les. Termine par `./gradlew spotlessApply verifyRoborazziDebug lintDebug` et demande une relecture `ux-copy-reviewer`.
