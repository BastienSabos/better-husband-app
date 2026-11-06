package com.bastiensabos.betterhusband.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PartnerDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertRoundTripsDates() = runBlocking {
        val entity = PartnerEntity(firstName = "Alex", addressForm = "NEUTRAL", birthday = LocalDate.of(1990, 5, 17), relationshipStart = null)

        database.partnerDao().upsert(entity)

        assertThat(database.partnerDao().observe().first()).isEqualTo(entity)
    }

    @Test
    fun upsertKeepsASinglePartner() = runBlocking {
        val dao = database.partnerDao()
        dao.upsert(PartnerEntity(firstName = "Alex", addressForm = "NEUTRAL", birthday = null, relationshipStart = null))
        dao.upsert(PartnerEntity(firstName = "Camille", addressForm = "FEMININE", birthday = null, relationshipStart = null))

        assertThat(dao.observe().first()?.firstName).isEqualTo("Camille")
    }
}
