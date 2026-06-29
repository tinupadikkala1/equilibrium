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
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DemoDialog(onDismiss: () -> Unit) {
    val moves = listOf(Pair(1, 1), Pair(0, 1), Pair(2, 1), Pair(1, 0))
    val startBoard = remember {
        val b = Array(3) { IntArray(3) { 5 } }
        moves.forEach { (r, c) -> GameRules.applyReverseTap(b, r, c) }
        b
    }
    var board by remember { mutableStateOf(startBoard.map { it.clone() }.toTypedArray()) }
    var moveIdx by remember { mutableStateOf(-1) }
    var won by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("Watch how the game works!") }
    val progress = remember(board) { GameRules.calculateProgress(board) }

    LaunchedEffect(Unit) {
        delay(2000)
        for (i in moves.indices) {
            msg = "Tap ${i + 1}/${moves.size} → Row ${moves[i].first + 1}, Col ${moves[i].second + 1}"
            moveIdx = i
            delay(1500)
            val nb = board.map { it.clone() }.toTypedArray()
            GameRules.applyTap(nb, moves[i].first, moves[i].second)
            board = nb
            delay(800)
        }
        if (GameRules.checkWinCondition(board)) { won = true; msg = "🎉 EQUILIBRIUM FOUND!" }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.92f)), contentAlignment = Alignment.Center) {
            if (won) ConfettiOverlay()
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DEMO", color = ActionNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text(msg, color = if (won) ActionNeonMint else SlateMutedText, fontSize = 11.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth(0.75f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.background).padding(6.dp), contentAlignment = Alignment.Center) {
                        Column(Modifier.fillMaxSize()) {
                            for (r in 0 until 3) {
                                Row(Modifier.weight(1f).fillMaxWidth()) {
                                    for (c in 0 until 3) {
                                        Box(Modifier.weight(1f)) {
                                            AnimatedTile(board[r][c], progress, moveIdx >= 0 && moveIdx < moves.size && moves[moveIdx] == Pair(r, c) && !won, onTap = {})
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Balance: ${(progress * 100).toInt()}%", color = ActionNeonCyan.copy(0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = if (won) ActionNeonMint else ActionNeonCyan), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) {
                        Text(if (won) "GOT IT!" else "SKIP", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
