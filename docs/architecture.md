# Architecture

## Vue d'ensemble

App Android native, mono-module pour l'instant, organisée par feature. Local d'abord : aucune donnée ne quitte
l'appareil au MVP.

```
ui (Compose)          feature/<x>/XxxScreen.kt   Route stateful + Screen stateless + previews
   │ événements ▲ état
ViewModel             feature/<x>/XxxViewModel.kt  StateFlow<XxxUiState>
   │
domain (optionnel)    domain/                      cas d'usage purs, testables (ex. calcul des échéances)
   │
data                  data/repository/             interfaces + implémentations, exposent des Flow
   ├── local          data/local/                  Room + SQLCipher (DAO, entités, migrations)
   └── work           data/work/                   WorkManager (rappels)
```

## Règles

- **Flux unidirectionnel** : l'état descend (`StateFlow` → `collectAsStateWithLifecycle`), les événements
  remontent (callbacks `onXxx`). L'état UI est immuable.
- **Source de vérité unique** : le repository. L'UI ne parle jamais à un DAO.
- **DI** : Hilt. Modules dans `di/`. Dispatchers injectés via un qualifier (`@IoDispatcher`).
- **Temps** : `java.time.Clock` injecté partout où une date « maintenant » est utilisée.
- **Erreurs** : les repositories renvoient des résultats typés ; l'UI a toujours un état d'erreur lisible.
- **Navigation** : Navigation Compose avec routes type-safe (`@Serializable`), introduite avec le deuxième écran.

## Arborescence cible du MVP

```
com.bastiensabos.betterhusband
├── BetterHusbandApplication.kt
├── MainActivity.kt
├── di/
├── ui/theme/                    thème Material 3, couleurs, typo
├── data/{local,repository,work}/
├── domain/
└── feature/
    ├── onboarding/              brique 1 : onboarding + fiche partenaire
    ├── partner/
    ├── dates/                   brique 2 : dates et rappels
    ├── ideas/                   brique 3 : carnet d'idées (+ partage Android, widget)
    ├── today/                   brique 4 : flux du jour
    └── chores/                  brique 5 : tâches du foyer
```

Quand modulariser : voir le skill `.claude/skills/add-module`.

## Décisions

Voir [`docs/adr/`](adr/README.md). Design system : [`docs/design-system.md`](design-system.md).
