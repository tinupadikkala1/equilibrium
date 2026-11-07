package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.*
import com.example.ui.components.*
import com.example.ui.screens.game.DemoDialog
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    userStats: UserStats?, levelProgressList: List<LevelProgress>, activeGameSave: ActiveGameSave?,
    allHistory: List<LevelAttemptHistory>, onSelectLevel: (Int) -> Unit, onResumeGame: () -> Unit,
    onSwitchTheme: (String) -> Unit, onToggleSound: (Boolean) -> Unit, onToggleMusic: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit, onResetAllData: () -> Unit, modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = { AdmobBanner() },
) {
    val p = LocalGamePalette.current
    var settings by rememberSaveable { mutableStateOf(false) }
    var demo by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    val highest = (levelProgressList.filter { it.completed }.maxOfOrNull { it.levelId } ?: 0) + 1
    val progressMap = remember(levelProgressList) { levelProgressList.associateBy { it.levelId } }
    val columns = if (LocalDensity.current.fontScale > 1.3f) 3 else 4
    val selected = activeGameSave?.levelId ?: highest
    GameBackdrop(modifier.fillMaxSize().testTag("home_screen")) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Equilibrium", style = MaterialTheme.typography.titleLarge, color = p.ink)
                            Text("One tap. Infinite ripples.", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        }
                        GameIconButton(Icons.Default.Settings, "Settings", { settings = true }, Modifier.testTag("home_settings"))
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GamePanel(Modifier.fillMaxWidth().padding(top = 12.dp), accent = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Eyebrow(if (activeGameSave != null) "Pick up your rhythm" else "Your next challenge")
                                Spacer(Modifier.height(6.dp))
                                Text("Stage $selected", style = MaterialTheme.typography.displaySmall, color = p.ink)
                                Spacer(Modifier.height(7.dp))
                                Text(if (activeGameSave != null) "${activeGameSave.movesCount} moves · ${formatTime(activeGameSave.countdownSecondsLeft)} left" else "Find balance. Beat the clock.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            }
                            BalanceEmblem()
                        }
                        Spacer(Modifier.height(20.dp))
                        GameButton(if (activeGameSave != null) "Continue puzzle" else "Let's play",
                            { if (activeGameSave != null) onResumeGame() else onSelectLevel(highest) },
                            Modifier.fillMaxWidth().testTag("home_play"), icon = Icons.AutoMirrored.Filled.ArrowForward)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeStat("Stars earned", levelProgressList.sumOf { it.stars }.toString(), Icons.Default.Star, Modifier.weight(1f))
                        HomeStat("Day streak", (userStats?.currentStreak ?: 1).toString(), Icons.Default.DateRange, Modifier.weight(1f))
                        HomeStat("Hints", (userStats?.hints ?: 5).toString(), Icons.Default.Info, Modifier.weight(1f))
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your journey", style = MaterialTheme.typography.titleLarge, color = p.ink)
                            Text("Every move brings you closer.", style = MaterialTheme.typography.bodySmall, color = p.muted)
                        }
                        TextButton(onClick = { history = true }, Modifier.testTag("history_button")) { Text("History", color = p.ink) }
                    }
                }
                items(highest + 19, key = { it + 1 }) { index ->
                    val id = index + 1
                    val locked = id > highest
                    val current = id == highest
                    val stars = progressMap[id]?.stars ?: 0
                    Surface(
                        onClick = { onSelectLevel(id) }, enabled = !locked,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp).testTag("stage_$id")
                            .semantics { contentDescription = if (locked) "Stage $id, locked" else "Stage $id, $stars stars" },
                        shape = RoundedCornerShape(if (p.dark) 20.dp else 17.dp),
                        color = if (current) p.accent else p.panel,
                        border = BorderStroke(if (p.dark) 1.dp else 2.dp, if (locked) p.line.copy(alpha = .4f) else p.line),
                        shadowElevation = if (p.dark && current) 8.dp else 0.dp,
                    ) {
                        Column(Modifier.padding(vertical = 15.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (locked) Icon(Icons.Default.Lock, null, Modifier.size(21.dp), tint = p.muted)
                            else {
                                Text("$id", style = MaterialTheme.typography.titleLarge, color = if (current) p.onAccent else p.ink)
                                if (stars > 0) Text("★".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.labelSmall, color = if (p.dark) p.gold else p.ink)
                                else Text("PLAY", style = MaterialTheme.typography.labelSmall, color = if (current) p.onAccent else p.muted)
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GameButton("How to play", { demo = true }, Modifier.fillMaxWidth().padding(top = 8.dp).testTag("home_demo"), Icons.Default.PlayArrow, ActionStyle.SECONDARY)
                }
            }
            banner()
        }
    }
    if (settings) SettingsDialog(userStats, onSwitchTheme, onToggleSound, onToggleMusic, onToggleHaptic,
        onDemo = { settings = false; demo = true }, onReset = onResetAllData, onDismiss = { settings = false })
    if (demo) DemoDialog { demo = false }
    if (history) GameDialog("history_dialog", { history = false }) {
        Eyebrow("Your progress")
        Text("Every attempt counts", style = MaterialTheme.typography.headlineMedium, color = p.ink)
        if (allHistory.isEmpty()) Text("Finish a stage to see your recent solves here.", color = p.muted)
        allHistory.filter { !it.isDaily }.take(12).forEach { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Stage ${entry.levelId}", color = p.ink, style = MaterialTheme.typography.titleMedium)
                    Text("${entry.movesTaken} moves · ${formatTime(entry.timeTakenSeconds)}", color = p.muted, style = MaterialTheme.typography.bodySmall) }
                Text("★".repeat(entry.stars.coerceIn(0, 3)), color = if (p.dark) p.gold else p.ink)
            }
        }
        GameButton("Back to journey", { history = false }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun HomeStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val p = LocalGamePalette.current
    Column(modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (p.dark) p.violet else p.violet)
        Text(value, style = MaterialTheme.typography.titleLarge, color = p.ink, modifier = Modifier.padding(top = 4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.muted)
    }
}
