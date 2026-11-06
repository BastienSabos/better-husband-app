---
name: release
description: Prépare une version Play Store (numéro de version, notes, vérifications vie privée, tag déclenchant le workflow de release). À lancer uniquement à la demande explicite de l'utilisateur.
disable-model-invocation: true
argument-hint: "[version, ex. 0.2.0]"
---

# Préparer une release

Publier est irréversible côté utilisateurs : chaque étape externe attend le feu vert explicite de l'utilisateur.

## Vérifications

1. `main` à jour, CI verte sur le dernier commit.
2. Agent `privacy-guardian` sur le diff depuis le dernier tag (`git diff <dernier-tag>..main`).
3. Formulaire « Sécurité des données » et politique de confidentialité toujours exacts (sinon, le signaler).
4. `applicationId` inchangé (il est définitif après la première publication).

## Préparer

1. Branche `chore/release-<version>`.
2. Dans `app/build.gradle.kts` : `versionName = "<version>"` (SemVer), `versionCode` + 1 (entier strictement croissant).
3. Notes de version en français dans `fastlane/metadata/android/fr-FR/changelogs/<versionCode>.txt`
   (≤ 500 caractères, orientées utilisateur), générées depuis les commits `feat:` et `fix:`.
4. PR `chore(release): <version>`, merge après revue.

## Publier (après accord explicite)

1. `git tag v<version>` sur le commit mergé, puis `git push origin v<version>`.
2. Le workflow `.github/workflows/release.yml` construit l'AAB signé et le pousse sur la piste **interne**.
3. Promotion vers fermée / production : manuelle dans la Play Console, par l'utilisateur.
