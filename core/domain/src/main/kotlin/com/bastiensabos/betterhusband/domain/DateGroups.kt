package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import java.time.YearMonth

/** Les dates d'un même mois, pour une liste qui se lit comme un agenda. */
data class MonthGroup(
    val month: YearMonth,
    val dates: List<UpcomingDate>,
)

object DateGroups {
    /** Regroupe par mois, dans l'ordre chronologique ; [dates] sont supposées déjà triées par échéance. */
    fun byMonth(dates: List<UpcomingDate>): List<MonthGroup> =
        dates
            .sortedBy { it.date }
            .groupBy { YearMonth.from(it.date) }
            .map { (month, inMonth) -> MonthGroup(month, inMonth) }
}
