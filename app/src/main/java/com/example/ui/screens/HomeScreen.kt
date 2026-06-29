package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.LevelProgress
import com.example.data.database.UserStats
import com.example.data.database.DailyChallengeProgress
import com.example.data.engine.LevelGenerator
import com.example.ui.components.AdmobBanner
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    userStats: UserStats?,
    levelProgressList: List<LevelProgress>,
    dailyProgressList: List<DailyChallengeProgress>,
    activeGameSave: com.example.data.database.ActiveGameSave?,
    allHistory: List<com.example.data.database.LevelAttemptHistory>,
    onSelectLevel: (Int) -> Unit,
    onSelectDailyLevel: (Int) -> Unit,
    onResumeGame: () -> Unit,
    onSwitchTheme: (String) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val highestCompleted = levelProgressList.filter { it.completed }.maxOfOrNull { it.levelId } ?: 0
    val highestUnlocked = highestCompleted + 1
    val totalStars = levelProgressList.sumOf { it.stars }
    val streak = userStats?.currentStreak ?: 1
    val hintCount = userStats?.hints ?: 5
    val todayKey = remember { val c = java.util.Calendar.getInstance(); c.get(java.util.Calendar.YEAR) * 10000 + (c.get(java.util.Calendar.MONTH) + 1) * 100 + c.get(java.util.Calendar.DAY_OF_MONTH) }
    val dailyLevel = remember(todayKey) { LevelGenerator.generateDailyLevel(todayKey) }

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background, bottomBar = { AdmobBanner(modifier = Modifier.navigationBarsPadding()) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {

            // ─── HEADER ───
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("EQUILIBRIUM", color = ActionNeonCyan, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    Text("One Tap. Infinite Ripples.", color = SlateMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceGradientStart, border = BorderStroke(1.dp, AccentGold.copy(0.3f))) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔥", fontSize = 12.sp)
                            Text("${streak}D", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                    IconButton(onClick = { showSettings = true }, Modifier.size(38.dp)) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ─── STATS CARD ───
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(SurfaceGradientStart, SurfaceGradientEnd))).padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⭐", fontSize = 24.sp)
                        Column { Text("TOTAL STARS", color = ActionNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text("$totalStars", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.08f)))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Info, null, tint = ActionNeonCyan, modifier = Modifier.size(24.dp))
                        Column { Text("HINT TOKENS", color = ActionNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text("$hintCount", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── RESUME BANNER ───
            if (activeGameSave != null) {
                Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ActionNeonMint.copy(0.4f)), colors = CardDefaults.cardColors(containerColor = ActionNeonMint.copy(0.06f)), modifier = Modifier.fillMaxWidth().clickable { onResumeGame() }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = ActionNeonMint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("CONTINUE", color = ActionNeonMint, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace); Text("Stage ${activeGameSave.levelId} · ${activeGameSave.movesCount} moves", color = SlateMutedText, fontSize = 10.sp) }
                        Icon(Icons.Default.ArrowForward, null, tint = ActionNeonMint.copy(0.5f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ─── SKIN SELECTION ───
            Text("SKINS", color = SlateMutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Neon Pulse" to ActionNeonCyan, "Pastel Breeze" to PastelCyan, "Minimal Mono" to MonoAccent, "Midnight" to MidnightPrimary, "Forest" to ForestPrimary, "Sunset" to SunsetPrimary).forEach { (name, color) ->
                    val sel = userStats?.themeName == name
                    Surface(shape = RoundedCornerShape(10.dp), color = if (sel) color.copy(0.12f) else SurfaceGradientStart, border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) color else Color.White.copy(0.06f)), modifier = Modifier.width(100.dp).clickable { onSwitchTheme(name) }) {
                        Column(Modifier.padding(10.dp)) { Text(name, color = if (sel) color else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── DAILY EVENT ───
            Card(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ActionNeonCyan.copy(0.4f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth().clickable { onSelectDailyLevel(todayKey) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text("DAILY CHALLENGE", color = ActionNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace); Text("${dailyLevel.size}×${dailyLevel.size} grid · Par ${dailyLevel.par}", color = SlateMutedText, fontSize = 10.sp) }
                    Surface(shape = RoundedCornerShape(6.dp), color = ActionNeonCyan.copy(0.12f)) { Text("PLAY", color = ActionNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ─── PUZZLE STAGES ───
            Text("STAGES", color = SlateMutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 6.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(highestUnlocked + 20) { i ->
                    val num = i + 1; val locked = num > highestUnlocked; val prog = levelProgressList.find { it.levelId == num }; val stars = prog?.stars ?: 0; val isCurrent = num == highestUnlocked
                    val bg = when { isCurrent -> ActionNeonMint.copy(0.08f); stars == 3 -> ActionNeonMint.copy(0.05f); stars > 0 -> ActionNeonCyan.copy(0.05f); else -> SurfaceGradientStart }
                    val bdr = when { isCurrent -> ActionNeonMint.copy(0.5f); stars == 3 -> ActionNeonMint.copy(0.2f); locked -> Color.White.copy(0.03f); else -> Color.White.copy(0.08f) }
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, bdr, RoundedCornerShape(12.dp)).clickable(enabled = !locked) { onSelectLevel(num) }, contentAlignment = Alignment.Center) {
                        if (locked) Icon(Icons.Default.Lock, null, tint = SlateMutedText.copy(0.3f), modifier = Modifier.size(16.dp))
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$num", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            if (stars > 0) Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 8.sp, color = AccentGold)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) SettingsSheet(userStats, onToggleSound, onToggleMusic, onToggleHaptic, onSwitchTheme, onResetAllData) { showSettings = false }
}

@Composable
private fun SettingsSheet(userStats: UserStats?, onSound: (Boolean) -> Unit, onMusic: (Boolean) -> Unit, onHaptic: (Boolean) -> Unit, onTheme: (String) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var showDemo by remember { mutableStateOf(false) }
    if (showDemo) com.example.ui.screens.game.DemoDialog { showDemo = false }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, ActionNeonCyan.copy(0.3f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SETTINGS", color = ActionNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                HorizontalDivider(color = Color.White.copy(0.06f))
                Toggle("Sound", userStats?.soundEnabled != false, onSound)
                Toggle("Music", userStats?.musicEnabled != false, onMusic)
                Toggle("Haptics", userStats?.hapticEnabled != false, onHaptic)
                HorizontalDivider(color = Color.White.copy(0.06f))
                // Themes
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Neon Pulse" to ActionNeonCyan, "Pastel Breeze" to PastelCyan, "Minimal Mono" to MonoAccent, "Midnight" to MidnightPrimary, "Forest" to ForestPrimary, "Sunset" to SunsetPrimary).forEach { (n, c) ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(c).border(if (userStats?.themeName == n) 2.dp else 0.dp, Color.White, CircleShape).clickable { onTheme(n) })
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.06f))
                // Demo
                OutlinedButton(onClick = { showDemo = true }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ActionNeonCyan.copy(0.3f)), modifier = Modifier.fillMaxWidth()) { Text("▶  DEMO", color = ActionNeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                // Privacy
                TextButton(onClick = { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://your-privacy-policy-url.com"))) }, Modifier.fillMaxWidth()) { Text("Privacy Policy", color = SlateMutedText, fontSize = 11.sp) }
                // Reset
                var confirm by remember { mutableStateOf(false) }
                if (!confirm) TextButton(onClick = { confirm = true }, Modifier.fillMaxWidth()) { Text("Reset Progress", color = ActionNeonCoral, fontSize = 11.sp) }
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { confirm = false }, Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel", color = Color.White, fontSize = 11.sp) }
                    Button(onClick = { onReset(); onDismiss() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral), shape = RoundedCornerShape(8.dp)) { Text("Reset", fontSize = 11.sp) }
                }
                // Close
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) { Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = ActionNeonCyan))
    }
}
