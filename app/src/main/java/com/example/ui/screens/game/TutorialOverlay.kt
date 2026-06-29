package com.example.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun TutorialOverlay(tutorialStep: Int, onNextStep: () -> Unit, onFinish: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ActionNeonCyan.copy(0.4f)), colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart), modifier = Modifier.fillMaxWidth(0.95f).testTag("tutorial_overlay")) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("HOW TO PLAY", color = ActionNeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                when (tutorialStep) {
                    1 -> {
                        Text("Tap a tile: it loses 1, each neighbor gains 1.\n\nMake every tile show the same number to win!", color = Color.White.copy(0.85f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
                        Button(onClick = onNextStep, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(42.dp).testTag("tutorial_step_1_next")) { Text("NEXT", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                    2 -> {
                        Text("Tap the glowing center tile to see the ripple effect!", color = Color.White.copy(0.85f), fontSize = 13.sp, textAlign = TextAlign.Center)
                        Text("Tap the highlighted tile...", color = ActionNeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    3 -> {
                        Text("Great! Notice how neighbors changed.\n\nKeep balancing until all tiles are equal!", color = Color.White.copy(0.85f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
                        Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(42.dp).testTag("tutorial_step_3_finish")) { Text("START PLAYING!", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
