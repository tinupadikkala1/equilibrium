package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGamePalette

@Composable
fun AnimatedTile(
    value: Int, progress: Float, isHintHighlighted: Boolean, onTap: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, label: String = "Tile, value $value",
) {
    val p = LocalGamePalette.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val compression by animateFloatAsState(if (pressed) .94f else 1f, spring(stiffness = 800f), label = "tile press")
    val special = value % 2 == 0 && progress < 1f
    val base = when { isHintHighlighted -> p.gold; progress >= 1f -> p.accent; special -> p.violet; else -> p.panelRaised }
    val fill by animateColorAsState(base, label = "tile energy")
    val ink = when { isHintHighlighted -> p.onGold; progress >= 1f -> p.onAccent; special -> p.onViolet; else -> p.ink }
    val shape = RoundedCornerShape(if (p.dark) 17.dp else 15.dp)
    Box(
        modifier.aspectRatio(1f).padding(bottom = 4.dp)
            .graphicsLayer { scaleX = compression; scaleY = compression }
            .drawBehind { if (!p.dark) drawRoundRect(p.shadow, topLeft = Offset(0f, 4.dp.toPx()), cornerRadius = CornerRadius(15.dp.toPx())) }
            .shadow(if (p.dark) 7.dp else 0.dp, shape)
            .clip(shape).background(Brush.linearGradient(listOf(fill, lerp(fill, p.background, .12f))))
            .border(if (isHintHighlighted) 3.dp else if (p.dark) 1.dp else 2.dp, if (isHintHighlighted) p.ink else p.line, shape)
            .semantics { contentDescription = if (isHintHighlighted) "$label, suggested move" else label }
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interactions, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text("$value", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = if (value in -99..99) 23.sp else 17.sp)
    }
}

@Composable
fun PuzzleBoard(board: Array<IntArray>, hint: Pair<Int, Int>?, enabled: Boolean, onTap: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalGamePalette.current
    val size = board.size
    val progress = com.example.data.engine.GameRules.calculateProgress(board)
    Column(modifier.background(p.panel.copy(alpha = .7f), RoundedCornerShape(26.dp))
        .border(if (p.dark) 1.dp else 2.dp, p.line, RoundedCornerShape(26.dp)).padding(if (size >= 5) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (size >= 5) 5.dp else 8.dp)) {
        for (r in board.indices) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (size >= 5) 5.dp else 8.dp)) {
                for (c in board[r].indices) {
                    AnimatedTile(board[r][c], progress, hint == Pair(r, c), { onTap(r, c) },
                        Modifier.weight(1f).testTag("tile_${r}_$c"), enabled, "Row ${r + 1}, column ${c + 1}, value ${board[r][c]}")
                }
            }
        }
    }
}
