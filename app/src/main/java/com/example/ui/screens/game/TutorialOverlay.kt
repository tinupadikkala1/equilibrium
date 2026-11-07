package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.engine.GameRules
import com.example.ui.components.*
import com.example.ui.theme.LocalGamePalette

/** Practice happens in the dialog itself so an overlay never blocks the requested tap. */
@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
    val p = LocalGamePalette.current
    var tapped by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(Array(3) { IntArray(3) { 5 } }.also { GameRules.applyReverseTap(it, 1, 1) }) }
    GameDialog("tutorial_overlay", onFinish) {
        Eyebrow("A little practice")
        Text(if (tapped) "That's equilibrium." else "One tap. A ripple.", color = p.ink, style = MaterialTheme.typography.headlineMedium)
        Text(if (tapped) "Every tile is equal. Now beat the clock in your first stage." else "Tap the highlighted center: it loses 1, and each neighbor gains 1. Make every number equal.", color = p.muted)
        PuzzleBoard(board, if (tapped) null else Pair(1, 1), !tapped, { r, c ->
            if (r == 1 && c == 1) {
                board = board.map { it.clone() }.toTypedArray().also { GameRules.applyTap(it, r, c) }
                tapped = true
            }
        })
        GameButton(if (tapped) "Let's play" else "Skip practice", onFinish, Modifier.fillMaxWidth().testTag("tutorial_finish"))
    }
}
