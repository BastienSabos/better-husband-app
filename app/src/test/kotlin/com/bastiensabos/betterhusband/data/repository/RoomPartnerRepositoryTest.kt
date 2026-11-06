package com.bastiensabos.betterhusband.data.repository

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.local.dao.PartnerDao
import com.bastiensabos.betterhusband.data.local.entity.PartnerEntity
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.Partner
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
    fun `no partner before onboarding`() = runTest {
        repository.observePartner().test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `saved partner is observed with all fields`() = runTest {
        val partner = Partner("Alex", AddressForm.NEUTRAL, LocalDate.of(1990, 5, 17), LocalDate.of(2018, 9, 1))

        repository.savePartner(partner)

        repository.observePartner().test {
            assertThat(awaitItem()).isEqualTo(partner)
        }
    }

    @Test
    fun `saving again replaces the single partner and trims the name`() = runTest {
        repository.savePartner(Partner("Alex", AddressForm.NEUTRAL))
        repository.savePartner(Partner("  Camille ", AddressForm.FEMININE))

        repository.observePartner().test {
            assertThat(awaitItem()).isEqualTo(Partner("Camille", AddressForm.FEMININE))
        }
    }

    @Test
    fun `unknown stored address form falls back to neutral`() = runTest {
        dao.upsert(PartnerEntity(firstName = "Sam", addressForm = "FROM_THE_FUTURE", birthday = null, relationshipStart = null))

        repository.observePartner().test {
            assertThat(awaitItem()?.addressForm).isEqualTo(AddressForm.NEUTRAL)
        }
    }

    @Test
    fun `delete removes the partner`() = runTest {
        repository.savePartner(Partner("Alex", AddressForm.NEUTRAL))

        repository.deletePartner()

        repository.observePartner().test {
            assertThat(awaitItem()).isNull()
        }
    }

    private class FakePartnerDao : PartnerDao {
        private val state = MutableStateFlow<PartnerEntity?>(null)

        override fun observe(): Flow<PartnerEntity?> = state

        override suspend fun upsert(partner: PartnerEntity) {
            state.value = partner
        }

        override suspend fun delete() {
            state.value = null
        }
    }
}
