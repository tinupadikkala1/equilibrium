package com.example.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.engine.GameRules
import com.example.ui.components.*
import com.example.ui.theme.LocalGamePalette

@Composable
fun DemoDialog(onDismiss: () -> Unit) {
    val p = LocalGamePalette.current
    val moves = remember { listOf(Pair(1, 1), Pair(0, 1), Pair(2, 1)) }
    fun initial(): Array<IntArray> = Array(3) { IntArray(3) { 5 } }.also { b -> moves.forEach { (r, c) -> GameRules.applyReverseTap(b, r, c) } }
    var board by remember { mutableStateOf(initial()) }
    var step by remember { mutableIntStateOf(0) }
    val won = step == moves.size
    GameDialog("demo_dialog", onDismiss) {
        Eyebrow("Learn by doing")
        Text(if (won) "All in balance." else "Follow the ripple.", style = MaterialTheme.typography.headlineMedium, color = p.ink)
        Text(if (won) "Three taps, one shared number. You're ready." else "Tap the highlighted tile. Its neighbors gain one; the tile itself loses one.", color = p.muted)
        PuzzleBoard(board, moves.getOrNull(step), !won, { r, c ->
            if (Pair(r, c) == moves.getOrNull(step)) {
                board = board.map { it.clone() }.toTypedArray().also { GameRules.applyTap(it, r, c) }
                step += 1
            }
        })
        Text("$step / ${moves.size} moves", color = p.muted)
        if (won) GameButton("Try the demo again", { board = initial(); step = 0 }, Modifier.fillMaxWidth(), style = ActionStyle.SECONDARY)
        GameButton("Got it", onDismiss, Modifier.fillMaxWidth().testTag("demo_done"))
    }
}
