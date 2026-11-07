package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.database.UserStats
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    stats: UserStats?, onTheme: (String) -> Unit, onSound: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit, onHaptic: (Boolean) -> Unit,
    onDemo: () -> Unit, onReset: () -> Unit, onDismiss: () -> Unit,
) {
    val p = LocalGamePalette.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    GameDialog("settings_dialog", onDismiss) {
        Eyebrow("Make it yours")
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = p.ink)
        Text("Two worlds. Your rhythm.", color = p.muted, style = MaterialTheme.typography.bodyMedium)
        ThemeChoices(stats?.themeName ?: VisualTheme.ORBITAL.displayName, onTheme)
        HorizontalDivider(color = p.line.copy(alpha = .5f))
        SettingsToggle("Sound effects", stats?.soundEnabled != false, onSound, "toggle_sound")
        SettingsToggle("Ambient music", stats?.musicEnabled != false, onMusic, "toggle_music")
        SettingsToggle("Haptic feedback", stats?.hapticEnabled != false, onHaptic, "toggle_haptic")
        GameButton("How to play", onDemo, Modifier.fillMaxWidth().testTag("settings_demo"), style = ActionStyle.SECONDARY)
        if (confirm) {
            Text("Reset all progress?", color = p.ink, style = MaterialTheme.typography.titleMedium)
            Text("This clears stars, attempts, tokens and your current puzzle.", color = p.muted, style = MaterialTheme.typography.bodyMedium)
            GameButton("Yes, reset progress", { onReset(); onDismiss() }, Modifier.fillMaxWidth().testTag("confirm_reset_data"), style = ActionStyle.DANGER)
            GameButton("Keep my progress", { confirm = false }, Modifier.fillMaxWidth(), style = ActionStyle.SECONDARY)
        } else {
            TextButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().testTag("reset_data")) { Text("Reset progress", color = p.danger) }
        }
        GameButton("Done", onDismiss, Modifier.fillMaxWidth().testTag("settings_done"))
    }
}

@Composable
fun ThemeChoices(themeName: String, onTheme: (String) -> Unit) {
    val current = VisualTheme.fromName(themeName)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VisualTheme.entries.forEach { theme ->
            val colors = if (theme == VisualTheme.ORBITAL) OrbitalPalette else PopPalette
            val active = theme == current
            Surface(
                onClick = { onTheme(theme.displayName) }, color = colors.panel,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(if (active) 3.dp else 1.dp, if (active) colors.accent else colors.line),
                modifier = Modifier.fillMaxWidth().testTag("theme_${theme.name.lowercase()}")
                    .semantics { selected = active; role = Role.RadioButton },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(colors.violet, colors.accent, colors.gold).forEach { color ->
                            Box(Modifier.size(18.dp, 30.dp).background(color, RoundedCornerShape(6.dp)))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(theme.displayName, color = colors.ink, style = MaterialTheme.typography.titleMedium)
                        Text(if (colors.dark) "Dark · luminous & focused" else "Light · bold & playful", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (active) Text("✓", color = colors.ink, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    val p = LocalGamePalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = p.ink, style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange, Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedTrackColor = p.violet, checkedThumbColor = p.onViolet,
                uncheckedTrackColor = p.panelRaised, uncheckedThumbColor = p.muted, uncheckedBorderColor = p.line))
    }
}
