package com.bastiensabos.betterhusband.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Un couple fond / contenu pour une pastille ou une carte de catégorie. */
@Immutable
data class ToneColors(
    val container: Color,
    val content: Color,
)

/**
 * Couleurs sémantiques hors Material 3 : une teinte par type de contenu, reprise dans tous les écrans
 * (carnet, dates, flux du jour). Toujours accompagnées d'un libellé : la couleur seule ne porte pas le sens.
 */
@Immutable
data class CategoryColors(
    /** Couple, attentions, dates à deux. */
    val care: ToneColors,
    /** Cadeaux, dates de la famille. */
    val gift: ToneColors,
    /** Sorties et moments à l'extérieur. */
    val outing: ToneColors,
    /** Fait, foyer, confirmation. */
    val done: ToneColors,
)

internal val LightCategoryColors =
    CategoryColors(
        care = ToneColors(container = Blush, content = BurgundyInk),
        gift = ToneColors(container = Apricot, content = ApricotInk),
        outing = ToneColors(container = Mist, content = MistInk),
        done = ToneColors(container = Sage, content = SageInk),
    )

internal val DarkCategoryColors =
    CategoryColors(
        care = ToneColors(container = Wine, content = WineLight),
        gift = ToneColors(container = Rust, content = RustLight),
        outing = ToneColors(container = Night, content = NightLight),
        done = ToneColors(container = Forest, content = ForestLight),
    )

internal val LocalCategoryColors = staticCompositionLocalOf { LightCategoryColors }
