package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun TimeUpDialog(onRetry: () -> Unit, onBack: () -> Unit, onWatchAdForTime: (() -> Unit)? = null) {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.5.dp, ActionNeonCoral.copy(0.5f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth(0.92f).testTag("times_up_dialog")) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("TIME'S UP!", color = ActionNeonCoral, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text("You ran out of time on Master difficulty.", color = Color.White.copy(0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
                if (onWatchAdForTime != null) {
                    Button(onClick = onWatchAdForTime, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp).testTag("times_up_watch_ad_button")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(16.dp)); Text("WATCH AD FOR +30s", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    }
                }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp).testTag("times_up_retry_button")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp)); Text("RETRY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                }
                TextButton(onClick = onBack) { Text("BACK TO MENU", color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
