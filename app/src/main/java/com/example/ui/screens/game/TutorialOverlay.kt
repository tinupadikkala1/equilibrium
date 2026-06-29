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
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonMint

@Composable
fun TutorialOverlay(
    tutorialStep: Int,
    onNextStep: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, ActionNeonCyan),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f).testTag("tutorial_overlay")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("HOW TO PLAY", color = ActionNeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                when (tutorialStep) {
                    1 -> {
                        Text("Equilibrium is a grid balancing challenge.\n\nYour objective is to balance all values on the grid so that they reach mathematical par. Tap on high or low tiles to spread, absorb, and ripple values to neighboring tiles until complete.", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        Button(onClick = onNextStep, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonCyan), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(40.dp).testTag("tutorial_step_1_next")) {
                            Text("NEXT STEP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        Text("Look at the pulsating illuminated center cell [1,1] in the background.\n\nTap it now to see how the mathematical flow spreads values across row and column intersections to achieve equilibrium!", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        Text("Tap the glowing tile to continue...", color = ActionNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    }
                    3 -> {
                        Text("Fantastic job!\n\nNotice how the surrounding numbers transformed toward equilibrium balance. Keep balancing untransformed cells until all are balanced to solve the stage!", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = ActionNeonMint), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(40.dp).testTag("tutorial_step_3_finish")) {
                            Text("START BALANCING!", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
