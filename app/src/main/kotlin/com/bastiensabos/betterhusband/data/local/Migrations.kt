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
