package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.HouseholdTask
import com.bastiensabos.betterhusband.domain.model.TaskRepeat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Échéances des tâches du foyer : calculs purs et testables. */
object TaskMath {
    /** Jour où la tâche est à faire : dès sa création si jamais faite, sinon un rythme après la dernière fois. */
    fun nextDue(task: HouseholdTask): LocalDate {
        val last = task.lastDoneOn ?: return task.createdOn
        return when (task.repeat) {
            TaskRepeat.DAILY -> last.plusDays(1)
            TaskRepeat.WEEKLY -> last.plusWeeks(1)
            TaskRepeat.BIWEEKLY -> last.plusWeeks(2)
            TaskRepeat.MONTHLY -> last.plusMonths(1)
        }
    }

    fun isDue(
        task: HouseholdTask,
        today: LocalDate,
    ): Boolean = !nextDue(task).isAfter(today)

    /** Jours de retard : 0 le jour même, négatif si la tâche n'est pas encore à faire. */
    fun daysOverdue(
        task: HouseholdTask,
        today: LocalDate,
    ): Long = ChronoUnit.DAYS.between(nextDue(task), today)

    /** À faire d'abord (la plus en retard en tête), puis les prochaines par échéance ; à égalité, par titre. */
    fun sorted(
        tasks: List<HouseholdTask>,
        today: LocalDate,
    ): List<HouseholdTask> =
        tasks.sortedWith(
            compareBy<HouseholdTask> { !isDue(it, today) }
                .thenBy { nextDue(it) }
                .thenBy { it.title.lowercase() },
        )
}
