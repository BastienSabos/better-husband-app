package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.data.local.entity.RelativeEntity
import com.bastiensabos.betterhusband.data.local.entity.SizeEntity
import com.bastiensabos.betterhusband.data.local.entity.TasteEntity
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.bastiensabos.betterhusband.domain.model.SizeKind
import com.bastiensabos.betterhusband.domain.model.Taste
import com.bastiensabos.betterhusband.domain.model.TasteCategory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class RoomPartnerRepositoryTest {
    private val dao = FakePartnerDao()
    private val repository = RoomPartnerRepository(dao)

    @Test
    fun `no partner before onboarding`() =
        runTest {
            repository.observePartner().test {
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `saved partner is observed with all fields`() =
        runTest {
            val partner =
                Partner(
                    firstName = "Alex",
                    addressForm = AddressForm.NEUTRAL,
                    birthday = LocalDate.of(1990, 5, 17),
                    relationshipStart = LocalDate.of(2018, 9, 1),
                    tastes = listOf(Taste(TasteCategory.FLOWERS, "Pivoines"), Taste(TasteCategory.COLORS, "Vert")),
                    sizes = mapOf(SizeKind.SHOES to "39", SizeKind.RING to "52"),
                    relatives = listOf(Relative("Camille", "Sœur", LocalDate.of(1988, 3, 2)), Relative("Sam", "Ami")),
                )

            repository.savePartner(partner)

            repository.observePartner().test {
                assertThat(awaitItem()).isEqualTo(partner)
            }
        }

    @Test
    fun `saving again replaces the whole profile and trims the name`() =
        runTest {
            repository.savePartner(
                Partner("Alex", AddressForm.NEUTRAL, tastes = listOf(Taste(TasteCategory.FOOD, "Sushis"))),
            )
            repository.savePartner(Partner("  Camille ", AddressForm.FEMININE))

            repository.observePartner().test {
                assertThat(awaitItem()).isEqualTo(Partner("Camille", AddressForm.FEMININE))
            }
        }

    @Test
    fun `unknown stored values fall back or are ignored`() =
        runTest {
            dao.upsertPartner(PartnerEntity(firstName = "Sam", addressForm = "FROM_THE_FUTURE", birthday = null, relationshipStart = null))
            dao.insertTastes(listOf(TasteEntity(category = "SPACE", label = "Étoiles"), TasteEntity(category = "COLORS", label = "Bleu")))

            repository.observePartner().test {
                val partner = awaitItem()
                assertThat(partner?.addressForm).isEqualTo(AddressForm.NEUTRAL)
                assertThat(partner?.tastes).containsExactly(Taste(TasteCategory.COLORS, "Bleu"))
            }
        }

    @Test
    fun `delete removes the partner and every detail`() =
        runTest {
            repository.savePartner(Partner("Alex", AddressForm.NEUTRAL, relatives = listOf(Relative("Camille", "Sœur"))))

            repository.deletePartner()

            repository.observePartner().test {
                assertThat(awaitItem()).isNull()
            }
            assertThat(dao.relatives.value).isEmpty()
        }

    private class FakePartnerDao : PartnerDao() {
        val partner = MutableStateFlow<PartnerEntity?>(null)
        val tastes = MutableStateFlow<List<TasteEntity>>(emptyList())
        val sizes = MutableStateFlow<List<SizeEntity>>(emptyList())
        val relatives = MutableStateFlow<List<RelativeEntity>>(emptyList())

        override fun observePartner(): Flow<PartnerEntity?> = partner

        override fun observeTastes(): Flow<List<TasteEntity>> = tastes

        override fun observeSizes(): Flow<List<SizeEntity>> = sizes

        override fun observeRelatives(): Flow<List<RelativeEntity>> = relatives

        override suspend fun upsertPartner(partner: PartnerEntity) {
            this.partner.value = partner
        }

        override suspend fun insertTastes(tastes: List<TasteEntity>) {
            this.tastes.value += tastes
        }

        override suspend fun insertSizes(sizes: List<SizeEntity>) {
            this.sizes.value = this.sizes.value.filter { old -> sizes.none { it.kind == old.kind } } + sizes
        }

        override suspend fun insertRelatives(relatives: List<RelativeEntity>) {
            this.relatives.value += relatives
        }

        override suspend fun deletePartner() {
            partner.value = null
        }

        override suspend fun deleteTastes() {
            tastes.value = emptyList()
        }

        override suspend fun deleteSizes() {
            sizes.value = emptyList()
        }

        override suspend fun deleteRelatives() {
            relatives.value = emptyList()
        }
    }
}
