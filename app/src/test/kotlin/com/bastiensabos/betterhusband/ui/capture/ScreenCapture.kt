@file:OptIn(ExperimentalRoborazziApi::class)

package com.bastiensabos.betterhusband.ui.capture

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.bastiensabos.betterhusband.ui.theme.BetterHusbandTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker.CheckLevel
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import org.robolectric.RuntimeEnvironment

/** Les configurations où un écran casse le plus souvent ; le nom devient le suffixe du PNG (repris du budget planner). */
enum class CaptureConfig(
    val qualifiers: String,
    val darkMode: Boolean = false,
    val fontScale: Float = 1f,
) {
    TELEPHONE("fr-rFR-w360dp-h640dp-480dpi"),
    TELEPHONE_SOMBRE("fr-rFR-w360dp-h640dp-480dpi", darkMode = true),
    TELEPHONE_POLICE_200("fr-rFR-w360dp-h640dp-480dpi", fontScale = 2f),
}

private val roborazziOptions =
    RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
    )

/** Affiche [content] dans chaque [CaptureConfig], passe les contrôles ATF puis capture `src/test/screenshots/<name>_<config>.png`. */
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.captureScreen(
    name: String,
    configs: List<CaptureConfig> = CaptureConfig.entries,
    content: @Composable () -> Unit,
) {
    val errors = mutableListOf<Throwable>()
    configs.forEach { config ->
        RuntimeEnvironment.setQualifiers(config.qualifiers)
        activity.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.DarkMode(config.darkMode) then
                    DeviceConfigurationOverride.FontScale(config.fontScale),
            ) {
                BetterHusbandTheme(darkTheme = config.darkMode) { content() }
            }
        }
        runCatching {
            onRoot().checkRoboAccessibility(
                roborazziATFAccessibilityCheckOptions =
                    RoborazziATFAccessibilityCheckOptions(
                        failureLevel = CheckLevel.Error,
                        checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                    ),
            )
        }.exceptionOrNull()?.let(errors::add)
        onRoot().captureRoboImage("src/test/screenshots/${name}_${config.name.lowercase()}.png", roborazziOptions = roborazziOptions)
    }
    errors.firstOrNull()?.let { throw it }
}
