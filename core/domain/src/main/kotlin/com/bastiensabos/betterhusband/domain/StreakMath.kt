package com.bastiensabos.betterhusband.domain

import java.time.LocalDate

/** Série de jours consécutifs où l'utilisateur a fait son attention du jour. Valorise sans points ni classement. */
object StreakMath {
    /**
     * Longueur de la série en cours. Elle ne se brise qu'à la fin de la journée : tant qu'[today] n'est pas fait, on
     * compte à partir d'hier. Une série qui ne touche ni aujourd'hui ni hier est terminée (0).
     */
    fun current(
        doneDays: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        var day = if (today in doneDays) today else today.minusDays(1)
        var length = 0
        while (day in doneDays) {
            length++
            day = day.minusDays(1)
        }
        return length
    }

    /** Plus longue série jamais atteinte. */
    fun best(doneDays: Set<LocalDate>): Int {
        var best = 0
        var length = 0
        var previous: LocalDate? = null
        for (day in doneDays.sorted()) {
            length = if (previous != null && day == previous.plusDays(1)) length + 1 else 1
            best = maxOf(best, length)
            previous = day
        }
        return best
    }
}
