package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.database.UserStats
import com.example.ui.components.*
import com.example.ui.screens.SettingsToggle
import com.example.ui.screens.ThemeChoices
import com.example.ui.theme.*

@Composable
fun PauseSettingsDialog(
    stats: UserStats?, onSound: (Boolean) -> Unit, onMusic: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit, onTheme: (String) -> Unit,
    onReset: () -> Unit, onSkip: () -> Unit, onDemo: () -> Unit, onBack: () -> Unit, onDismiss: () -> Unit,
) {
    val p = LocalGamePalette.current
    GameDialog("pause_dialog", onDismiss) {
        Eyebrow("Take a breath")
        Text("Paused.", color = p.ink, style = MaterialTheme.typography.headlineLarge)
        Text("Your board and timer are on hold.", color = p.muted, style = MaterialTheme.typography.bodyMedium)
        GameButton("Resume puzzle", onDismiss, Modifier.fillMaxWidth().testTag("pause_resume"))
        ThemeChoices(stats?.themeName ?: VisualTheme.ORBITAL.displayName, onTheme)
        SettingsToggle("Sound", stats?.soundEnabled != false, onSound, "pause_sound")
        SettingsToggle("Music", stats?.musicEnabled != false, onMusic, "pause_music")
        SettingsToggle("Haptics", stats?.hapticEnabled != false, onHaptic, "pause_haptic")
        GameButton("Restart stage", onReset, Modifier.fillMaxWidth().testTag("pause_restart"), style = ActionStyle.SECONDARY)
        GameButton("Skip stage · ${stats?.skips ?: 0} left", onSkip, Modifier.fillMaxWidth().testTag("pause_skip"), style = ActionStyle.SECONDARY)
        TextButton(onClick = onDemo, Modifier.fillMaxWidth()) { Text("How to play", color = p.ink) }
        GameButton("Save & leave", onBack, Modifier.fillMaxWidth().testTag("pause_home"), style = ActionStyle.SECONDARY)
    }
}
