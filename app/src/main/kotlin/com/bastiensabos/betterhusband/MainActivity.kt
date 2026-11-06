package com.bastiensabos.betterhusband

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bastiensabos.betterhusband.feature.today.TodayRoute
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterHusbandTheme {
                TodayRoute()
            }
        }
    }
}
