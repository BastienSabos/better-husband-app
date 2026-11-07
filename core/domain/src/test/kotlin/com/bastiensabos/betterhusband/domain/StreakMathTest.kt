package com.bastiensabos.betterhusband.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class StreakMathTest {
    private val today = LocalDate.of(2027, 1, 20)

    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `no day done gives no streak`() {
        assertThat(StreakMath.current(emptySet(), today)).isEqualTo(0)
        assertThat(StreakMath.best(emptySet())).isEqualTo(0)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        assertThat(StreakMath.current(days(0, 1, 2), today)).isEqualTo(3)
    }

    @Test
    fun `streak is still alive when today is not done yet`() {
        assertThat(StreakMath.current(days(1, 2, 3, 4), today)).isEqualTo(4)
    }

    @Test
    fun `streak is over when neither today nor yesterday is done`() {
        assertThat(StreakMath.current(days(2, 3, 4), today)).isEqualTo(0)
    }

    @Test
    fun `a gap stops the count`() {
        assertThat(StreakMath.current(days(0, 1, 3, 4, 5), today)).isEqualTo(2)
    }

    @Test
    fun `best streak is the longest run ever`() {
        assertThat(StreakMath.best(days(0, 1, 5, 6, 7, 8, 12))).isEqualTo(4)
        assertThat(StreakMath.best(days(3))).isEqualTo(1)
    }
}
