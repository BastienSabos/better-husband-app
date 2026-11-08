package com.bastiensabos.betterhusband.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2 : goûts, tailles et proches de la fiche partenaire. La table `partner` ne change pas. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `partner_taste` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`category` TEXT NOT NULL, `label` TEXT NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `partner_size` " +
                    "(`kind` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`kind`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `partner_relative` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`relation` TEXT NOT NULL, `name` TEXT NOT NULL, `birthday` TEXT)",
            )
        }
    }

/** v2 → v3 : dates qui comptent saisies par l'utilisateur. */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `important_date` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`kind` TEXT NOT NULL, `date` TEXT NOT NULL, `recurring` INTEGER NOT NULL)",
            )
        }
    }

/** v3 → v4 : carnet d'idées. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `idea` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `done` INTEGER NOT NULL)",
            )
        }
    }

/** v4 → v5 : tâches récurrentes du foyer. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `household_task` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`repeat` TEXT NOT NULL, `created_on` TEXT NOT NULL, `last_done_on` TEXT)",
            )
        }
    }

/** v5 → v6 : historique des attentions du jour (une ligne par jour). */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `attention_log` " +
                    "(`date` TEXT NOT NULL, `attention_id` INTEGER NOT NULL, `done` INTEGER NOT NULL, PRIMARY KEY(`date`))",
            )
        }
    }

/** v6 → v7 : profil du couple (enfants, vie commune, budget, rythme, centres d'intérêt), pour filtrer les attentions. */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `couple_profile` " +
                    "(`id` INTEGER NOT NULL, `kids` TEXT, `living` TEXT, `budget` TEXT, `rhythm` TEXT, " +
                    "`interests` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

/** v7 → v8 : situation familiale du profil (famille recomposée, garde alternée), pour des attentions qui s'y prêtent. */
val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `couple_profile` ADD COLUMN `family` TEXT NOT NULL DEFAULT ''")
        }
    }

/**
 * v8 → v9 : mode duo (ADR 0004, `docs/duo.md`). Les tâches et les dates gagnent un identifiant stable et les colonnes
 * de synchro (tout reste privé : `shared` = 0) ; trois tables nouvelles : envies, membres du duo, état de la synchro.
 */
val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            for (table in listOf("household_task", "important_date")) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `shared` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `revision` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `author` TEXT")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `local_changed_at` INTEGER NOT NULL DEFAULT 0")
                // Un UUID version 4 par ligne existante (randomblob est évalué pour chaque ligne).
                db.execSQL("UPDATE `$table` SET `uid` = $UUID_V4_SQL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_uid` ON `$table` (`uid`)")
            }
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `wish` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `owner` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                    "`link` TEXT, `created_at` INTEGER NOT NULL, `uid` TEXT NOT NULL DEFAULT '', " +
                    "`shared` INTEGER NOT NULL DEFAULT 0, `revision` INTEGER NOT NULL DEFAULT 0, `author` TEXT, " +
                    "`deleted` INTEGER NOT NULL DEFAULT 0, `local_changed_at` INTEGER NOT NULL DEFAULT 0)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_wish_uid` ON `wish` (`uid`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `duo_member` " +
                    "(`member_id` TEXT NOT NULL, `is_me` INTEGER NOT NULL, PRIMARY KEY(`member_id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `duo_state` " +
                    "(`id` INTEGER NOT NULL, `duo_id` TEXT NOT NULL, `last_seen` INTEGER NOT NULL, " +
                    "`pushed_until` INTEGER NOT NULL, `ended` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

private const val UUID_V4_SQL =
    "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
        "substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"
