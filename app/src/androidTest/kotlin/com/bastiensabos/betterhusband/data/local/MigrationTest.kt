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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
