---
name: reminders
description: Implémente des rappels et notifications (dates clés J-14/J-3/jour J, flux du jour, tâches récurrentes) avec WorkManager et des notifications discrètes. À utiliser pour tout travail en arrière-plan ou toute notification.
---

# Rappels et notifications

## Planification

- WorkManager (ADR 0001) : `OneTimeWorkRequest` avec `setInitialDelay` pour une date précise,
  `PeriodicWorkRequest` pour le flux du jour. Pas d'`AlarmManager` exact sauf besoin prouvé.
- Travaux **uniques et idempotents** : `enqueueUniqueWork(nomStable, ExistingWorkPolicy.REPLACE, …)`, nom dérivé
  de l'identifiant métier (`reminder-date-<id>-j14`).
- Replanifier quand la donnée change, et après redémarrage ou mise à jour (WorkManager persiste, vérifier quand même).
- Workers `@HiltWorker` avec `HiltWorkerFactory` ; logique métier dans un cas d'usage testable, le Worker reste fin.
- Temps injecté (`Clock`), calculs en `java.time` avec le fuseau de l'appareil ; tester changement d'heure et 29 février.

## Notifications

- Permission `POST_NOTIFICATIONS` (Android 13+) demandée en contexte, après avoir expliqué l'intérêt.
- Canaux dédiés et nommés en français (« Dates importantes », « Idée du jour »).
- **Discrétion** : `setVisibility(VISIBILITY_PRIVATE)` + `setPublicVersion(...)` au texte neutre
  (« Tu as un rappel »). Jamais de prénom, de cadeau ou de détail intime visible sur l'écran verrouillé.
- Une action utile par notification (« Voir l'idée », « C'est fait »), `PendingIntent.FLAG_IMMUTABLE`.

## Tests

- Cas d'usage de calcul des échéances : tests unitaires purs avec horloge fixe.
- Workers : `TestListenableWorkerBuilder` / `work-testing` en test.

Relecture `privacy-guardian` obligatoire pour toute notification.
