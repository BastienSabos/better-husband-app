package com.bastiensabos.betterhusband

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.feature.dates.DatesRoute
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingRoute
import com.bastiensabos.betterhusband.feature.partner.PartnerProfileRoute
import com.bastiensabos.betterhusband.feature.today.TodayRoute
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint

/** Navigation à plat tant qu'il n'y a que quelques écrans ; Navigation Compose viendra avec les onglets. */
private enum class Destination { TODAY, PARTNER, DATES }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterHusbandTheme {
                val state by appViewModel.uiState.collectAsStateWithLifecycle()
                when (state) {
                    // Rien tant que la base n'est pas lue : évite de faire clignoter l'onboarding.
                    AppUiState.Loading -> Unit
                    AppUiState.NeedsOnboarding -> OnboardingRoute()
                    AppUiState.Ready -> {
                        var destination by rememberSaveable { mutableStateOf(Destination.TODAY) }
                        BackHandler(enabled = destination != Destination.TODAY) { destination = Destination.TODAY }
                        when (destination) {
                            Destination.TODAY ->
                                TodayRoute(
                                    onOpenPartner = { destination = Destination.PARTNER },
                                    onOpenDates = { destination = Destination.DATES },
                                )
                            Destination.PARTNER -> PartnerProfileRoute(onDone = { destination = Destination.TODAY })
                            Destination.DATES -> DatesRoute(onBack = { destination = Destination.TODAY })
                        }
                    }
                }
            }
        }
    }
}
