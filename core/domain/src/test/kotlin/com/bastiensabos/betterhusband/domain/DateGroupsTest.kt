package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.domain.model.DateKind
import com.bastiensabos.betterhusband.domain.model.DateLabel
import com.bastiensabos.betterhusband.domain.model.UpcomingDate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateGroupsTest {
    private fun upcoming(
        key: String,
        date: LocalDate,
    ) = UpcomingDate(key, DateLabel.Custom(key), DateKind.OTHER, date, 0, null, null)

    @Test
    fun `dates are grouped by month in calendar order, across the new year`() {
        val groups =
            DateGroups.byMonth(
                listOf(
                    upcoming("réveillon", LocalDate.of(2026, 12, 24)),
                    upcoming("repas", LocalDate.of(2026, 12, 6)),
                    upcoming("léon", LocalDate.of(2027, 1, 3)),
                ),
            )

        assertThat(groups.map { it.month }).containsExactly(YearMonth.of(2026, 12), YearMonth.of(2027, 1)).inOrder()
        assertThat(groups.first().dates.map { it.key }).containsExactly("repas", "réveillon").inOrder()
    }

    @Test
    fun `no date gives no group`() {
        assertThat(DateGroups.byMonth(emptyList())).isEmpty()
    }
}
