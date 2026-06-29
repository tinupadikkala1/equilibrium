package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.UserStats
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel

@Composable
fun PauseSettingsDialog(userStats: UserStats?, difficultyMode: GameViewModel.Difficulty, onReset: () -> Unit, onChangeDifficulty: ((GameViewModel.Difficulty) -> Unit)?, onToggleSound: (Boolean) -> Unit, onToggleHaptic: (Boolean) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, ActionNeonCyan.copy(0.3f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth(0.92f).testTag("settings_dialog")) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SETTINGS", color = ActionNeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                HorizontalDivider(color = Color.White.copy(0.06f))
                // Reset
                Button(onClick = { onReset(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(44.dp).testTag("dialog_reset_level_button")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp)); Text("RESET LEVEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                }
                // Difficulty
                Text("DIFFICULTY", color = SlateMutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.Start))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onChangeDifficulty?.invoke(GameViewModel.Difficulty.ZEN) }, border = BorderStroke(1.dp, if (difficultyMode == GameViewModel.Difficulty.ZEN) ActionNeonCyan else Color.White.copy(0.1f)), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (difficultyMode == GameViewModel.Difficulty.ZEN) ActionNeonCyan.copy(0.15f) else Color.Transparent), modifier = Modifier.weight(1f).height(38.dp)) { Text("ZEN", color = if (difficultyMode == GameViewModel.Difficulty.ZEN) ActionNeonCyan else Color.White.copy(0.6f), fontWeight = FontWeight.Black, fontSize = 11.sp) }
                    OutlinedButton(onClick = { onChangeDifficulty?.invoke(GameViewModel.Difficulty.MASTER) }, border = BorderStroke(1.dp, if (difficultyMode == GameViewModel.Difficulty.MASTER) ActionNeonCoral else Color.White.copy(0.1f)), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (difficultyMode == GameViewModel.Difficulty.MASTER) ActionNeonCoral.copy(0.15f) else Color.Transparent), modifier = Modifier.weight(1f).height(38.dp)) { Text("MASTER", color = if (difficultyMode == GameViewModel.Difficulty.MASTER) ActionNeonCoral else Color.White.copy(0.6f), fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
                // Toggles
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Sound", color = Color.White, fontSize = 12.sp); Switch(userStats?.soundEnabled != false, { onToggleSound(it) }, colors = SwitchDefaults.colors(checkedTrackColor = ActionNeonCyan, checkedThumbColor = Color.Black)) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Haptics", color = Color.White, fontSize = 12.sp); Switch(userStats?.hapticEnabled != false, { onToggleHaptic(it) }, colors = SwitchDefaults.colors(checkedTrackColor = ActionNeonCyan, checkedThumbColor = Color.Black)) }
                // Resume
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) { Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}
