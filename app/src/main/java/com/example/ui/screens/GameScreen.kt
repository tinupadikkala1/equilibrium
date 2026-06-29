package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonMint
import com.example.ui.theme.ActionNeonCoral
import com.example.ui.theme.SlateMutedText
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.screens.game.WinDialog
import com.example.ui.screens.game.PauseSettingsDialog
import com.example.ui.screens.game.TimeUpDialog
import com.example.ui.screens.game.TutorialOverlay

@Composable
fun GameScreen(
    levelId: Int,
    gridSize: Int,
    par: Int,
    movesCount: Int,
    board: Array<IntArray>?,
    winState: Boolean,
    starredScore: Int,
    highlightedCell: Pair<Int, Int>?,
    userStats: UserStats?,
    secondsElapsed: Int,
    hintsUsedThisLevel: Int,
    bestTimeSeconds: Int,
    previousBestMoves: Int = 0,
    onCellTapped: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onHint: () -> Unit,
    onSkip: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onNextLevel: () -> Unit,
    onEarnReward: (String, Int) -> Unit,
    onBack: () -> Unit,
    isDailyChallenge: Boolean = false,
    difficultyMode: GameViewModel.Difficulty = GameViewModel.Difficulty.ZEN,
    countdownSecondsLeft: Int? = null,
    isTimeUp: Boolean = false,
    onChangeDifficulty: ((GameViewModel.Difficulty) -> Unit)? = null,
    onShowRewardedAd: (String, (Int) -> Unit) -> Unit,
    onShowInterstitialAd: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPauseSettings by remember { mutableStateOf(false) }
    var tutorialActive by remember { mutableStateOf(levelId == 1 && !isDailyChallenge) }
    var tutorialStep by remember { mutableStateOf(1) }

    val progress = remember(board) {
        if (board != null) GameRules.calculateProgress(board) else 0.0f
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AdmobBanner(modifier = Modifier.navigationBarsPadding()) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── TOP BAR: Back + Title + Settings ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("back_button")) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDailyChallenge) "DAILY" else "STAGE $levelId",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("stage_title")
                    )
                    if (isDailyChallenge) {
                        Text(
                            text = formatDateKey(levelId),
                            color = SlateMutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(onClick = { showPauseSettings = true }, modifier = Modifier.size(40.dp).testTag("settings_menu_button")) {
                    Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── STATS ROW ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("MOVES", "$movesCount", MaterialTheme.colorScheme.onSurface)
                StatItem("BALANCE", "${(progress * 100).toInt()}%", if (progress >= 1.0f) ActionNeonMint else ActionNeonCyan)
                StatItem("PAR", "$par", AccentGold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── TIMER ROW ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isMaster = difficultyMode == GameViewModel.Difficulty.MASTER
                Text(
                    text = if (isMaster) "⏱ ${formatTime(countdownSecondsLeft ?: 0)}" else "⏱ ${formatTime(secondsElapsed)}",
                    color = if (isMaster && (countdownSecondsLeft ?: 99) <= 15) ActionNeonCoral else SlateMutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("game_timer")
                )
                Text(
                    text = if (bestTimeSeconds > 0) "Best: ${formatTime(bestTimeSeconds)}" else "",
                    color = AccentGold.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("best_time")
                )
            }

            Spacer(modifier = Modifier.weight(0.05f))

            // ─── GAME GRID ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (board != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (r in 0 until gridSize) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0 until gridSize) {
                                    val value = board[r][c]
                                    val isHighlight = tutorialActive && tutorialStep == 2 && r == 1 && c == 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { contentDescription = "Row ${r + 1}, Column ${c + 1}, value $value" }
                                    ) {
                                        AnimatedTile(
                                            value = value,
                                            progress = progress,
                                            isHintHighlighted = (highlightedCell == Pair(r, c)) || isHighlight,
                                            onTap = {
                                                if (tutorialActive) {
                                                    if (tutorialStep == 2 && r == 1 && c == 1) { onCellTapped(r, c); tutorialStep = 3 }
                                                } else { onCellTapped(r, c) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.05f))

            // ─── ACTION BUTTONS: Undo | Reset | Hint ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // UNDO
                val undoLeft = userStats?.undos ?: 5
                ActionButton(
                    label = "UNDO",
                    subtitle = if (undoLeft > 0) "$undoLeft" else "Ad",
                    icon = Icons.Default.ArrowBack,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "undo_button",
                    onClick = {
                        if (undoLeft > 0) onUndo()
                        else onShowRewardedAd("undo") { onEarnReward("undo", it) }
                    }
                )

                // RESET
                ActionButton(
                    label = "RESET",
                    subtitle = "",
                    icon = Icons.Default.Refresh,
                    accentColor = ActionNeonCoral,
                    modifier = Modifier.weight(1f),
                    testTag = "reset_button",
                    onClick = onReset
                )

                // HINT
                val hintLeft = userStats?.hints ?: 5
                val isZen = difficultyMode == GameViewModel.Difficulty.ZEN
                val limitReached = !isZen && hintsUsedThisLevel >= 2
                ActionButton(
                    label = "HINT",
                    subtitle = when {
                        isZen -> "Free"
                        limitReached -> "Max"
                        hintLeft > 0 -> "$hintLeft"
                        else -> "Ad +3"
                    },
                    icon = Icons.Default.Star,
                    accentColor = AccentGold,
                    enabled = isZen || !limitReached,
                    modifier = Modifier.weight(1f),
                    testTag = "hint_button",
                    onClick = {
                        if (isZen) onHint()
                        else if (!limitReached) {
                            if (hintLeft > 0) onHint()
                            else onShowRewardedAd("hint") { amount ->
                                onEarnReward("hint", amount)
                                onHint() // auto-use one hint after ad
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // ─── DIALOGS ───
    if (winState) {
        WinDialog(
            starredScore = starredScore, movesCount = movesCount, par = par,
            levelId = levelId, isDailyChallenge = isDailyChallenge,
            previousBestMoves = previousBestMoves,
            onNextLevel = onNextLevel, onReplay = onReset, onBack = onBack,
            onShowInterstitialAd = onShowInterstitialAd,
            onShowRewardedAd = { cb -> onShowRewardedAd("hint") { _ -> cb(0) } }
        )
    }
    if (showPauseSettings) {
        PauseSettingsDialog(
            userStats = userStats, difficultyMode = difficultyMode,
            onReset = onReset, onChangeDifficulty = onChangeDifficulty,
            onToggleSound = onToggleSound, onToggleHaptic = onToggleHaptic,
            onDismiss = { showPauseSettings = false }
        )
    }
    if (isTimeUp && difficultyMode == GameViewModel.Difficulty.MASTER) {
        TimeUpDialog(
            onRetry = onReset, onBack = onBack,
            onWatchAdForTime = { onShowRewardedAd("time") { _ -> onEarnReward("time", 30) } }
        )
    }
    if (tutorialActive) {
        TutorialOverlay(
            tutorialStep = tutorialStep,
            onNextStep = { tutorialStep = 2 },
            onFinish = { tutorialActive = false },
            onDismiss = { tutorialActive = false }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SlateMutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ActionButton(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp).testTag(testTag),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.3f else 0.1f)),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, modifier = Modifier.size(18.dp), tint = if (enabled) accentColor else Color.Gray)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (enabled) Color.White else Color.Gray)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 9.sp, color = if (enabled) accentColor.copy(alpha = 0.8f) else Color.Gray)
            }
        }
    }
}

private fun formatDateKey(dateKey: Int): String {
    if (dateKey < 10000000) return dateKey.toString()
    val y = dateKey / 10000
    val m = (dateKey % 10000) / 100
    val d = dateKey % 100
    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${if (m in 1..12) months[m] else "$m"} $d, $y"
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
