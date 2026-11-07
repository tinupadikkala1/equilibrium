package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class VisualTheme(val displayName: String) {
    ORBITAL("Orbital Glass"), POP("Pop Machine");
    companion object {
        fun fromName(name: String) = when (name) {
            "Pop Machine", "Pastel Breeze", "Minimal Mono" -> POP
            else -> ORBITAL
        }
    }
}

@Immutable
data class GamePalette(
    val dark: Boolean, val background: Color, val panel: Color, val panelRaised: Color,
    val ink: Color, val muted: Color, val accent: Color, val onAccent: Color,
    val violet: Color, val onViolet: Color, val gold: Color, val onGold: Color,
    val danger: Color, val line: Color, val shadow: Color,
)

val OrbitalPalette = GamePalette(
    dark = true,
    background = Color(0xFF070B1C), panel = Color(0xFF131D36), panelRaised = Color(0xFF1B2949),
    ink = Color(0xFFF3F6FF), muted = Color(0xFFA6B4D1),
    accent = Color(0xFF73F4D1), onAccent = Color(0xFF06251E),
    violet = Color(0xFFA699FF), onViolet = Color(0xFF15112D),
    gold = Color(0xFFFFD47B), onGold = Color(0xFF312008), danger = Color(0xFFFF9AA8),
    line = Color(0xFF354563), shadow = Color(0xFF020612),
)
val PopPalette = GamePalette(
    dark = false,
    background = Color(0xFFF5F0FF), panel = Color(0xFFFFFDFF), panelRaised = Color(0xFFE7DEFF),
    ink = Color(0xFF25213D), muted = Color(0xFF625978),
    accent = Color(0xFFFF7295), onAccent = Color(0xFF301124),
    violet = Color(0xFF6750D8), onViolet = Color.White,
    gold = Color(0xFFFFD269), onGold = Color(0xFF332405), danger = Color(0xFFAD254A),
    line = Color(0xFF302943), shadow = Color(0xFF302943),
)
val LocalGamePalette = staticCompositionLocalOf { OrbitalPalette }

@Composable
fun EquilibriumTheme(themeName: String = VisualTheme.ORBITAL.displayName, content: @Composable () -> Unit) {
    val p = if (VisualTheme.fromName(themeName) == VisualTheme.ORBITAL) OrbitalPalette else PopPalette
    val scheme = if (p.dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalGamePalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent, onPrimary = p.onAccent,
                primaryContainer = p.panelRaised, onPrimaryContainer = p.ink,
                secondary = p.violet, onSecondary = p.onViolet,
                secondaryContainer = p.panelRaised, onSecondaryContainer = p.ink,
                tertiary = p.gold, onTertiary = p.onGold,
                background = p.background, onBackground = p.ink,
                surface = p.panel, onSurface = p.ink,
                surfaceVariant = p.panelRaised, onSurfaceVariant = p.muted,
                error = p.danger, outline = p.line,
            ),
            typography = Typography, content = content,
        )
    }
}
