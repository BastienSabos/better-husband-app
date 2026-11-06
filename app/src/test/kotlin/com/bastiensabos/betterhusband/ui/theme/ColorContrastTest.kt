package com.bastiensabos.betterhusband.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Garantit le contraste WCAG 2.1 AA de la palette, en clair comme en sombre. */
class ColorContrastTest {
    @Test
    fun `light scheme text pairs meet AA`() = assertTextPairs(LightColors, "clair")

    @Test
    fun `dark scheme text pairs meet AA`() = assertTextPairs(DarkColors, "sombre")

    @Test
    fun `outlines are visible against surfaces`() {
        for ((name, scheme) in listOf("clair" to LightColors, "sombre" to DarkColors)) {
            for (surface in listOf(scheme.surface, scheme.surfaceContainerLow)) {
                assertWithMessage("outline / surface ($name)")
                    .that(contrast(scheme.outline, surface))
                    .isAtLeast(NON_TEXT_MIN)
            }
        }
    }

    @Test
    fun `category tones meet AA`() {
        for ((name, colors) in listOf("clair" to LightCategoryColors, "sombre" to DarkCategoryColors)) {
            listOf(colors.care, colors.gift, colors.outing, colors.done).forEach { tone ->
                assertWithMessage("catégorie ($name) $tone")
                    .that(contrast(tone.content, tone.container))
                    .isAtLeast(TEXT_MIN)
            }
        }
    }

    @Test
    fun `contrast formula matches WCAG reference values`() {
        assertWithMessage("noir sur blanc").that(contrast(Color.Black, Color.White)).isWithin(0.01).of(21.0)
        assertWithMessage("même couleur").that(contrast(Burgundy, Burgundy)).isWithin(0.001).of(1.0)
    }

    private fun assertTextPairs(
        scheme: ColorScheme,
        name: String,
    ) {
        val pairs =
            mapOf(
                "onPrimary / primary" to (scheme.onPrimary to scheme.primary),
                "onPrimaryContainer / primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondary / secondary" to (scheme.onSecondary to scheme.secondary),
                "onSecondaryContainer / secondaryContainer" to
                    (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "onTertiary / tertiary" to (scheme.onTertiary to scheme.tertiary),
                "onTertiaryContainer / tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
                "onError / error" to (scheme.onError to scheme.error),
                "onBackground / background" to (scheme.onBackground to scheme.background),
                "onSurface / surface" to (scheme.onSurface to scheme.surface),
                "onSurfaceVariant / surface" to (scheme.onSurfaceVariant to scheme.surface),
                "onSurfaceVariant / surfaceContainerHigh" to (scheme.onSurfaceVariant to scheme.surfaceContainerHigh),
                "onSurfaceVariant / surfaceContainerHighest" to
                    (scheme.onSurfaceVariant to scheme.surfaceContainerHighest),
                "primary / surface" to (scheme.primary to scheme.surface),
                "inverseOnSurface / inverseSurface" to (scheme.inverseOnSurface to scheme.inverseSurface),
            )
        pairs.forEach { (label, colors) ->
            assertWithMessage("$label ($name)").that(contrast(colors.first, colors.second)).isAtLeast(TEXT_MIN)
        }
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private companion object {
        const val TEXT_MIN = 4.5
        const val NON_TEXT_MIN = 3.0
    }
}
