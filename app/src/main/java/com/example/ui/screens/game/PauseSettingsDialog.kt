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
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonCoral
import com.example.ui.theme.ActionNeonMint
import com.example.ui.theme.SlateMutedText
import com.example.ui.viewmodel.GameViewModel

@Composable
fun PauseSettingsDialog(
    userStats: UserStats?,
    difficultyMode: GameViewModel.Difficulty,
    onReset: () -> Unit,
    onChangeDifficulty: ((GameViewModel.Difficulty) -> Unit)?,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, ActionNeonCyan),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.92f).testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("SETTINGS & OPTIONS", color = ActionNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))

                Button(
                    onClick = { onReset(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_reset_level_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Level", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("RESET LEVEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("DIFFICULTY MODE", color = SlateMutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onChangeDifficulty?.invoke(GameViewModel.Difficulty.ZEN) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (difficultyMode == GameViewModel.Difficulty.ZEN) ActionNeonCyan else Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, if (difficultyMode == GameViewModel.Difficulty.ZEN) Color.Transparent else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("ZEN", color = if (difficultyMode == GameViewModel.Difficulty.ZEN) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }

                    Button(
                        onClick = { onChangeDifficulty?.invoke(GameViewModel.Difficulty.MASTER) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (difficultyMode == GameViewModel.Difficulty.MASTER) ActionNeonCoral else Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, if (difficultyMode == GameViewModel.Difficulty.MASTER) Color.Transparent else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("MASTER", color = if (difficultyMode == GameViewModel.Difficulty.MASTER) Color.White else Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (userStats?.soundEnabled != false) Icons.Default.Check else Icons.Default.Clear, contentDescription = "Sound Icon", tint = SlateMutedText, modifier = Modifier.size(18.dp))
                        Text("SOUND EFFECTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Switch(checked = userStats?.soundEnabled != false, onCheckedChange = { onToggleSound(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = ActionNeonCyan))
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (userStats?.hapticEnabled != false) Icons.Default.Check else Icons.Default.Clear, contentDescription = "Haptic Icon", tint = SlateMutedText, modifier = Modifier.size(18.dp))
                        Text("HAPTIC FEEDBACK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Switch(checked = userStats?.hapticEnabled != false, onCheckedChange = { onToggleHaptic(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = ActionNeonCyan))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("RESUME GAME", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}
