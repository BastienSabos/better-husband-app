package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.data.repository.DuoSession
import com.bastiensabos.betterhusband.data.repository.FakeAttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.FakeCoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.FakeDuoRepository
import com.bastiensabos.betterhusband.data.repository.FakeHouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.FakeIdeaRepository
import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.data.repository.FakeWishRepository
import com.bastiensabos.betterhusband.data.repository.Wish
import com.bastiensabos.betterhusband.data.work.FakeReminderScheduler
import com.bastiensabos.betterhusband.domain.model.AddressForm
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.IdeaCategory
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UserDataUseCasesTest {
    private val clock = Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2027, 1, 20)
    private val partners = FakePartnerRepository(Partner("Camille", AddressForm.NEUTRAL))
    private val dates =
        FakeImportantDateRepository(listOf(ImportantDate(1, "Voyage", DateKind.OTHER, today.plusDays(30), recurring = false)))
    private val ideas = FakeIdeaRepository(listOf(Idea(1, "Un livre", IdeaCategory.GIFT, Instant.parse("2027-01-10T08:00:00Z"))))
    private val tasks = FakeHouseholdTaskRepository(listOf(HouseholdTask(1, "Courses", TaskRepeat.WEEKLY, today)))
    private val attentions = FakeAttentionLogRepository(listOf(AttentionLogEntry(today, attentionId = 3)))
    private val coupleProfile = FakeCoupleProfileRepository(CoupleProfile(kids = KidsAtHome.NONE))
    private val scheduler = FakeReminderScheduler()
    private val duo = FakeDuoRepository(DuoSession("duo-1", "me", ended = false))
    private val wishes =
        FakeWishRepository(
            mine = listOf(Wish(1, "Un vélo", Instant.parse("2027-01-12T08:00:00Z").toEpochMilli())),
            theirs = listOf(Wish(2, "Un album", Instant.parse("2027-01-15T08:00:00Z").toEpochMilli())),
        )

    @Test
    fun `export gathers every repository and stamps the current time`() =
        runTest {
            val json = ExportUserDataUseCase(partners, dates, ideas, tasks, attentions, coupleProfile, wishes, duo, clock)()

            assertThat(json).contains("\"exportedAt\": \"2027-01-20T09:00:00Z\"")
            assertThat(json).contains("\"firstName\": \"Camille\"")
            assertThat(json).contains("\"title\": \"Voyage\"")
            assertThat(json).contains("\"text\": \"Un livre\"")
            assertThat(json).contains("\"title\": \"Courses\"")
            assertThat(json).contains("\"attentionId\": 3")
            assertThat(json).contains("\"kids\": \"NONE\"")
            assertThat(json).contains("\"text\": \"Un vélo\",\n      \"createdAt\": \"2027-01-12T08:00:00Z\",\n      \"author\": \"me\"")
            assertThat(
                json,
            ).contains("\"text\": \"Un album\",\n      \"createdAt\": \"2027-01-15T08:00:00Z\",\n      \"author\": \"partner\"")
        }

    @Test
    fun `delete all empties every repository and cancels the reminders`() =
        runTest {
            DeleteAllUserDataUseCase(partners, dates, ideas, tasks, attentions, coupleProfile, scheduler, duo)()

            assertThat(partners.state.value).isNull()
            assertThat(dates.state.value).isEmpty()
            assertThat(ideas.state.value).isEmpty()
            assertThat(tasks.state.value).isEmpty()
            assertThat(attentions.state.value).isEmpty()
            assertThat(coupleProfile.state.value).isEqualTo(CoupleProfile())
            assertThat(duo.state.value).isNull()
            assertThat(scheduler.calls).containsExactly(emptyList<PlannedReminder>())
        }
}
