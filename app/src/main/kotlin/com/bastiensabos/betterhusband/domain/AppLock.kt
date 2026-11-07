package com.bastiensabos.betterhusband.domain

import com.bastiensabos.betterhusband.data.security.LockSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * État verrouillé / déverrouillé de l'appli. Verrouillée au lancement si le réglage est actif, puis après
 * [LockPolicy.DEFAULT_GRACE] passé en arrière-plan.
 */
@Singleton
class AppLock
    @Inject
    constructor(
        private val settings: LockSettings,
        private val clock: Clock,
    ) {
        private val _locked = MutableStateFlow(settings.enabled.value)
        val locked: StateFlow<Boolean> = _locked.asStateFlow()

        private var leftAt: Instant? = null

        fun onBackground() {
            leftAt = Instant.now(clock)
        }

        fun onForeground() {
            if (settings.enabled.value && LockPolicy.shouldRelock(leftAt, Instant.now(clock))) {
                _locked.value = true
            }
            leftAt = null
        }

        fun unlock() {
            _locked.value = false
        }
    }
