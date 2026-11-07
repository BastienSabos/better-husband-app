package com.bastiensabos.betterhusband.domain

import app.cash.turbine.test
import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.Relative
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ObserveUpcomingDatesUseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2027-03-10T09:00:00Z"), ZoneOffset.UTC)

    private fun useCase(
        partner: Partner? = null,
        dates: List<ImportantDate> = emptyList(),
    ) = ObserveUpcomingDatesUseCase(FakePartnerRepository(partner), FakeImportantDateRepository(dates), clock)

    @Test
    fun `no partner and no dates gives an empty list`() = runTest {
        useCase()().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `partner dates and custom dates are merged and sorted by next occurrence`() = runTest {
        val partner =
            Partner(
                firstName = "Camille",
                addressForm = AddressForm.NEUTRAL,
                birthday = LocalDate.of(1992, 4, 12),
                relationshipStart = LocalDate.of(2018, 3, 12),
                relatives = listOf(Relative("Léa", "Sœur", LocalDate.of(1989, 3, 11)), Relative("Sam", "Ami")),
            )
        val custom = ImportantDate(7, "Mariage", DateKind.ANNIVERSARY, LocalDate.of(2015, 6, 20), recurring = true)

        useCase(partner, listOf(custom))().test {
            val items = awaitItem()
            assertThat(items.map { it.key })
                .containsExactly("relative-0", "relationship-start", "partner-birthday", "date-7")
                .inOrder()
            assertThat(items.first().label).isEqualTo(DateLabel.RelativeBirthday("Léa"))
            assertThat(items[2].label).isEqualTo(DateLabel.PartnerBirthday("Camille"))
            assertThat(items.first().daysUntil).isEqualTo(1)
            assertThat(items.first().yearsSince).isEqualTo(38)
            assertThat(items.last().editableId).isEqualTo(7)
        }
    }

    @Test
    fun `past one-off events are dropped`() = runTest {
        val past = ImportantDate(1, "Médecin", DateKind.OTHER, LocalDate.of(2027, 1, 5), recurring = false)
        val future = ImportantDate(2, "Voyage", DateKind.OTHER, LocalDate.of(2027, 7, 1), recurring = false)

        useCase(dates = listOf(past, future))().test {
            val items = awaitItem()
            assertThat(items.map { it.key }).containsExactly("date-2")
            assertThat(items.single().yearsSince).isNull()
        }
    }
}
