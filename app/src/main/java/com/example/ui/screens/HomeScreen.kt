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
import com.example.data.database.LevelProgress
import com.example.data.database.UserStats
import com.example.data.database.DailyChallengeProgress
import com.example.data.engine.LevelGenerator
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
    val streakCount = userStats?.currentStreak ?: 1
    val hintCount = userStats?.hints ?: 5

    val todayDateKey = remember {
        val cal = java.util.Calendar.getInstance()
        cal.get(java.util.Calendar.YEAR) * 10000 + (cal.get(java.util.Calendar.MONTH) + 1) * 100 + cal.get(java.util.Calendar.DAY_OF_MONTH)
    }
    val dailyLevel = remember(todayDateKey) { LevelGenerator.generateDailyLevel(todayDateKey) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AdmobBanner(modifier = Modifier.navigationBarsPadding()) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ─── HEADER: EQUILIBRIUM + Streak Badge + Settings ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EQUILIBRIUM",
                        color = ActionNeonCyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "One Tap. Infinite Ripples.",
                        color = SlateMutedText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⭐", fontSize = 14.sp)
                            Text("${streakCount}D STREAK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                    // Settings icon
                    IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── STATS CARD: Stars + Hints ───
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Stars
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⭐", fontSize = 28.sp)
                        Column {
                            Text("TOTAL STARS", color = ActionNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("$totalStars Stars", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.1f)))
                    // Hint Tokens
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ActionNeonCyan, modifier = Modifier.size(28.dp))
                        Column {
                            Text("HINT TOKENS", color = ActionNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("$hintCount Hints", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── SKIN SELECTION ───
            Text("SKIN SELECTION", color = SlateMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val themes = listOf(
                    Triple("Neon Pulse", "Default", ActionNeonCyan),
                    Triple("Pastel Breeze", "Soft Pastel", Color(0xFF55EFC4)),
                    Triple("Minimal Mono", "Retro Green", Color(0xFF00FF00)),
                    Triple("Midnight", "Electric Indigo", Color(0xFF4D38EC)),
                    Triple("Forest", "Deep Sage", Color(0xFF2EA671)),
                    Triple("Sunset", "Warm Horizon", Color(0xFFFF5E62))
                )
                themes.forEach { (name, subtitle, color) ->
                    val isSelected = userStats?.themeName == name
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color.White.copy(alpha = 0.08f)),
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface),
                        modifier = Modifier.width(140.dp).clickable { onSwitchTheme(name) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(subtitle, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── DAILY EVENT ───
            Text("DAILY EVENT", color = SlateMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, ActionNeonCyan),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📅", fontSize = 18.sp)
                            Text("DAILY CHALLENGE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = ActionNeonCyan.copy(alpha = 0.15f)) {
                            Text("LIVE TODAY", color = ActionNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "A brand new procedurally scrambled grid is live of dimensions ${dailyLevel.size}x${dailyLevel.size}. Standard rules apply—can you restore perfect harmony?",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onSelectDailyLevel(todayDateKey) },
                        colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("▶  PLAY EVENT", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── PUZZLE STAGES ───
            Text("PUZZLE STAGES", color = SlateMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val totalLevelsToShow = highestUnlockedLevel + 20
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(totalLevelsToShow) { index ->
                    val levelNum = index + 1
                    val isLocked = levelNum > highestUnlockedLevel
                    val progress = levelProgressList.find { it.levelId == levelNum }
                    val stars = progress?.stars ?: 0
                    val isCurrentLevel = levelNum == highestUnlockedLevel

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isCurrentLevel) 1.5.dp else 1.dp,
                            if (isCurrentLevel) ActionNeonMint else Color.White.copy(alpha = 0.06f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCurrentLevel -> ActionNeonMint.copy(alpha = 0.08f)
                                stars > 0 -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable(enabled = !isLocked) { onSelectLevel(levelNum) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isLocked) {
                                Icon(Icons.Default.Lock, null, tint = SlateMutedText.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$levelNum", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    if (stars > 0) {
                                        Text(
                                            "★".repeat(stars) + "☆".repeat(3 - stars),
                                            fontSize = 9.sp,
                                            color = AccentGold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── SETTINGS DIALOG ───
    if (showSettingsDialog) {
        SettingsDialog(
            userStats = userStats,
            onToggleSound = onToggleSound,
            onToggleMusic = onToggleMusic,
            onToggleHaptic = onToggleHaptic,
            onSwitchTheme = onSwitchTheme,
            onResetAllData = onResetAllData,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun SettingsDialog(
    userStats: UserStats?,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSwitchTheme: (String) -> Unit,
    onResetAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDemo by remember { mutableStateOf(false) }

    if (showDemo) {
        com.example.ui.screens.game.DemoDialog(onDismiss = { showDemo = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, ActionNeonCyan),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("SETTINGS", color = ActionNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                SettingToggle("SOUND EFFECTS", userStats?.soundEnabled != false, onToggleSound)
                SettingToggle("MUSIC", userStats?.musicEnabled != false, onToggleMusic)
                SettingToggle("HAPTIC FEEDBACK", userStats?.hapticEnabled != false, onToggleHaptic)

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Demo
                Button(
                    onClick = { showDemo = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("▶  DEMO — How to Play", color = ActionNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // Privacy Policy
                TextButton(onClick = {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://your-privacy-policy-url.com")))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Privacy Policy", color = SlateMutedText, fontSize = 12.sp)
                }

                // Reset
                var confirmReset by remember { mutableStateOf(false) }
                if (!confirmReset) {
                    TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("RESET ALL PROGRESSION", color = ActionNeonCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("This will permanently wipe all progress!", color = ActionNeonCoral, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirmReset = false }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = Color.White, fontSize = 11.sp)
                        }
                        Button(onClick = { onResetAllData(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Text("Yes, Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = ActionNeonCyan))
    }
}
