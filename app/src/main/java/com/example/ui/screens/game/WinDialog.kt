package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun WinDialog(starredScore: Int, movesCount: Int, par: Int, levelId: Int, isDailyChallenge: Boolean, previousBestMoves: Int, onNextLevel: () -> Unit, onReplay: () -> Unit, onBack: () -> Unit, onShowInterstitialAd: (() -> Unit) -> Unit, onShowRewardedAd: ((Int) -> Unit) -> Unit = {}) {
    val personalMsg = remember(movesCount, previousBestMoves) {
        when { previousBestMoves == 0 -> "🎯 First clear!"; movesCount < previousBestMoves -> "🔥 New best! ${previousBestMoves - movesCount} fewer moves"; movesCount == previousBestMoves -> "Matched your best!"; else -> "Best: $previousBestMoves moves" }
    }
    val (doubled, setDoubled) = remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
            ConfettiOverlay()
            Card(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.5.dp, ActionNeonMint.copy(0.5f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth().padding(20.dp).testTag("win_dialog")) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, null, tint = AccentGold, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("EQUILIBRIUM FOUND", color = ActionNeonMint, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    Text("Perfect harmony achieved.", color = SlateMutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    // Stars
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { i -> Icon(Icons.Default.Star, null, tint = if (i < starredScore) AccentGold else Color.White.copy(0.1f), modifier = Modifier.size(36.dp)) } }
                    Spacer(Modifier.height(10.dp))
                    // Personal best
                    Text(personalMsg, color = if (previousBestMoves == 0 || movesCount <= previousBestMoves) AccentGold else SlateMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(12.dp))
                    // Moves / Par
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Moves: $movesCount", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Par: $par", color = SlateMutedText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(14.dp))
                    // Double stars ad
                    if (!doubled && starredScore < 3) {
                        OutlinedButton(onClick = { onShowRewardedAd { setDoubled(true) } }, border = BorderStroke(1.dp, AccentGold.copy(0.5f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("⭐ WATCH AD → 3 STARS", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // Next
                    Button(onClick = { if (isDailyChallenge) onBack() else if (levelId % 5 == 0) onShowInterstitialAd { onNextLevel() } else onNextLevel() }, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp).testTag("win_next_button")) {
                        Text(if (isDailyChallenge) "BACK TO MENU" else "NEXT STAGE", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    TextButton(onClick = onReplay, Modifier.testTag("win_replay_button")) { Text("REPLAY", color = Color.White.copy(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
            }
        }
    }
}
