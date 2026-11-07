package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.*
import com.example.ui.theme.LocalGamePalette

@Composable
fun RewardDialog(type: String, busy: Boolean, onWatch: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalGamePalette.current
    val title = if (type == "time") "A little more time." else "A fresh boost."
    val reward = when (type) { "undo" -> "3 undos"; "skip" -> "3 skips"; "time" -> "30 seconds"; else -> "3 hints" }
    GameDialog("reward_dialog", onDismiss, dismissible = !busy) {
        Eyebrow("Optional reward")
        Text(title, color = p.ink, style = MaterialTheme.typography.headlineLarge)
        Text("Watch a video to earn $reward. Your timer stays paused while the ad plays.", color = p.muted)
        GameButton(if (busy) "Opening ad…" else "Watch & earn $reward", onWatch, Modifier.fillMaxWidth().testTag("reward_watch"), style = ActionStyle.GOLD, enabled = !busy)
        GameButton("Not now", onDismiss, Modifier.fillMaxWidth().testTag("reward_cancel"), style = ActionStyle.SECONDARY, enabled = !busy)
    }
}
