package com.bastiensabos.betterhusband.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

class LockPolicyTest {
    private val left = Instant.parse("2027-01-20T10:00:00Z")

    @Test
    fun `never left the screen means no relock`() {
        assertThat(LockPolicy.shouldRelock(null, left)).isFalse()
    }

    @Test
    fun `back within the grace period stays unlocked`() {
        assertThat(LockPolicy.shouldRelock(left, left.plusSeconds(59))).isFalse()
    }

    @Test
    fun `back after the grace period relocks`() {
        assertThat(LockPolicy.shouldRelock(left, left.plus(LockPolicy.DEFAULT_GRACE))).isTrue()
        assertThat(LockPolicy.shouldRelock(left, left.plusSeconds(3600))).isTrue()
    }

    @Test
    fun `a clock moved backwards relocks`() {
        assertThat(LockPolicy.shouldRelock(left, left.minusSeconds(5))).isTrue()
    }

    @Test
    fun `a custom grace period is honoured`() {
        assertThat(LockPolicy.shouldRelock(left, left.plusSeconds(5), Duration.ofSeconds(10))).isFalse()
        assertThat(LockPolicy.shouldRelock(left, left.plusSeconds(10), Duration.ofSeconds(10))).isTrue()
    }
}
