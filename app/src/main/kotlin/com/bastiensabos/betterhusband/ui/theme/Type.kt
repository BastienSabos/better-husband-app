package com.bastiensabos.betterhusband.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bastiensabos.betterhusband.R

// Polices embarquées (aucun téléchargement) : variables, réduites au latin. Licence SIL OFL 1.1,
// textes dans assets/licenses/.

@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resId: Int,
    weight: FontWeight,
) = Font(resId = resId, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Titres : Bricolage Grotesque, chaleureuse et un peu espiègle. */
val DisplayFontFamily =
    FontFamily(
        variableFont(R.font.bricolage_grotesque, FontWeight.Medium),
        variableFont(R.font.bricolage_grotesque, FontWeight.SemiBold),
        variableFont(R.font.bricolage_grotesque, FontWeight.Bold),
    )

/** Texte courant : Inter, très lisible aux petites tailles. */
val TextFontFamily =
    FontFamily(
        variableFont(R.font.inter, FontWeight.Normal),
        variableFont(R.font.inter, FontWeight.Medium),
        variableFont(R.font.inter, FontWeight.SemiBold),
    )

private fun display(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun text(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = TextFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

internal val AppTypography =
    Typography(
        displayLarge = display(size = 57, line = 64, weight = FontWeight.Bold, tracking = -0.02),
        displayMedium = display(size = 45, line = 52, weight = FontWeight.Bold, tracking = -0.02),
        displaySmall = display(size = 36, line = 44, weight = FontWeight.Bold, tracking = -0.015),
        // Titre d'écran (« Bonjour Alex », « Carnet d'idées »).
        headlineLarge = display(size = 30, line = 36, weight = FontWeight.Bold, tracking = -0.015),
        headlineMedium = display(size = 28, line = 34, weight = FontWeight.Bold, tracking = -0.01),
        // Texte de la carte du jour.
        headlineSmall = display(size = 24, line = 30, weight = FontWeight.SemiBold, tracking = -0.008),
        titleLarge = display(size = 20, line = 25, weight = FontWeight.SemiBold),
        // Titre de section ou de carte (« Anniversaire de Camille »).
        titleMedium = display(size = 18, line = 24, weight = FontWeight.SemiBold),
        titleSmall = text(size = 15, line = 20, weight = FontWeight.SemiBold),
        bodyLarge = text(size = 16, line = 22, weight = FontWeight.Normal),
        bodyMedium = text(size = 14, line = 20, weight = FontWeight.Normal),
        bodySmall = text(size = 13, line = 18, weight = FontWeight.Normal),
        labelLarge = text(size = 15, line = 20, weight = FontWeight.SemiBold),
        labelMedium = text(size = 13, line = 18, weight = FontWeight.SemiBold, tracking = 0.02),
        labelSmall = text(size = 12, line = 16, weight = FontWeight.Medium),
    )
