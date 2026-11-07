package com.bastiensabos.betterhusband.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DateMathTest {
    private val today = LocalDate.of(2027, 3, 10)

    @Test
    fun `recurring date later this year stays in this year`() {
        assertThat(DateMath.nextOccurrence(LocalDate.of(1990, 5, 17), true, today)).isEqualTo(LocalDate.of(2027, 5, 17))
    }

    @Test
    fun `recurring date already passed moves to next year`() {
        assertThat(DateMath.nextOccurrence(LocalDate.of(1990, 1, 5), true, today)).isEqualTo(LocalDate.of(2028, 1, 5))
    }

    @Test
    fun `date today counts as today`() {
        assertThat(DateMath.nextOccurrence(LocalDate.of(1990, 3, 10), true, today)).isEqualTo(today)
    }

    @Test
    fun `february 29 falls on february 28 in non leap years`() {
        val born = LocalDate.of(2000, 2, 29)

        assertThat(DateMath.nextOccurrence(born, true, LocalDate.of(2027, 1, 1))).isEqualTo(LocalDate.of(2027, 2, 28))
        assertThat(DateMath.nextOccurrence(born, true, LocalDate.of(2028, 1, 1))).isEqualTo(LocalDate.of(2028, 2, 29))
    }

    @Test
    fun `one-off event in the future is kept and in the past is dropped`() {
        assertThat(DateMath.nextOccurrence(LocalDate.of(2027, 3, 11), false, today)).isEqualTo(LocalDate.of(2027, 3, 11))
        assertThat(DateMath.nextOccurrence(LocalDate.of(2027, 3, 9), false, today)).isNull()
    }

    @Test
    fun `year end rolls over`() {
        assertThat(DateMath.nextOccurrence(LocalDate.of(1990, 1, 2), true, LocalDate.of(2027, 12, 31)))
            .isEqualTo(LocalDate.of(2028, 1, 2))
    }
}
