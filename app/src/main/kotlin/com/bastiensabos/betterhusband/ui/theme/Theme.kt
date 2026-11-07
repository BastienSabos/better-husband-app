package com.bastiensabos.betterhusband.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * Thème de l'app. Les couleurs de marque sont utilisées par défaut : la couleur dynamique (Material You)
 * reste disponible mais désactivée, car l'identité visuelle et les contrastes ont été réglés à la main.
 */
@Composable
fun BetterHusbandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColors
            }

            else -> {
                LightColors
            }
        }
    val categoryColors = if (darkTheme) DarkCategoryColors else LightCategoryColors
    CompositionLocalProvider(
        LocalCategoryColors provides categoryColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Accès aux jetons propres à l'app, en complément de [MaterialTheme]. */
object AppTheme {
    val categoryColors: CategoryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCategoryColors.current

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}
