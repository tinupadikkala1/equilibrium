package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel

@Composable
fun DifficultySelectScreen(
    levelId: Int,
    isDailyChallenge: Boolean,
    onSelectDifficulty: (GameViewModel.Difficulty) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Top bar
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, Modifier.testTag("difficulty_back_button")) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                Text(if (isDailyChallenge) "Daily Challenge" else "Stage $levelId", color = SlateMutedText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.weight(0.25f))

            Text("Choose Mode", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("How do you want to play?", color = SlateMutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 36.dp))

            // ZEN
            ModeCard("🧘", "Zen", "No timer · Free hints · Relax", ActionNeonCyan, "difficulty_zen_card") { onSelectDifficulty(GameViewModel.Difficulty.ZEN) }
            Spacer(Modifier.height(16.dp))
            // MASTER
            ModeCard("⚡", "Master", "Countdown · Limited hints · Compete", ActionNeonCoral, "difficulty_master_card") { onSelectDifficulty(GameViewModel.Difficulty.MASTER) }

            Spacer(Modifier.weight(0.4f))
        }
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, desc: String, accent: Color, tag: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = SurfaceGradientStart),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = accent.copy(0.2f), spotColor = accent.copy(0.1f))
            .clickable(onClick = onClick).testTag(tag)
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 26.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text(desc, color = Color.White.copy(0.7f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
