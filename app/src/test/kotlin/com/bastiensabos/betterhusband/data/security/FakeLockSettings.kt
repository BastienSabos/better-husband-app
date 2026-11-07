package com.bastiensabos.betterhusband.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLockSettings(
    initial: Boolean = false,
) : LockSettings {
    private val state = MutableStateFlow(initial)

    override val enabled: StateFlow<Boolean> = state

    override fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
