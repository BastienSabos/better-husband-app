package com.bastiensabos.betterhusband.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity
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

    private fun partner(name: String) =
        PartnerEntity(firstName = name, addressForm = "NEUTRAL", birthday = LocalDate.of(1990, 5, 17), relationshipStart = null)

    @Test
    fun replaceProfileRoundTripsEverything() = runBlocking {
        val dao = database.partnerDao()

        dao.replaceProfile(
            partner = partner("Alex"),
            tastes = listOf(TasteEntity(category = "FLOWERS", label = "Pivoines")),
            sizes = listOf(SizeEntity(kind = "SHOES", value = "39")),
            relatives = listOf(RelativeEntity(relation = "Sœur", name = "Camille", birthday = LocalDate.of(1988, 3, 2))),
        )

        assertThat(dao.observePartner().first()).isEqualTo(partner("Alex"))
        assertThat(dao.observeTastes().first().map { it.label }).containsExactly("Pivoines")
        assertThat(dao.observeSizes().first().map { it.value }).containsExactly("39")
        assertThat(dao.observeRelatives().first().single().birthday).isEqualTo(LocalDate.of(1988, 3, 2))
    }

    @Test
    fun replaceProfileDropsPreviousDetails() = runBlocking {
        val dao = database.partnerDao()
        dao.replaceProfile(partner("Alex"), listOf(TasteEntity(category = "FOOD", label = "Sushis")), emptyList(), emptyList())

        dao.replaceProfile(partner("Camille"), emptyList(), emptyList(), emptyList())

        assertThat(dao.observePartner().first()?.firstName).isEqualTo("Camille")
        assertThat(dao.observeTastes().first()).isEmpty()
    }

    @Test
    fun deleteAllEmptiesEveryTable() = runBlocking {
        val dao = database.partnerDao()
        dao.replaceProfile(partner("Alex"), listOf(TasteEntity(category = "FOOD", label = "Sushis")), emptyList(), emptyList())

        dao.deleteAll()

        assertThat(dao.observePartner().first()).isNull()
        assertThat(dao.observeTastes().first()).isEmpty()
    }
}
