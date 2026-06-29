package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ActionNeonMint
import com.example.ui.theme.SlateMutedText

@Composable
fun WinDialog(
    starredScore: Int,
    movesCount: Int,
    par: Int,
    levelId: Int,
    isDailyChallenge: Boolean,
    previousBestMoves: Int,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
    onShowInterstitialAd: (() -> Unit) -> Unit,
    onShowRewardedAd: ((Int) -> Unit) -> Unit = {}
) {
    // Personal performance comparison
    val personalMessage = remember(movesCount, previousBestMoves, par) {
        when {
            previousBestMoves == 0 -> "First clear! 🎯"
            movesCount < previousBestMoves -> "🔥 New personal best! ${previousBestMoves - movesCount} fewer moves"
            movesCount == previousBestMoves -> "Matched your best!"
            else -> "Personal best: $previousBestMoves moves"
        }
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            ConfettiOverlay()
            Card(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, ActionNeonMint),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("win_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Success celebration", tint = AccentGold, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("EQUILIBRIUM FOUND", color = ActionNeonMint, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    Text("The elements have settled in perfect harmony.", color = SlateMutedText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        for (i in 1..3) {
                            Icon(Icons.Default.Star, contentDescription = "Earned Star", tint = if (i <= starredScore) AccentGold else Color.White.copy(alpha = 0.1f), modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Moves Taken: $movesCount", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Stage Par: $par", color = SlateMutedText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = personalMessage,
                        color = if (previousBestMoves == 0 || movesCount <= previousBestMoves) AccentGold else SlateMutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Double Stars via rewarded ad
                    val (doubled, setDoubled) = remember { mutableStateOf(false) }
                    if (!doubled && starredScore < 3) {
                        OutlinedButton(
                            onClick = { onShowRewardedAd { setDoubled(true) } },
                            border = BorderStroke(1.dp, AccentGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("⭐ WATCH AD → 3 STARS", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (isDailyChallenge) onBack()
                            else if (levelId % 5 == 0) onShowInterstitialAd { onNextLevel() }
                            else onNextLevel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("win_next_button")
                    ) {
                        Text(if (isDailyChallenge) "BACK TO MENU" else "NEXT STAGE", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onReplay, modifier = Modifier.testTag("win_replay_button")) {
                        Text("REPLAY STAGE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
