package com.bastiensabos.betterhusband.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette issue des maquettes v1 (https://claude.ai/artifact/NDawahJc68Zk7pafzyJgBY).
// Chaque paire « texte / fond » est vérifiée au contraste WCAG AA par ColorContrastTest.

// Clair
internal val Burgundy = Color(0xFF8E3B46)
internal val BurgundyInk = Color(0xFF5C1F2A)
internal val Blush = Color(0xFFF7DEE1)
internal val Apricot = Color(0xFFFBE6D4)
internal val ApricotInk = Color(0xFF7A3B0E)
internal val Sage = Color(0xFFDCEAE1)
internal val SageDeep = Color(0xFF24513C)
internal val SageInk = Color(0xFF183D2C)
internal val Mist = Color(0xFFDCE6F2)
internal val MistInk = Color(0xFF1F3F66)
internal val Linen = Color(0xFFFAF6F2)
internal val Ink = Color(0xFF231B1E)
internal val InkMuted = Color(0xFF6D6064)
internal val Sand = Color(0xFFF1E8E3)
internal val SandDeep = Color(0xFFEADFD9)
internal val Pebble = Color(0xFF8C7D82)

// Sombre
internal val Rose = Color(0xFFFFB2BA)
internal val RoseInk = Color(0xFF561D27)
internal val Wine = Color(0xFF73303A)
internal val WineLight = Color(0xFFFFD9DD)
internal val Peach = Color(0xFFFFB68A)
internal val PeachInk = Color(0xFF4F2405)
internal val Rust = Color(0xFF6A3A17)
internal val RustLight = Color(0xFFFFDCC6)
internal val Mint = Color(0xFFA6D0B6)
internal val MintInk = Color(0xFF0E3724)
internal val Forest = Color(0xFF2A4F3B)
internal val ForestLight = Color(0xFFC2EDD1)
internal val Night = Color(0xFF24384F)
internal val NightLight = Color(0xFFD3E4FA)
internal val Cocoa = Color(0xFF1A1517)
internal val CocoaRaised = Color(0xFF221C1E)
internal val CocoaHigh = Color(0xFF2B2427)
internal val CocoaHighest = Color(0xFF362E31)
internal val Cream = Color(0xFFF2E9E6)
internal val CreamMuted = Color(0xFFD3C4C7)
internal val Smoke = Color(0xFF9C8D91)
internal val SmokeDeep = Color(0xFF4A3F43)

internal val LightColors =
    lightColorScheme(
        primary = Burgundy,
        onPrimary = Color.White,
        primaryContainer = Blush,
        onPrimaryContainer = BurgundyInk,
        secondary = ApricotInk,
        onSecondary = Color.White,
        secondaryContainer = Apricot,
        onSecondaryContainer = ApricotInk,
        tertiary = SageDeep,
        onTertiary = Color.White,
        tertiaryContainer = Sage,
        onTertiaryContainer = SageInk,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Linen,
        onBackground = Ink,
        surface = Linen,
        onSurface = Ink,
        surfaceVariant = Sand,
        onSurfaceVariant = InkMuted,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color(0xFFF6F0EC),
        surfaceContainerHigh = Sand,
        surfaceContainerHighest = SandDeep,
        outline = Pebble,
        outlineVariant = SandDeep,
        inverseSurface = Ink,
        inverseOnSurface = Linen,
        inversePrimary = Rose,
        scrim = Color.Black,
    )

internal val DarkColors =
    darkColorScheme(
        primary = Rose,
        onPrimary = RoseInk,
        primaryContainer = Wine,
        onPrimaryContainer = WineLight,
        secondary = Peach,
        onSecondary = PeachInk,
        secondaryContainer = Rust,
        onSecondaryContainer = RustLight,
        tertiary = Mint,
        onTertiary = MintInk,
        tertiaryContainer = Forest,
        onTertiaryContainer = ForestLight,
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Cocoa,
        onBackground = Cream,
        surface = Cocoa,
        onSurface = Cream,
        surfaceVariant = CocoaHigh,
        onSurfaceVariant = CreamMuted,
        surfaceContainerLowest = Color(0xFF140F11),
        surfaceContainerLow = CocoaRaised,
        surfaceContainer = CocoaRaised,
        surfaceContainerHigh = CocoaHigh,
        surfaceContainerHighest = CocoaHighest,
        outline = Smoke,
        outlineVariant = SmokeDeep,
        inverseSurface = Cream,
        inverseOnSurface = Cocoa,
        inversePrimary = Burgundy,
        scrim = Color.Black,
    )
