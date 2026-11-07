package com.bastiensabos.betterhusband

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.data.security.BiometricAuthenticator
import com.bastiensabos.betterhusband.data.security.LockSettings
import com.bastiensabos.betterhusband.domain.AppLock
import com.bastiensabos.betterhusband.feature.lock.LockRoute
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingRoute
import com.bastiensabos.betterhusband.ui.navigation.AppShell
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FragmentActivity (et non ComponentActivity) : BiometricPrompt l'exige.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Inject lateinit var appLock: AppLock

    @Inject lateinit var lockSettings: LockSettings

    @Inject lateinit var authenticator: BiometricAuthenticator

    override fun onStart() {
        super.onStart()
        appLock.onForeground()
    }

    override fun onStop() {
        appLock.onBackground()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterHusbandTheme {
                val state by appViewModel.uiState.collectAsStateWithLifecycle()
                val locked by appLock.locked.collectAsStateWithLifecycle()
                val lockEnabled by lockSettings.enabled.collectAsStateWithLifecycle()
                // Verrouillage actif : l'aperçu des applis récentes et les captures d'écran restent noirs.
                LaunchedEffect(lockEnabled) {
                    if (lockEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
                if (locked) {
                    // Rien d'autre n'est composé tant que c'est verrouillé : aucune donnée ne passe à l'écran.
                    LockRoute(authenticate = { authenticator.authenticate(this@MainActivity) }, onUnlock = appLock::unlock)
                } else {
                    AppContent(state)
                }
            }
        }
    }
}

@Composable
private fun AppContent(state: AppUiState) {
    when (state) {
        // Rien tant que la base n'est pas lue : évite de faire clignoter l'onboarding.
        AppUiState.Loading -> {
            Unit
        }

        AppUiState.NeedsOnboarding -> {
            OnboardingRoute()
        }

        AppUiState.Ready -> {
            AppShell()
        }
    }
}
