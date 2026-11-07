package com.bastiensabos.betterhusband.data.work

import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.PlannedReminder
import com.bastiensabos.betterhusband.domain.ReminderPlanner
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReminderSyncTest {
    private val clock = Clock.fixed(Instant.parse("2027-03-10T09:00:00Z"), ZoneOffset.UTC)
    private val dates = FakeImportantDateRepository()
    private val scheduler = RecordingScheduler()

    private fun sync(scope: kotlinx.coroutines.CoroutineScope) =
        ReminderSync(ObserveUpcomingDatesUseCase(FakePartnerRepository(), dates, clock), ReminderPlanner(clock), scheduler, scope)

    @Test
    fun `syncOnce plans reminders for current dates`() = runTest {
        dates.save(ImportantDate(title = "Mariage", kind = DateKind.ANNIVERSARY, date = LocalDate.of(2015, 4, 12), recurring = true))

        sync(this).syncOnce()

        assertThat(scheduler.calls.single().map { it.offsetDays }).containsExactly(14, 3, 0).inOrder()
    }

    @Test
    fun `start ensures the daily sync and reschedules when dates change`() = runTest {
        sync(backgroundScope).start()
        advanceUntilIdle()
        assertThat(scheduler.dailySyncEnsured).isTrue()
        assertThat(scheduler.calls.last()).isEmpty()

        dates.save(ImportantDate(title = "Mariage", kind = DateKind.ANNIVERSARY, date = LocalDate.of(2015, 4, 12), recurring = true))
        advanceUntilIdle()

        assertThat(scheduler.calls.last()).hasSize(3)
    }

    @Test
    fun `an unchanged plan is not rescheduled again`() = runTest {
        sync(backgroundScope).start()
        advanceUntilIdle()
        val before = scheduler.calls.size

        // Modifier un titre ne change aucun rappel : pas de nouvelle planification.
        val id = dates.save(ImportantDate(title = "A", kind = DateKind.OTHER, date = LocalDate.of(2027, 9, 1), recurring = false))
        advanceUntilIdle()
        dates.save(ImportantDate(id, "B", DateKind.OTHER, LocalDate.of(2027, 9, 1), recurring = false))
        advanceUntilIdle()

        assertThat(scheduler.calls.size).isEqualTo(before)
    }

    private class RecordingScheduler : ReminderScheduler {
        val calls = mutableListOf<List<PlannedReminder>>()
        var dailySyncEnsured = false

        override fun reschedule(reminders: List<PlannedReminder>) {
            calls += reminders
        }

        override fun ensureDailySync() {
            dailySyncEnsured = true
        }
    }
}
