package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyanArcColorScheme = darkColorScheme(
    primary = ArcCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = ArcCyanMuted,
    onPrimaryContainer = ArcCyanGlow,
    secondary = ArcCyanSecondary,
    onSecondary = Color.White,
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = CyberVoid,
    onBackground = TextHigh,
    surface = CyberDarkSurface,
    onSurface = TextHigh,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextMedium,
    outline = CyberCardBorder
)

private val EmeraldMatrixColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldMuted,
    onPrimaryContainer = EmeraldPrimary,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    tertiary = ArcCyanGlow,
    onTertiary = Color.Black,
    background = CyberVoid,
    onBackground = TextHigh,
    surface = CyberDarkSurface,
    onSurface = TextHigh,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextMedium,
    outline = CyberCardBorder
)

private val CrimsonCoreColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonMuted,
    onPrimaryContainer = CrimsonSecondary,
    secondary = CrimsonSecondary,
    onSecondary = Color.Black,
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = CyberVoid,
    onBackground = TextHigh,
    surface = CyberDarkSurface,
    onSurface = TextHigh,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextMedium,
    outline = CyberCardBorder
)

@Composable
fun RobbyAiTheme(
    themeKey: String = "cyan_arc",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeKey) {
        "emerald_matrix" -> EmeraldMatrixColorScheme
        "crimson_core" -> CrimsonCoreColorScheme
        else -> CyanArcColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
