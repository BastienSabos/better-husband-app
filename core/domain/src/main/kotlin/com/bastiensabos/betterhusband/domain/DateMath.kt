package com.bastiensabos.betterhusband.domain

import java.time.LocalDate
import java.time.Year

/** Calculs de dates purs et testables. */
object DateMath {
    /**
     * Prochaine occurrence à partir d'[today] (aujourd'hui compris). Pour une date annuelle, le 29 février
     * tombe le 28 février les années non bissextiles. Un événement ponctuel passé n'a plus d'occurrence (`null`).
     */
    fun nextOccurrence(
        date: LocalDate,
        recurring: Boolean,
        today: LocalDate,
    ): LocalDate? {
        if (!recurring) return date.takeIf { !it.isBefore(today) }
        val thisYear = anniversaryIn(date, today.year)
        return if (thisYear.isBefore(today)) anniversaryIn(date, today.year + 1) else thisYear
    }

    private fun anniversaryIn(
        date: LocalDate,
        year: Int,
    ): LocalDate =
        if (date.monthValue == 2 && date.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            date.withYear(year)
        }
}
