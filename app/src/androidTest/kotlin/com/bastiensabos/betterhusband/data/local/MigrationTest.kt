package com.bastiensabos.betterhusband.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Nécessite `app/schemas/.../1.json` et `2.json` committés (générés par le build, jamais à la main). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsThePartnerAndAddsDetailTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO partner (id, first_name, address_form, birthday, relationship_start) " +
                    "VALUES (1, 'Alex', 'NEUTRAL', '1990-05-17', NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT first_name FROM partner").use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getString(0)).isEqualTo("Alex")
        }
        db.query("SELECT COUNT(*) FROM partner_taste").use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(0)
        }
    }

    @Test
    fun migrate2To3AddsImportantDates() {
        helper.createDatabase(DB_NAME, 2).close()

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        db.execSQL("INSERT INTO important_date (title, kind, date, recurring) VALUES ('Mariage', 'ANNIVERSARY', '2015-06-20', 1)")
        db.query("SELECT COUNT(*) FROM important_date").use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun migrate3To4AddsIdeas() {
        helper.createDatabase(DB_NAME, 3).close()

        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        db.execSQL("INSERT INTO idea (text, category, created_at, done) VALUES ('Pivoines', 'GIFT', 1800000000000, 0)")
        db.query("SELECT COUNT(*) FROM idea").use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun migrate4To5AddsHouseholdTasks() {
        helper.createDatabase(DB_NAME, 4).close()

        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.execSQL(
            "INSERT INTO household_task (title, `repeat`, created_on, last_done_on) " +
                "VALUES ('Poubelles', 'WEEKLY', '2027-01-04', NULL)",
        )
        db.query("SELECT COUNT(*) FROM household_task").use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun migrate5To6AddsAttentionLog() {
        helper.createDatabase(DB_NAME, 5).close()

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)

        db.execSQL("INSERT INTO attention_log (date, attention_id, done) VALUES ('2027-01-20', 12, 0)")
        db.query("SELECT COUNT(*) FROM attention_log").use {
            it.moveToFirst()
            assertThat(it.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun migrate6To7AddsCoupleProfile() {
        helper.createDatabase(DB_NAME, 6).close()

        val db = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        db.execSQL("INSERT INTO couple_profile (id, kids, interests) VALUES (1, 'YOUNG', 'SPORT')")
        db.query("SELECT kids, living FROM couple_profile").use {
            it.moveToFirst()
            assertThat(it.getString(0)).isEqualTo("YOUNG")
            assertThat(it.isNull(1)).isTrue()
        }
    }

    @Test
    fun migrate7To8AddsFamilyToCoupleProfile() {
        helper.createDatabase(DB_NAME, 7).use {
            it.execSQL("INSERT INTO couple_profile (id, kids, interests) VALUES (1, 'YOUNG', 'SPORT')")
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

        db.query("SELECT kids, family FROM couple_profile").use {
            it.moveToFirst()
            assertThat(it.getString(0)).isEqualTo("YOUNG")
            assertThat(it.getString(1)).isEmpty()
        }
    }

    @Test
    fun migrate8To9AddsSyncColumnsAndDuoTables() {
        helper.createDatabase(DB_NAME, 8).use {
            it.execSQL(
                "INSERT INTO household_task (title, `repeat`, created_on, last_done_on) " +
                    "VALUES ('Poubelles', 'WEEKLY', '2027-01-04', NULL), ('Linge', 'DAILY', '2027-01-04', NULL)",
            )
            it.execSQL(
                "INSERT INTO important_date (title, kind, date, recurring) VALUES ('Rencontre', 'ANNIVERSARY', '2019-06-01', 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 9, true, MIGRATION_8_9)

        db.query("SELECT uid, shared, deleted FROM household_task").use {
            val uids = mutableSetOf<String>()
            while (it.moveToNext()) {
                uids += it.getString(0)
                assertThat(it.getInt(1)).isEqualTo(0)
                assertThat(it.getInt(2)).isEqualTo(0)
            }
            assertThat(uids).hasSize(2)
            assertThat(uids.all { uid -> UUID_V4.matches(uid) }).isTrue()
        }
        db.query("SELECT uid FROM important_date").use {
            it.moveToFirst()
            assertThat(UUID_V4.matches(it.getString(0))).isTrue()
        }
        db.execSQL("INSERT INTO wish (owner, text, created_at, uid) VALUES ('alex', 'Un carnet', 1, 'a')")
        db.execSQL("INSERT INTO duo_member (member_id, is_me) VALUES ('alex', 1)")
        db.execSQL("INSERT INTO duo_state (id, duo_id, last_seen, pushed_until, ended) VALUES (1, 'd', 0, 0, 0)")
    }

    @Test
    fun migrate9To10LinksIdeasToWishes() {
        helper.createDatabase(DB_NAME, 9).use {
            it.execSQL("INSERT INTO idea (text, category, created_at, done) VALUES ('Pivoines', 'GIFT', 1, 0)")
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 10, true, MIGRATION_9_10)

        db.query("SELECT text, wish_uid FROM idea").use {
            it.moveToFirst()
            assertThat(it.getString(0)).isEqualTo("Pivoines")
            assertThat(it.isNull(1)).isTrue()
        }
    }

    private companion object {
        val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        const val DB_NAME = "migration-test"
    }
}
