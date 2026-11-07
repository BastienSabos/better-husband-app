package com.bastiensabos.betterhusband

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.data.security.BiometricAuthenticator
import com.bastiensabos.betterhusband.data.security.LockSettings
import com.bastiensabos.betterhusband.domain.AppLock
import com.bastiensabos.betterhusband.feature.couple.CoupleProfileRoute
import com.bastiensabos.betterhusband.feature.dates.DatesRoute
import com.bastiensabos.betterhusband.feature.ideas.IdeasRoute
import com.bastiensabos.betterhusband.feature.lock.LockRoute
import com.bastiensabos.betterhusband.feature.mydata.MyDataRoute
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingRoute
import com.bastiensabos.betterhusband.feature.partner.PartnerProfileRoute
import com.bastiensabos.betterhusband.feature.tasks.TasksRoute
import com.bastiensabos.betterhusband.feature.today.TodayRoute
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Navigation à plat tant qu'il n'y a que quelques écrans ; Navigation Compose viendra avec les onglets. */
private enum class Destination { TODAY, PARTNER, DATES, IDEAS, TASKS, MY_DATA, COUPLE }

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
            var destination by rememberSaveable { mutableStateOf(Destination.TODAY) }
            BackHandler(enabled = destination != Destination.TODAY) { destination = Destination.TODAY }
            when (destination) {
                Destination.TODAY -> {
                    TodayRoute(
                        onOpenPartner = { destination = Destination.PARTNER },
                        onOpenDates = { destination = Destination.DATES },
                        onOpenIdeas = { destination = Destination.IDEAS },
                        onOpenTasks = { destination = Destination.TASKS },
                        onOpenMyData = { destination = Destination.MY_DATA },
                        onOpenCoupleProfile = { destination = Destination.COUPLE },
                    )
                }

                Destination.PARTNER -> {
                    PartnerProfileRoute(onFinish = { destination = Destination.TODAY })
                }

                Destination.DATES -> {
                    DatesRoute(onBack = { destination = Destination.TODAY })
                }

                Destination.IDEAS -> {
                    IdeasRoute(onBack = { destination = Destination.TODAY })
                }

                Destination.TASKS -> {
                    TasksRoute(onBack = { destination = Destination.TODAY })
                }

                Destination.MY_DATA -> {
                    MyDataRoute(onBack = { destination = Destination.TODAY })
                }

                Destination.COUPLE -> {
                    CoupleProfileRoute(onFinish = { destination = Destination.TODAY })
                }
            }
        }
    }
}
