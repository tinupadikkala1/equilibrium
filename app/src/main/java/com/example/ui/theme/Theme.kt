package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Default schema maps
private val NeonColorScheme = darkColorScheme(
    primary = ActionNeonCyan,
    background = EquilibriumAmbientBg,
    surface = EquilibriumCardDepth,
    primaryContainer = ActionNeonCyan,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = ActionNeonMint,
    error = ActionNeonCoral
)

private val PastelColorScheme = darkColorScheme(
    primary = PastelCyan,
    background = PastelBg,
    surface = PastelCard,
    primaryContainer = PastelCyan,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = PastelMint,
    error = PastelCoral
)

private val MonoColorScheme = darkColorScheme(
    primary = MonoText,
    background = MonoBg,
    surface = MonoCard,
    primaryContainer = MonoText,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = MonoAccent,
    error = Color.Red
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    background = MidnightBg,
    surface = MidnightCard,
    primaryContainer = MidnightPrimary,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = MidnightSecondary,
    error = MidnightError
)

private val ForestColorScheme = darkColorScheme(
    primary = ForestPrimary,
    background = ForestBg,
    surface = ForestCard,
    primaryContainer = ForestPrimary,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = ForestSecondary,
    error = ForestError
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    background = SunsetBg,
    surface = SunsetCard,
    primaryContainer = SunsetPrimary,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = SunsetSecondary,
    error = SunsetError
)

@Composable
fun EquilibriumTheme(
    themeName: String = "Neon Pulse",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Pastel Breeze" -> PastelColorScheme
        "Minimal Mono" -> MonoColorScheme
        "Midnight" -> MidnightColorScheme
        "Forest" -> ForestColorScheme
        "Sunset" -> SunsetColorScheme
        else -> NeonColorScheme // Default "Neon Pulse"
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
