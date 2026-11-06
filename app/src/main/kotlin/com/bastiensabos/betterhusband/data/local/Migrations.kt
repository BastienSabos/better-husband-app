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
