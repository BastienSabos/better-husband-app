package com.bastiensabos.betterhusband.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bastiensabos.betterhusband.R
import com.bastiensabos.betterhusband.data.repository.AttentionBank
import com.bastiensabos.betterhusband.data.repository.AttentionLogRepository
import com.bastiensabos.betterhusband.data.repository.CoupleProfileRepository
import com.bastiensabos.betterhusband.data.repository.HouseholdTaskRepository
import com.bastiensabos.betterhusband.data.repository.IdeaRepository
import com.bastiensabos.betterhusband.data.repository.PartnerRepository
import com.bastiensabos.betterhusband.domain.AttentionCatalog
import com.bastiensabos.betterhusband.domain.AttentionContext
import com.bastiensabos.betterhusband.domain.AttentionGroup
import com.bastiensabos.betterhusband.domain.AttentionPicker
import com.bastiensabos.betterhusband.domain.IdeaSuggestions
import com.bastiensabos.betterhusband.domain.MomentCalendar
import com.bastiensabos.betterhusband.domain.ObserveUpcomingDatesUseCase
import com.bastiensabos.betterhusband.domain.StreakMath
import com.bastiensabos.betterhusband.domain.TaskMath
import com.bastiensabos.betterhusband.domain.model.AttentionLogEntry
import com.bastiensabos.betterhusband.domain.model.CoupleProfile
import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.Idea
import com.bastiensabos.betterhusband.domain.model.Partner
import com.bastiensabos.betterhusband.ui.component.UndoNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Flux du jour : une attention suggérée, les tâches du foyer à faire, la prochaine date à préparer (avec les idées
 * du carnet qui peuvent servir), et la série.
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
        private val ideaRepository: IdeaRepository,
        private val coupleProfile: CoupleProfileRepository,
        private val observeUpcomingDates: ObserveUpcomingDatesUseCase,
        private val bank: AttentionBank,
        private val clock: Clock,
    ) : ViewModel() {
        /** Attentions déjà écartées aujourd'hui avec « Autre idée » : on ne les reproposera pas. */
        private val swapped = MutableStateFlow<Set<Int>>(emptySet())

        /**
         * Le jour affiché. Il ne change que par [refreshDay] (retour au premier plan, minuit, une action) : tout l'écran
         * se recalcule alors pour ce jour, et l'attention du nouveau jour est tirée.
         */
        private val day = MutableStateFlow(LocalDate.now(clock))

        private val undo = MutableStateFlow<UndoNotice?>(null)

        /** La tâche telle qu'elle était avant d'être cochée, tant que « Annuler » est proposé. */
        private var checkedTask: HouseholdTask? = null

        init {
            viewModelScope.launch {
                day.collectLatest { today ->
                    val log = attentionLog.observeAll().first()
                    if (log.none { it.date == today }) {
                        swapped.value = emptySet()
                        attentionLog.save(AttentionLogEntry(today, pickFor(today, log, emptySet())))
                    }
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<TodayUiState> =
            combine(
                day,
                attentionLog.observeAll(),
                taskRepository.observeAll(),
                // Les « dans N jours » se recalculent quand le jour change.
                day.flatMapLatest { observeUpcomingDates() },
                combine(
                    coupleProfile.observe(),
                    partnerRepository.observePartner(),
                    ideaRepository.observeAll(),
                    undo,
                ) { profile, partner, ideas, undoNotice -> Extras(profile, partner, ideas, undoNotice) },
            ) { today, log, tasks, dates, (profile, partner, ideas, undoNotice) ->
                val entry = log.lastOrNull { it.date == today }
                if (entry == null) {
                    TodayUiState.Loading
                } else {
                    val doneDays = log.filter { it.done }.map { it.date }.toSet()
                    val nextDate = dates.firstOrNull { it.daysUntil <= NEXT_DATE_WINDOW_DAYS }
                    val due = TaskMath.sorted(tasks.filter { TaskMath.isDue(it, today) }, today)
                    TodayUiState.Ready(
                        date = today,
                        attention = AttentionCard(bank.text(entry.attentionId), entry.done),
                        tasks = due.take(MAX_TASKS).map { TodayTask(it.id, it.title, TaskMath.daysOverdue(it, today)) },
                        moreTasks = (due.size - MAX_TASKS).coerceAtLeast(0),
                        nextDate = nextDate,
                        nextDateIdeas = nextDate?.let { IdeaSuggestions.forDate(ideas, it.kind).map { idea -> idea.text } }.orEmpty(),
                        streak = StreakMath.current(doneDays, today),
                        week = StreakMath.week(doneDays, today),
                        suggestCoupleProfile = profile == CoupleProfile(),
                        partnerName = partner?.firstName,
                        undo = undoNotice,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayUiState.Loading)

        /** À appeler au retour au premier plan et à minuit : passe au nouveau jour s'il a changé. */
        fun refreshDay() {
            day.value = LocalDate.now(clock)
        }

        /** Millisecondes jusqu'au prochain minuit, pour que l'écran resté ouvert passe au jour suivant. */
        fun millisUntilNextDay(): Long {
            val now = ZonedDateTime.now(clock)
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            return Duration.between(now, midnight).toMillis().coerceAtLeast(1)
        }

        fun onAttentionDone(done: Boolean) {
            refreshDay()
            viewModelScope.launch { attentionLog.setDone(day.value, done) }
        }

        /** « Autre idée » : remplace l'attention du jour par une autre, tant qu'elle n'est pas faite. */
        fun onOtherAttention() {
            refreshDay()
            viewModelScope.launch {
                val today = day.value
                val log = attentionLog.observeAll().first()
                val current = log.lastOrNull { it.date == today } ?: return@launch
                if (current.done) return@launch
                val skipped = swapped.value + current.attentionId
                swapped.value = skipped
                attentionLog.save(AttentionLogEntry(today, pickFor(today, log, skipped)))
            }
        }

        fun onTaskDone(id: Long) {
            refreshDay()
            viewModelScope.launch {
                checkedTask = taskRepository.observeAll().first().firstOrNull { it.id == id }
                taskRepository.setLastDone(id, day.value)
                undo.value = UndoNotice(R.string.today_task_done, id)
            }
        }

        /** Décoche la tâche : elle retrouve sa dernière date faite, donc revient dans la liste. */
        fun onUndo() {
            val task = checkedTask ?: return
            checkedTask = null
            undo.value = null
            viewModelScope.launch { taskRepository.save(task) }
        }

        fun onUndoDismissed() {
            checkedTask = null
            undo.value = null
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

        private data class Extras(
            val profile: CoupleProfile,
            val partner: Partner?,
            val ideas: List<Idea>,
            val undo: UndoNotice?,
        )

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_TASKS = 3
            const val NEXT_DATE_WINDOW_DAYS = 30L
        }
    }
