package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import com.example.ui.theme.ActionNeonCoral
import com.example.ui.theme.ActionNeonCyan

@Composable
fun TimeUpDialog(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onWatchAdForTime: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, ActionNeonCoral),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.92f).testTag("times_up_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("TIME HAS EXPIRED!", color = ActionNeonCoral, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Text("Oops! You ran out of time while attempting to bring equilibrium to the grid on Master difficulty.", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                if (onWatchAdForTime != null) {
                    Button(
                        onClick = onWatchAdForTime,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("times_up_watch_ad_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Watch Ad", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Text("WATCH AD FOR +30s", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("times_up_retry_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("RETRY STAGE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text("BACK TO MENU", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
