package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserStats
import com.example.data.engine.GameRules
import com.example.ui.components.AdmobBanner
import com.example.ui.components.AnimatedTile
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.screens.game.WinDialog
import com.example.ui.screens.game.PauseSettingsDialog
import com.example.ui.screens.game.TimeUpDialog
import com.example.ui.screens.game.TutorialOverlay

@Composable
fun GameScreen(
    levelId: Int, gridSize: Int, par: Int, movesCount: Int,
    board: Array<IntArray>?, winState: Boolean, starredScore: Int,
    highlightedCell: Pair<Int, Int>?, userStats: UserStats?,
    secondsElapsed: Int, hintsUsedThisLevel: Int, bestTimeSeconds: Int,
    previousBestMoves: Int = 0,
    onCellTapped: (Int, Int) -> Unit, onUndo: () -> Unit, onReset: () -> Unit,
    onHint: () -> Unit, onSkip: () -> Unit,
    onToggleSound: (Boolean) -> Unit, onToggleHaptic: (Boolean) -> Unit,
    onNextLevel: () -> Unit, onEarnReward: (String, Int) -> Unit, onBack: () -> Unit,
    isDailyChallenge: Boolean = false,
    difficultyMode: GameViewModel.Difficulty = GameViewModel.Difficulty.ZEN,
    countdownSecondsLeft: Int? = null, isTimeUp: Boolean = false,
    onChangeDifficulty: ((GameViewModel.Difficulty) -> Unit)? = null,
    onShowRewardedAd: (String, (Int) -> Unit) -> Unit,
    onShowInterstitialAd: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPauseSettings by remember { mutableStateOf(false) }
    var tutorialActive by remember { mutableStateOf(levelId == 1 && !isDailyChallenge) }
    var tutorialStep by remember { mutableStateOf(1) }
    val progress = remember(board) { if (board != null) GameRules.calculateProgress(board) else 0f }
    val progressColor = lerp(ActionNeonCoral, ActionNeonMint, progress)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AdmobBanner(modifier = Modifier.navigationBarsPadding()) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ─── TOP BAR ───
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, Modifier.size(40.dp).testTag("back_button")) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        if (isDailyChallenge) "DAILY · ${formatDateKey(levelId)}" else "STAGE $levelId",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("stage_title")
                    )
                }
                IconButton(onClick = { showPauseSettings = true }, Modifier.size(40.dp).testTag("settings_menu_button")) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── STATS ROW with gradient bg ───
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(SurfaceGradientStart, SurfaceGradientEnd)))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCell("MOVES", "$movesCount", Color.White)
                StatCell("BALANCE", "${(progress * 100).toInt()}%", progressColor)
                StatCell("PAR", "$par", AccentGold)
            }

            // ─── PROGRESS BAR ───
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface,
            )

            // ─── TIMER ROW ───
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val isMaster = difficultyMode == GameViewModel.Difficulty.MASTER
                Text(
                    if (isMaster) "⏱ ${formatTime(countdownSecondsLeft ?: 0)}" else "⏱ ${formatTime(secondsElapsed)}",
                    color = if (isMaster && (countdownSecondsLeft ?: 99) <= 15) ActionNeonCoral else SlateMutedText,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
                if (bestTimeSeconds > 0) Text("Best: ${formatTime(bestTimeSeconds)}", color = AccentGold.copy(0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.weight(0.04f))

            // ─── GAME GRID ───
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (board != null) {
                    Column(Modifier.fillMaxSize()) {
                        for (r in 0 until gridSize) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0 until gridSize) {
                                    val value = board[r][c]
                                    val isHighlight = tutorialActive && tutorialStep == 2 && r == 1 && c == 1
                                    Box(Modifier.weight(1f).semantics { contentDescription = "Row ${r + 1}, Column ${c + 1}, value $value" }) {
                                        AnimatedTile(value, progress, (highlightedCell == Pair(r, c)) || isHighlight, onTap = {
                                            if (tutorialActive) { if (tutorialStep == 2 && r == 1 && c == 1) { onCellTapped(r, c); tutorialStep = 3 } }
                                            else onCellTapped(r, c)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.04f))

            // ─── ACTION BUTTONS (pill style, inline icon+text+count) ───
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val undoLeft = userStats?.undos ?: 5
                val hintLeft = userStats?.hints ?: 5
                val isZen = difficultyMode == GameViewModel.Difficulty.ZEN

                PillButton(Icons.Default.ArrowBack, "UNDO", if (undoLeft > 0) "($undoLeft)" else "▶+3", ActionNeonCyan, Modifier.weight(1f), "undo_button") {
                    if (undoLeft > 0) onUndo() else onShowRewardedAd("undo") { onEarnReward("undo", it) }
                }
                PillButton(Icons.Default.Refresh, "RESET", "", ActionNeonCoral, Modifier.weight(1f), "reset_button") { onReset() }
                PillButton(Icons.Default.Star, "HINT", if (isZen) "(∞)" else if (hintLeft > 0) "($hintLeft)" else "▶+3", AccentGold, Modifier.weight(1f), "hint_button") {
                    if (isZen) onHint() else { if (hintLeft > 0) onHint() else onShowRewardedAd("hint") { onEarnReward("hint", it) } }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    // ─── DIALOGS ───
    var showWinDialog by remember { mutableStateOf(false) }
    LaunchedEffect(winState) { if (winState) { kotlinx.coroutines.delay(4000L); showWinDialog = true } else showWinDialog = false }

    if (showWinDialog) {
        WinDialog(starredScore, movesCount, par, levelId, isDailyChallenge, previousBestMoves, onNextLevel, onReset, onBack, onShowInterstitialAd, { cb -> onShowRewardedAd("hint") { _ -> cb(0) } })
    }
    if (showPauseSettings) {
        PauseSettingsDialog(userStats, difficultyMode, onReset, onChangeDifficulty, onToggleSound, onToggleHaptic) { showPauseSettings = false }
    }
    if (isTimeUp && difficultyMode == GameViewModel.Difficulty.MASTER) {
        TimeUpDialog(onReset, onBack) { onShowRewardedAd("time") { _ -> onEarnReward("time", 30) } }
    }
    if (tutorialActive) {
        TutorialOverlay(tutorialStep, { tutorialStep = 2 }, { tutorialActive = false }, { tutorialActive = false })
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SlateMutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, count: String, accent: Color, modifier: Modifier, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp).testTag(tag),
        shape = RoundedCornerShape(27.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, label, Modifier.size(14.dp), tint = accent)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
            if (count.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(count, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }
}

private fun formatDateKey(dateKey: Int): String {
    if (dateKey < 10000000) return dateKey.toString()
    val y = dateKey / 10000; val m = (dateKey % 10000) / 100; val d = dateKey % 100
    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${if (m in 1..12) months[m] else "$m"} $d, $y"
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60; val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
