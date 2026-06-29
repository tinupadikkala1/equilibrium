package com.example.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.engine.GameRules
import com.example.ui.components.AnimatedTile
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonMint
import com.example.ui.theme.SlateMutedText
import kotlinx.coroutines.delay

@Composable
fun DemoDialog(onDismiss: () -> Unit) {
    // Predefined 3x3 board: starts scrambled, solved in 4 taps
    // Start from solved [5,5,5 / 5,5,5 / 5,5,5], apply reverse taps at (1,1),(0,0),(2,2),(1,0)
    val initialBoard = arrayOf(
        intArrayOf(4, 4, 5),
        intArrayOf(3, 7, 4),
        intArrayOf(5, 4, 4)
    )
    // Solution: tap (1,1), (1,1), (0,0), (1,0) — but let's use a cleaner 4-move sequence
    // Actually let's generate a proper demo: start solved, reverse-tap 4 times, record moves
    val moves = listOf(Pair(1, 1), Pair(0, 1), Pair(2, 1), Pair(1, 0))

    // Build the starting board by applying reverse taps to [5,5,5...]
    val startBoard = remember {
        val board = Array(3) { IntArray(3) { 5 } }
        moves.forEach { (r, c) -> GameRules.applyReverseTap(board, r, c) }
        board
    }

    var board by remember { mutableStateOf(startBoard.map { it.clone() }.toTypedArray()) }
    var moveIndex by remember { mutableStateOf(-1) }
    var won by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Watch how the game is played!") }

    val progress = remember(board) { GameRules.calculateProgress(board) }

    // Auto-play moves with delays
    LaunchedEffect(Unit) {
        delay(2000) // Initial pause
        for (i in moves.indices) {
            message = "Tap ${i + 1}/${moves.size}: Row ${moves[i].first + 1}, Col ${moves[i].second + 1}"
            moveIndex = i
            delay(1500) // Pause before tap
            // Apply tap
            val newBoard = board.map { it.clone() }.toTypedArray()
            GameRules.applyTap(newBoard, moves[i].first, moves[i].second)
            board = newBoard
            delay(800) // Pause after tap to see result
        }
        // Check win
        if (GameRules.checkWinCondition(board)) {
            won = true
            message = "🎉 EQUILIBRIUM FOUND!"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            if (won) {
                ConfettiOverlay()
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DEMO", color = ActionNeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text(message, color = if (won) ActionNeonMint else SlateMutedText, fontSize = 12.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)

                    Spacer(Modifier.height(16.dp))

                    // Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (r in 0 until 3) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    for (c in 0 until 3) {
                                        val isHighlighted = moveIndex >= 0 && moveIndex < moves.size && moves[moveIndex] == Pair(r, c) && !won
                                        Box(modifier = Modifier.weight(1f)) {
                                            AnimatedTile(
                                                value = board[r][c],
                                                progress = progress,
                                                isHintHighlighted = isHighlighted,
                                                onTap = { /* Demo is non-interactive */ }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Progress
                    Text("Balance: ${(progress * 100).toInt()}%", color = ActionNeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = if (won) ActionNeonMint else ActionNeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(if (won) "GOT IT!" else "SKIP", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
