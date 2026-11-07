package com.bastiensabos.betterhusband.feature.today

import com.bastiensabos.betterhusband.data.repository.FakeAttentionBank
import com.bastiensabos.betterhusband.data.repository.FakeAttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.FakeCoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.FakeHouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.FakeImportantDateRepository
import com.bastiensabos.betterhusband.data.repository.FakePartnerRepository
import com.bastiensabos.betterhusband.domain.AttentionGroup
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.ImportantDate
import com.bastiensabos.betterhusband.domain.model.KidsAtHome
import com.bastiensabos.betterhusband.domain.model.LivingArrangement
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import com.bastiensabos.betterhusband.testutil.MainDispatcherRule
import com.bastiensabos.betterhusband.testutil.MutableClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TodayViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val today = LocalDate.of(2027, 1, 20)
    private val clock = Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneOffset.UTC)
    private val log = FakeAttentionLogRepository()
    private val tasks = FakeHouseholdTaskRepository()
    private val dates = FakeImportantDateRepository()
    private val coupleProfile = FakeCoupleProfileRepository()

    private fun viewModel(
        bank: FakeAttentionBank = FakeAttentionBank(),
        clock: Clock = this.clock,
    ) = TodayViewModel(
        attentionLog = log,
        taskRepository = tasks,
        partnerRepository = FakePartnerRepository(),
        coupleProfile = coupleProfile,
        observeUpcomingDates = ObserveUpcomingDatesUseCase(FakePartnerRepository(), dates, clock),
        bank = bank,
        clock = clock,
    )

    /** Garde le flux actif (WhileSubscribed) pendant le test. */
    private fun TestScope.collectState(viewModel: TodayViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun TodayViewModel.ready() = uiState.value as TodayUiState.Ready

    @Test
    fun `first open of the day chooses and keeps an attention`() =
        runTest {
            val viewModel = viewModel()
            collectState(viewModel)

            val entry = log.state.value.single()
            assertThat(entry.date).isEqualTo(today)
            assertThat(entry.done).isFalse()
            assertThat(viewModel.ready().attention.text).isEqualTo("Attention ${entry.attentionId}")
        }

    @Test
    fun `reopening the same day keeps the same attention`() =
        runTest {
            log.save(AttentionLogEntry(today, 42))
            val viewModel = viewModel()
            collectState(viewModel)

            assertThat(log.state.value).containsExactly(AttentionLogEntry(today, 42))
            assertThat(viewModel.ready().attention.text).isEqualTo("Attention 42")
        }

    @Test
    fun `an attention used in the last days is not proposed again`() =
        runTest {
            val first = AttentionLogEntry(today.minusDays(1), 0)
            log.save(first)
            val viewModel = viewModel()
            collectState(viewModel)

            val chosen = log.state.value.single { it.date == today }
            assertThat(chosen.attentionId).isNotEqualTo(first.attentionId)
        }

    @Test
    fun `marking the attention done updates the card and the streak, and can be undone`() =
        runTest {
            log.save(AttentionLogEntry(today.minusDays(1), 1, done = true))
            log.save(AttentionLogEntry(today, 2))
            val viewModel = viewModel()
            collectState(viewModel)
            assertThat(viewModel.ready().streak).isEqualTo(1)

            viewModel.onAttentionDone(true)
            advanceUntilIdle()
            assertThat(viewModel.ready().attention.done).isTrue()
            assertThat(viewModel.ready().streak).isEqualTo(2)

            viewModel.onAttentionDone(false)
            advanceUntilIdle()
            assertThat(viewModel.ready().attention.done).isFalse()
            assertThat(viewModel.ready().streak).isEqualTo(1)
        }

    @Test
    fun `other idea replaces the attention, never repeats one already swapped and is refused once done`() =
        runTest {
            val viewModel = viewModel()
            collectState(viewModel)
            val seen =
                mutableSetOf(
                    log.state.value
                        .single()
                        .attentionId,
                )

            repeat(3) {
                viewModel.onOtherAttention()
                advanceUntilIdle()
                assertThat(
                    seen.add(
                        log.state.value
                            .single()
                            .attentionId,
                    ),
                ).isTrue()
            }

            viewModel.onAttentionDone(true)
            advanceUntilIdle()
            val kept =
                log.state.value
                    .single()
                    .attentionId
            viewModel.onOtherAttention()
            advanceUntilIdle()
            assertThat(
                log.state.value
                    .single()
                    .attentionId,
            ).isEqualTo(kept)
            assertThat(viewModel.ready().attention.canSwap).isFalse()
        }

    @Test
    fun `due tasks are listed most overdue first, capped at three, with the rest counted`() =
        runTest {
            val created = today.minusDays(40)
            listOf("A" to 5L, "B" to 4L, "C" to 3L, "D" to 2L).forEach { (title, overdue) ->
                tasks.save(
                    HouseholdTask(
                        title = title,
                        repeat = TaskRepeat.WEEKLY,
                        createdOn = created,
                        lastDoneOn = today.minusDays(7 + overdue),
                    ),
                )
            }
            tasks.save(HouseholdTask(title = "Faite", repeat = TaskRepeat.WEEKLY, createdOn = created, lastDoneOn = today))
            val viewModel = viewModel()
            collectState(viewModel)

            val ready = viewModel.ready()
            assertThat(ready.tasks.map { it.title }).containsExactly("A", "B", "C").inOrder()
            assertThat(ready.tasks.first().overdueDays).isEqualTo(5)
            assertThat(ready.moreTasks).isEqualTo(1)
        }

    @Test
    fun `checking a task marks it done today and removes it from the list`() =
        runTest {
            val id = tasks.save(HouseholdTask(title = "Linge", repeat = TaskRepeat.WEEKLY, createdOn = today.minusDays(3)))
            val viewModel = viewModel()
            collectState(viewModel)
            assertThat(viewModel.ready().tasks).hasSize(1)

            viewModel.onTaskDone(id)
            advanceUntilIdle()

            assertThat(
                tasks.state.value
                    .single()
                    .lastDoneOn,
            ).isEqualTo(today)
            assertThat(viewModel.ready().tasks).isEmpty()
        }

    @Test
    fun `next date is shown only within thirty days`() =
        runTest {
            dates.save(ImportantDate(title = "Loin", kind = DateKind.OTHER, date = today.plusDays(90), recurring = false))
            val viewModel = viewModel()
            collectState(viewModel)
            assertThat(viewModel.ready().nextDate).isNull()

            dates.save(ImportantDate(title = "Bientôt", kind = DateKind.OTHER, date = today.plusDays(10), recurring = false))
            advanceUntilIdle()

            assertThat(viewModel.ready().nextDate?.daysUntil).isEqualTo(10)
        }

    @Test
    fun `an empty couple profile invites to fill it`() =
        runTest {
            val viewModel = viewModel()
            collectState(viewModel)

            assertThat(viewModel.ready().suggestCoupleProfile).isTrue()

            coupleProfile.save(CoupleProfile(kids = KidsAtHome.NONE))
            advanceUntilIdle()

            assertThat(viewModel.ready().suggestCoupleProfile).isFalse()
        }

    @Test
    fun `the attention comes from a group that matches the profile`() =
        runTest {
            coupleProfile.save(CoupleProfile(living = LivingArrangement.LONG_DISTANCE))
            val bank =
                FakeAttentionBank(
                    mapOf(AttentionGroup.EVERYONE to 0, AttentionGroup.LONG_DISTANCE to 3, AttentionGroup.TOGETHER to 3),
                )
            val viewModel = viewModel(bank)
            collectState(viewModel)

            assertThat(
                AttentionGroup.of(
                    log.state.value
                        .single()
                        .attentionId,
                ),
            ).isEqualTo(AttentionGroup.LONG_DISTANCE)
        }

    @Test
    fun `a moment day takes over the daily draw until its ideas are all swapped`() =
        runTest {
            dates.save(ImportantDate(title = "Mariage", kind = DateKind.ANNIVERSARY, date = today.minusYears(5), recurring = true))
            val bank = FakeAttentionBank(mapOf(AttentionGroup.EVERYONE to 5, AttentionGroup.ANNIVERSARY_DAY to 1))
            val viewModel = viewModel(bank)
            collectState(viewModel)

            assertThat(
                AttentionGroup.of(
                    log.state.value
                        .single()
                        .attentionId,
                ),
            ).isEqualTo(AttentionGroup.ANNIVERSARY_DAY)

            viewModel.onOtherAttention()
            advanceUntilIdle()

            assertThat(
                AttentionGroup.of(
                    log.state.value
                        .single()
                        .attentionId,
                ),
            ).isEqualTo(AttentionGroup.EVERYONE)
        }

    @Test
    fun `after midnight the screen moves to the new day instead of spinning`() =
        runTest {
            val clock = MutableClock(Instant.parse("2027-01-20T23:30:00Z"))
            tasks.save(
                HouseholdTask(
                    title = "Sortir les poubelles",
                    repeat = TaskRepeat.DAILY,
                    createdOn = today.minusDays(3),
                    lastDoneOn = today.minusDays(1),
                ),
            )
            val viewModel = viewModel(clock = clock)
            collectState(viewModel)
            assertThat(viewModel.ready().tasks).hasSize(1)

            clock.instant = Instant.parse("2027-01-21T00:05:00Z")
            // Une action après minuit, sans retour au premier plan : l'accueil doit suivre.
            viewModel.onTaskDone(
                viewModel
                    .ready()
                    .tasks
                    .single()
                    .id,
            )
            advanceUntilIdle()

            assertThat(log.state.value.map { it.date }).containsExactly(today, today.plusDays(1))
            assertThat(viewModel.uiState.value).isInstanceOf(TodayUiState.Ready::class.java)
        }

    @Test
    fun `millis until next day counts down to midnight`() {
        val clock = MutableClock(Instant.parse("2027-01-20T23:59:00Z"))

        assertThat(viewModel(clock = clock).millisUntilNextDay()).isEqualTo(60_000L)
    }
}
