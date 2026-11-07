package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    @Test fun bothThemesKeepTextLegibleOnEveryActionSurface() {
        listOf(OrbitalPalette, PopPalette).forEach { p ->
            listOf(p.ink to p.background, p.ink to p.panel, p.muted to p.panel, p.ink to p.panelRaised,
                p.onAccent to p.accent, p.onViolet to p.violet, p.onGold to p.gold, p.danger to p.panelRaised).forEach { (text, fill) ->
                val contrast = (maxOf(text.luminance(), fill.luminance()) + .05f) / (minOf(text.luminance(), fill.luminance()) + .05f)
                assertTrue("dark=${p.dark}: $text on $fill has contrast $contrast", contrast >= 4.5f)
            }
        }
    }

    @Test fun legacyThemesResolveIntoTheTwoSupportedModes() {
        assertEquals(VisualTheme.POP, VisualTheme.fromName("Pastel Breeze"))
        assertEquals(VisualTheme.ORBITAL, VisualTheme.fromName("Neon Pulse"))
        assertEquals(VisualTheme.ORBITAL, VisualTheme.fromName("unknown"))
    }
}
