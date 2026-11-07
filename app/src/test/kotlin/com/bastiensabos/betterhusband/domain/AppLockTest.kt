package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.data.security.FakeLockSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AppLockTest {
    private val settings = FakeLockSettings()
    private var now = Instant.parse("2027-01-20T10:00:00Z")
    private val clock =
        object : Clock() {
            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: java.time.ZoneId?) = this

            override fun instant(): Instant = now
        }

    private fun lock() = AppLock(settings, clock)

    @Test
    fun `starts unlocked when the lock is off`() {
        assertThat(lock().locked.value).isFalse()
    }

    @Test
    fun `starts locked when the lock is on`() {
        settings.setEnabled(true)

        assertThat(lock().locked.value).isTrue()
    }

    @Test
    fun `unlock opens the app`() {
        settings.setEnabled(true)
        val lock = lock()

        lock.unlock()

        assertThat(lock.locked.value).isFalse()
    }

    @Test
    fun `a short trip to the background keeps the app open`() {
        settings.setEnabled(true)
        val lock = lock().also { it.unlock() }

        lock.onBackground()
        now = now.plusSeconds(20)
        lock.onForeground()

        assertThat(lock.locked.value).isFalse()
    }

    @Test
    fun `a long trip to the background relocks`() {
        settings.setEnabled(true)
        val lock = lock().also { it.unlock() }

        lock.onBackground()
        now = now.plus(LockPolicy.DEFAULT_GRACE).plus(Duration.ofSeconds(1))
        lock.onForeground()

        assertThat(lock.locked.value).isTrue()
    }

    @Test
    fun `never relocks when the lock is off`() {
        val lock = lock()

        lock.onBackground()
        now = now.plusSeconds(3600)
        lock.onForeground()

        assertThat(lock.locked.value).isFalse()
    }
}
