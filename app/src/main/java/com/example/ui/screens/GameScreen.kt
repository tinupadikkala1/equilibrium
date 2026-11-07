package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.database.UserStats
import com.example.data.engine.GameRules
import com.example.ui.components.*
import com.example.ui.screens.game.*
import com.example.ui.theme.LocalGamePalette

@Composable
fun GameScreen(
    levelId: Int, par: Int, movesCount: Int, board: Array<IntArray>?, winState: Boolean,
    starredScore: Int, highlightedCell: Pair<Int, Int>?, userStats: UserStats?,
    secondsElapsed: Int, bestTimeSeconds: Int, previousBestMoves: Int,
    countdownSecondsLeft: Int, isTimeUp: Boolean, canUndo: Boolean, busy: Boolean,
    onCellTapped: (Int, Int) -> Unit, onUndo: () -> Unit, onReset: () -> Unit, onHint: () -> Unit,
    onSkip: () -> Unit, onNextLevel: () -> Unit, onBack: () -> Unit,
    onToggleSound: (Boolean) -> Unit, onToggleMusic: (Boolean) -> Unit, onToggleHaptic: (Boolean) -> Unit,
    onSwitchTheme: (String) -> Unit, onTutorialComplete: () -> Unit, onOverlayChanged: (Boolean) -> Unit,
    onReward: (String, () -> Unit) -> Unit, onInterstitial: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier, banner: @Composable () -> Unit = { AdmobBanner() },
) {
    val p = LocalGamePalette.current
    var pause by rememberSaveable { mutableStateOf(false) }
    var restart by rememberSaveable { mutableStateOf(false) }
    var skip by rememberSaveable { mutableStateOf(false) }
    var reward by rememberSaveable { mutableStateOf<String?>(null) }
    var adBusy by remember { mutableStateOf(false) }
    var demo by rememberSaveable { mutableStateOf(false) }
    var tutorialDismissed by rememberSaveable { mutableStateOf(false) }
    val tutorial = userStats != null && !userStats.tutorialCompleted && !tutorialDismissed
    val overlay = pause || restart || skip || reward != null || adBusy || demo || tutorial
    LaunchedEffect(overlay, winState, isTimeUp) { onOverlayChanged(overlay || winState || isTimeUp) }
    DisposableEffect(Unit) { onDispose { onOverlayChanged(false) } }
    BackHandler { pause = true }
    val progress = remember(board) { board?.let(GameRules::calculateProgress) ?: 0f }
    val actionsEnabled = !winState && !isTimeUp && !busy
    GameBackdrop(modifier.fillMaxSize().testTag("game_screen")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GameIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { pause = true }, Modifier.testTag("back_button"))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("Beat the clock")
                        Text("Stage $levelId", color = p.ink, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("stage_title"))
                    }
                    GameIconButton(Icons.Default.Settings, "Pause and settings", { pause = true }, Modifier.testTag("settings_menu_button"))
                }
                Spacer(Modifier.height(20.dp))
                GamePanel(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("Moves", "$movesCount", Modifier.weight(1f))
                        Stat("Balance", "${(progress * 100).toInt()}%", Modifier.weight(1f))
                        Stat("Par", "$par", Modifier.weight(1f))
                    }
                }
                Row(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(countdownSecondsLeft), style = MaterialTheme.typography.titleMedium, color = if (countdownSecondsLeft <= 15) p.danger else p.ink, modifier = Modifier.testTag("countdown"))
                    Text(if (bestTimeSeconds > 0) "Best ${formatTime(bestTimeSeconds)}" else "Match every number", style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
                if (board != null) PuzzleBoard(board, highlightedCell, actionsEnabled && !overlay, onCellTapped, Modifier.widthIn(max = 520.dp).fillMaxWidth().testTag("puzzle_board"))
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().height(5.dp), color = if (p.dark) p.accent else p.violet, trackColor = p.panelRaised)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val undos = userStats?.undos ?: 0
                    val hints = userStats?.hints ?: 0
                    GameButton("Undo", { if (undos > 0) onUndo() else reward = "undo" }, Modifier.weight(1f).testTag("undo_button"),
                        Icons.AutoMirrored.Filled.ArrowBack, ActionStyle.SECONDARY, actionsEnabled && canUndo,
                        if (undos > 0) "$undos left" else "Ad · +3", stacked = true)
                    GameButton("Restart", { restart = true }, Modifier.weight(1f).testTag("reset_button"),
                        Icons.Default.Refresh, ActionStyle.SECONDARY, actionsEnabled, "Start fresh", stacked = true)
                    GameButton("Hint", { if (hints > 0) onHint() else reward = "hint" }, Modifier.weight(1f).testTag("hint_button"),
                        Icons.Default.Star, ActionStyle.GOLD, actionsEnabled,
                        if (hints > 0) "$hints left" else "Ad · +3", stacked = true)
                }
                Spacer(Modifier.height(8.dp))
                Text(if (highlightedCell != null) "Try the highlighted tile." else "Tap −1. Neighbors +1. Find your balance.", color = p.muted, style = MaterialTheme.typography.bodySmall)
            }
            banner()
        }
    }
    if (pause) PauseSettingsDialog(userStats, onToggleSound, onToggleMusic, onToggleHaptic, onSwitchTheme,
        onReset = { pause = false; restart = true },
        onSkip = { pause = false; skip = true },
        onDemo = { pause = false; demo = true },
        onBack = { pause = false; onBack() }, onDismiss = { pause = false })
    if (restart) ConfirmGameDialog("restart_dialog", "A fresh start?", "The board and timer will reset. Your tokens stay with you.",
        "Restart stage", { restart = false; onReset() }, { restart = false })
    if (skip) ConfirmGameDialog("skip_dialog", "Move to the next stage?", "Use one skip token. This stage is recorded with one star.",
        if ((userStats?.skips ?: 0) > 0) "Use skip token" else "Earn skip tokens",
        { skip = false; if ((userStats?.skips ?: 0) > 0) onSkip() else reward = "skip" }, { skip = false })
    if (tutorial) TutorialOverlay(onFinish = { tutorialDismissed = true; onTutorialComplete() })
    if (demo) DemoDialog { demo = false }
    if (isTimeUp && !overlay) TimeUpDialog(onReset, onBack) { reward = "time" }
    if (winState && !overlay) WinDialog(starredScore, movesCount, par, levelId, previousBestMoves, secondsElapsed,
        onNextLevel = {
            if (levelId % 5 == 0) { adBusy = true; onInterstitial { adBusy = false; onNextLevel() } }
            else onNextLevel()
        }, onReplay = onReset, onBack = onBack,
        onReward = { reward = "hint" })
    reward?.let { type ->
        RewardDialog(type, adBusy, onWatch = {
            adBusy = true
            onReward(type) { adBusy = false; reward = null }
        }, onDismiss = { if (!adBusy) reward = null })
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label)
        Text(value, style = MaterialTheme.typography.titleLarge, color = LocalGamePalette.current.ink)
    }
}

@Composable
private fun ConfirmGameDialog(tag: String, title: String, description: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalGamePalette.current
    GameDialog(tag, onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = p.ink)
        Text(description, color = p.muted)
        GameButton(action, onConfirm, Modifier.fillMaxWidth().testTag("confirm_action"))
        GameButton("Keep playing", onDismiss, Modifier.fillMaxWidth(), style = ActionStyle.SECONDARY)
    }
}
