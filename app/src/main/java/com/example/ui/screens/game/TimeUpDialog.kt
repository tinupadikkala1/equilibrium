package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.*
import com.example.ui.theme.LocalGamePalette

@Composable
fun TimeUpDialog(onRetry: () -> Unit, onBack: () -> Unit, onWatchAdForTime: () -> Unit) {
    val p = LocalGamePalette.current
    GameDialog("times_up_dialog", onBack, dismissible = false) {
        Eyebrow("Time's up")
        Text("Another ripple?", style = MaterialTheme.typography.headlineLarge, color = p.ink)
        Text("Try a fresh approach, or give this puzzle 30 more seconds.", color = p.muted)
        GameButton("Try again", onRetry, Modifier.fillMaxWidth().testTag("times_up_retry_button"))
        GameButton("Add 30 seconds", onWatchAdForTime, Modifier.fillMaxWidth().testTag("times_up_watch_ad_button"), style = ActionStyle.GOLD, subtitle = "Watch a rewarded ad")
        GameButton("Back to journey", onBack, Modifier.fillMaxWidth(), style = ActionStyle.SECONDARY)
    }
}
