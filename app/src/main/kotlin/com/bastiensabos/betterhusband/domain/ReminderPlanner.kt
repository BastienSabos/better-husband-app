package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/** Un rappel à déclencher. Aucune donnée personnelle ici : le texte est relu dans la base chiffrée au moment de l'envoi. */
data class PlannedReminder(
    val dateKey: String,
    val offsetDays: Int,
    val triggerAt: ZonedDateTime,
) {
    val workName: String get() = "reminder-$dateKey-j$offsetDays"
}

/**
 * Calcule les rappels J-14, J-3 et jour J à 9 h (heure locale). On ne planifie que les 45 prochains jours : une
 * synchro quotidienne étend l'horizon, ce qui évite d'empiler des travaux pour toute l'année.
 */
class ReminderPlanner
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        fun plan(upcoming: List<UpcomingDate>): List<PlannedReminder> {
            val now = ZonedDateTime.now(clock)
            val horizon = now.plusDays(HORIZON_DAYS)
            return upcoming
                .flatMap { item ->
                    REMINDER_OFFSETS_DAYS.map { offset ->
                        PlannedReminder(
                            dateKey = item.key,
                            offsetDays = offset,
                            triggerAt =
                                item.date
                                    .minusDays(offset.toLong())
                                    .atTime(REMINDER_HOUR, 0)
                                    .atZone(clock.zone),
                        )
                    }
                }.filter { it.triggerAt.isAfter(now) && it.triggerAt.isBefore(horizon) }
                .sortedBy { it.triggerAt }
        }

        companion object {
            val REMINDER_OFFSETS_DAYS = listOf(14, 3, 0)
            const val REMINDER_HOUR = 9
            const val HORIZON_DAYS = 45L

            /** Au déclenchement, retrouve la date visée ; `null` si elle a changé depuis (le rappel est alors ignoré). */
            fun selectDue(
                upcoming: List<UpcomingDate>,
                dateKey: String,
                offsetDays: Int,
            ): UpcomingDate? = upcoming.firstOrNull { it.key == dateKey && it.daysUntil == offsetDays.toLong() }
        }
    }
