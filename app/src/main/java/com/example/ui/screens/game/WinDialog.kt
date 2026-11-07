package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.LocalGamePalette

@Composable
fun WinDialog(
    starredScore: Int, movesCount: Int, par: Int, levelId: Int, previousBestMoves: Int, secondsElapsed: Int,
    onNextLevel: () -> Unit, onReplay: () -> Unit, onBack: () -> Unit, onReward: () -> Unit,
) {
    val p = LocalGamePalette.current
    GameDialog("win_dialog", onBack, dismissible = false) {
        Box(Modifier.fillMaxWidth().height(94.dp), contentAlignment = Alignment.Center) {
            BalanceEmblem()
            ConfettiOverlay()
        }
        Eyebrow("Stage $levelId complete")
        Text("Perfect balance.", color = p.ink, style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth().semantics { contentDescription = "$starredScore out of 3 stars" }, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { Icon(Icons.Default.Star, null, Modifier.size(30.dp), tint = if (it < starredScore) { if (p.dark) p.gold else p.violet } else p.muted.copy(alpha = .3f)) }
        }
        val message = when {
            previousBestMoves == 0 -> "Your first clear. A new rhythm found."
            movesCount < previousBestMoves -> "New best! ${previousBestMoves - movesCount} fewer moves."
            movesCount == previousBestMoves -> "You matched your best solve."
            else -> "Your best is $previousBestMoves moves. Another try?"
        }
        Text(message, color = p.muted, style = MaterialTheme.typography.bodyMedium)
        Text("$movesCount moves  /  $par par  ·  ${formatTime(secondsElapsed)}", color = p.ink, style = MaterialTheme.typography.titleMedium)
        GameButton("Next stage", onNextLevel, Modifier.fillMaxWidth().testTag("win_next_button"))
        GameButton("Replay stage", onReplay, Modifier.fillMaxWidth().testTag("win_replay_button"), style = ActionStyle.SECONDARY)
        GameButton("Earn 3 hints", onReward, Modifier.fillMaxWidth().testTag("win_reward"), style = ActionStyle.GOLD, subtitle = "Watch a rewarded ad")
        TextButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Back to journey", color = p.ink) }
    }
}
