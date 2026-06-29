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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.LevelProgress
import com.example.data.database.UserStats
import com.example.data.database.DailyChallengeProgress
import com.example.ui.components.AdmobBanner
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonMint
import com.example.ui.theme.ActionNeonCoral
import com.example.ui.theme.SlateMutedText

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
    var showSettingsDialog by remember { mutableStateOf(false) }

    val highestCompletedLevel = levelProgressList.filter { it.completed }.maxOfOrNull { it.levelId } ?: 0
    val highestUnlockedLevel = highestCompletedLevel + 1
    val totalStars = levelProgressList.sumOf { it.stars }
    val streakCount = userStats?.currentStreak ?: 0

    val todayDateKey = remember {
        val cal = java.util.Calendar.getInstance()
        cal.get(java.util.Calendar.YEAR) * 10000 + (cal.get(java.util.Calendar.MONTH) + 1) * 100 + cal.get(java.util.Calendar.DAY_OF_MONTH)
    }
    val dailyCompleted = dailyProgressList.any { it.dateKey == todayDateKey && it.completed }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AdmobBanner(modifier = Modifier.navigationBarsPadding()) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ─── HEADER ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EQUILIBRIUM",
                        color = ActionNeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "One tap. Infinite ripples.",
                        color = SlateMutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── STATS PANEL ───
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("⭐", "$totalStars", "Stars")
                    StatColumn("🏆", "$highestCompletedLevel", "Cleared")
                    StatColumn("🔥", "$streakCount", "Streak")
                    StatColumn("💡", "${userStats?.hints ?: 5}", "Hints")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── RESUME GAME BANNER ───
            if (activeGameSave != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ActionNeonMint.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = ActionNeonMint.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().clickable { onResumeGame() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, "Resume", tint = ActionNeonMint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("CONTINUE GAME", color = ActionNeonMint, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("Stage ${activeGameSave.levelId} · ${activeGameSave.movesCount} moves in", color = SlateMutedText, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = ActionNeonMint)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ─── DAILY CHALLENGE ───
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable { onSelectDailyLevel(todayDateKey) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DAILY CHALLENGE", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            if (dailyCompleted) "✓ Completed today" else "New puzzle available!",
                            color = if (dailyCompleted) ActionNeonMint else SlateMutedText,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = AccentGold.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── LEVEL SELECT HEADER ───
            Text(
                text = "SELECT STAGE",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ─── LEVEL GRID ───
            val totalLevelsToShow = highestUnlockedLevel + 20

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(totalLevelsToShow) { index ->
                    val levelNum = index + 1
                    val isLocked = levelNum > highestUnlockedLevel
                    val progress = levelProgressList.find { it.levelId == levelNum }
                    val starsCount = progress?.stars ?: 0

                    LevelSelectItem(
                        levelId = levelNum,
                        stars = starsCount,
                        isLocked = isLocked,
                        onClick = { if (!isLocked) onSelectLevel(levelNum) }
                    )
                }
            }
        }
    }

    // ─── SETTINGS DIALOG ───
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, ActionNeonCyan),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("SETTINGS", color = ActionNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Sound
                    SettingRow("SOUND EFFECTS", userStats?.soundEnabled != false) { onToggleSound(it) }
                    SettingRow("MUSIC", userStats?.musicEnabled != false) { onToggleMusic(it) }
                    SettingRow("HAPTIC FEEDBACK", userStats?.hapticEnabled != false) { onToggleHaptic(it) }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Themes
                    Text("THEMES", color = SlateMutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val themes = listOf(
                            "Neon Pulse" to ActionNeonCyan,
                            "Pastel Breeze" to Color(0xFF81ECEC),
                            "Minimal Mono" to Color(0xFF00FF00),
                            "Midnight" to Color(0xFF4D38EC),
                            "Forest" to Color(0xFF2EA671),
                            "Sunset" to Color(0xFFFF5E62)
                        )
                        themes.forEach { (name, color) ->
                            val isSelected = userStats?.themeName == name
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onSwitchTheme(name) }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Privacy Policy
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://your-privacy-policy-url.com"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Privacy Policy", color = SlateMutedText, fontSize = 12.sp)
                    }

                    // Reset
                    var showConfirmReset by remember { mutableStateOf(false) }
                    if (!showConfirmReset) {
                        TextButton(onClick = { showConfirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("RESET ALL PROGRESSION", color = ActionNeonCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("This will permanently wipe all progress!", color = ActionNeonCoral, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showConfirmReset = false }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Text("Cancel", color = Color.White, fontSize = 11.sp)
                            }
                            Button(onClick = { onResetAllData(); showSettingsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Text("Yes, Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(label, color = SlateMutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LevelSelectItem(levelId: Int, stars: Int, isLocked: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        isLocked -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        stars == 3 -> ActionNeonMint.copy(alpha = 0.12f)
        stars > 0 -> ActionNeonCyan.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        stars == 3 -> ActionNeonMint.copy(alpha = 0.5f)
        stars > 0 -> ActionNeonCyan.copy(alpha = 0.3f)
        isLocked -> Color.White.copy(alpha = 0.03f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = !isLocked, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLocked) {
                Icon(Icons.Default.Lock, null, tint = SlateMutedText.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$levelId",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (stars > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(3) { i ->
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = if (i < stars) AccentGold else Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = ActionNeonCyan)
        )
    }
}
