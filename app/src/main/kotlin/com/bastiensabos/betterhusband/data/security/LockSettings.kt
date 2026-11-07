package com.bastiensabos.betterhusband.data.security

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Le verrouillage est-il activé ? Lu avant l'ouverture de la base, donc gardé hors de Room : un simple drapeau,
 * sans donnée personnelle (la sauvegarde cloud est de toute façon coupée par `allowBackup=false`).
 */
interface LockSettings {
    val enabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
}

@Singleton
class SharedPreferencesLockSettings
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : LockSettings {
        private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        private val state = MutableStateFlow(preferences.getBoolean(KEY_ENABLED, false))

        override val enabled: StateFlow<Boolean> = state.asStateFlow()

        override fun setEnabled(enabled: Boolean) {
            preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
            state.value = enabled
        }

        private companion object {
            const val FILE = "lock_settings"
            const val KEY_ENABLED = "enabled"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class LockSettingsModule {
    @Binds
    abstract fun bindLockSettings(impl: SharedPreferencesLockSettings): LockSettings
}
