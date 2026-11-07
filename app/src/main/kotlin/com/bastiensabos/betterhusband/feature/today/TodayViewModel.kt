package com.bastiensabos.betterhusband.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.data.repository.AttentionBank
import com.bastiensabos.betterhusband.data.repository.AttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.CoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.HouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.AttentionCatalog
import com.bastiensabos.betterhusband.domain.AttentionContext
import com.bastiensabos.betterhusband.domain.AttentionGroup
import com.bastiensabos.betterhusband.domain.AttentionPicker
import com.bastiensabos.betterhusband.domain.MomentCalendar
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.StreakMath
import com.bastiensabos.betterhusband.domain.TaskMath
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/**
 * Flux du jour : une attention suggérée, les tâches du foyer à faire, la prochaine date à préparer, et la série.
 * L'attention du jour est choisie une fois (au premier affichage de la journée) puis conservée dans l'historique.
 * Autour d'un temps fort (anniversaire, Saint-Valentin, Noël…), elle vient des idées de ce temps fort.
 */
@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        private val attentionLog: AttentionLogRepository,
        private val taskRepository: HouseholdTaskRepository,
        private val partnerRepository: PartnerRepository,
        private val coupleProfile: CoupleProfileRepository,
        private val observeUpcomingDates: ObserveUpcomingDatesUseCase,
        private val bank: AttentionBank,
        private val clock: Clock,
    ) : ViewModel() {
        /** Attentions déjà écartées aujourd'hui avec « Autre idée » : on ne les reproposera pas. */
        private val swapped = MutableStateFlow<Set<Int>>(emptySet())

        init {
            viewModelScope.launch {
                val today = LocalDate.now(clock)
                val log = attentionLog.observeAll().first()
                if (log.none { it.date == today }) {
                    attentionLog.save(AttentionLogEntry(today, pickFor(today, log, emptySet())))
                }
            }
        }

        val uiState: StateFlow<TodayUiState> =
            combine(
                attentionLog.observeAll(),
                taskRepository.observeAll(),
                observeUpcomingDates(),
                coupleProfile.observe(),
            ) { log, tasks, dates, profile ->
                val today = LocalDate.now(clock)
                val entry = log.lastOrNull { it.date == today }
                if (entry == null) {
                    TodayUiState.Loading
                } else {
                    val due = TaskMath.sorted(tasks.filter { TaskMath.isDue(it, today) }, today)
                    TodayUiState.Ready(
                        attention = AttentionCard(bank.text(entry.attentionId), entry.done),
                        tasks = due.take(MAX_TASKS).map { TodayTask(it.id, it.title, TaskMath.daysOverdue(it, today)) },
                        moreTasks = (due.size - MAX_TASKS).coerceAtLeast(0),
                        nextDate = dates.firstOrNull { it.daysUntil <= NEXT_DATE_WINDOW_DAYS },
                        streak = StreakMath.current(log.filter { it.done }.map { it.date }.toSet(), today),
                        suggestCoupleProfile = profile == CoupleProfile(),
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayUiState.Loading)

        fun onAttentionDone(done: Boolean) {
            viewModelScope.launch { attentionLog.setDone(LocalDate.now(clock), done) }
        }

        /** « Autre idée » : remplace l'attention du jour par une autre, tant qu'elle n'est pas faite. */
        fun onOtherAttention() {
            viewModelScope.launch {
                val today = LocalDate.now(clock)
                val log = attentionLog.observeAll().first()
                val current = log.lastOrNull { it.date == today } ?: return@launch
                if (current.done) return@launch
                val skipped = swapped.value + current.attentionId
                swapped.value = skipped
                attentionLog.save(AttentionLogEntry(today, pickFor(today, log, skipped)))
            }
        }

        fun onTaskDone(id: Long) {
            viewModelScope.launch { taskRepository.setLastDone(id, LocalDate.now(clock)) }
        }

        private suspend fun pickFor(
            today: LocalDate,
            log: List<AttentionLogEntry>,
            skipped: Set<Int>,
        ): Int {
            val recent =
                log
                    .filter { it.date < today && it.date >= today.minusDays(AttentionPicker.NO_REPEAT_DAYS.toLong()) }
                    .map { it.attentionId }
            val profile = coupleProfile.observe().first()
            val moments = MomentCalendar.priorityGroups(observeUpcomingDates().first(), profile.kids, today)
            // Un temps fort passe devant le tirage ; une fois toutes ses idées écartées, on revient au tirage habituel.
            val pool =
                MomentCalendar
                    .priorityIds(bank.groupSizes, moments)
                    .filterNot { it in skipped }
                    .ifEmpty { eligibleIds(today, profile) }
            return AttentionPicker.pick(today, pool, recent, skipped)
        }

        /** Les attentions qui conviennent aujourd'hui ; si aucune ne convient, toute la banque commune. */
        private suspend fun eligibleIds(
            today: LocalDate,
            profile: CoupleProfile,
        ): List<Int> {
            val partner = partnerRepository.observePartner().first()
            val years = partner?.relationshipStart?.let { Period.between(it, today).years }
            val context = AttentionContext(profile = profile, relationshipYears = years, month = today.month)
            val eligible = AttentionCatalog.eligibleIds(bank.groupSizes, context)
            return eligible.ifEmpty { (0 until (bank.groupSizes[AttentionGroup.EVERYONE] ?: 0)).toList() }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_TASKS = 3
            const val NEXT_DATE_WINDOW_DAYS = 30L
        }
    }
