package com.bastiensabos.betterhusband

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bastiensabos.betterhusband.feature.onboarding.OnboardingRoute
import com.bastiensabos.betterhusband.feature.today.TodayRoute
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint

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
                    AppUiState.Ready -> TodayRoute()
                }
            }
        }
    }
}
