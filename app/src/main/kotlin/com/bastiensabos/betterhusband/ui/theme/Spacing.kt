package com.bastiensabos.betterhusband.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Échelle d'espacement sur une grille de 4 dp. */
@Immutable
data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    /** Marge latérale des écrans. */
    val screen: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    /** Hauteur minimale d'une cible tactile. */
    val touchTarget: Dp = 48.dp,
)
