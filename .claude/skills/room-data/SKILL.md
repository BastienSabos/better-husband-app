---
name: room-data
description: Ajoute ou modifie la persistance locale chiffrée (entités Room, DAO, repository, migrations, SQLCipher) avec ses tests. À utiliser pour toute donnée stockée sur l'appareil.
---

# Persistance Room + SQLCipher

Toutes les données utilisateur vivent dans une base Room unique, chiffrée par SQLCipher (ADR 0002).

## Mise en place initiale (première brique de données uniquement)

1. Ajoute au module : plugins `ksp` et `room` (`alias(libs.plugins.room)`), dépendances `androidx.room.runtime`,
   `androidx.room.ktx`, `ksp(libs.androidx.room.compiler)`, `sqlcipher.android`, `androidx.room.testing` en test.
2. `room { schemaDirectory("$projectDir/schemas") }` : les schémas exportés sont **committés** et ne se modifient
   jamais à la main.
3. Clé : 32 octets aléatoires générés au premier lancement, chiffrés par une clé AES de l'Android Keystore,
   stockés chiffrés. Jamais de clé en dur, jamais loggée.
4. `Room.databaseBuilder(...).openHelperFactory(SupportOpenHelperFactory(passphrase))`, fourni par un module Hilt
   `@Singleton`. `System.loadLibrary("sqlcipher")` avant ouverture.

## Ajouter une entité

- `@Entity` dans `data/local/entity`, nommage SQL snake_case, index sur les colonnes filtrées ou triées.
- Dates : `java.time` (`LocalDate`, `Instant`) via `@TypeConverter`, stockées en ISO-8601 ou epoch UTC.
- DAO : fonctions `suspend` pour l'écriture, `Flow<…>` pour la lecture.
- Repository dans `data/repository` : interface + implémentation, expose des modèles domaine (pas les entités),
  binding Hilt `@Binds`.

## Migrations

- Toute modification de schéma incrémente `version` et fournit une `Migration` explicite ou une `AutoMigration`
  justifiée. **Jamais** `fallbackToDestructiveMigration()` : on ne perd pas les souvenirs d'un couple.
- Test `MigrationTestHelper` pour chaque migration, en `androidTest`.

## Tests

- DAO : base en mémoire (`Room.inMemoryDatabaseBuilder`) en `androidTest`.
- Repository : tests unitaires avec un faux DAO ou en `androidTest` avec base en mémoire.
- Données fictives uniquement.

Demande ensuite une relecture `privacy-guardian`.
