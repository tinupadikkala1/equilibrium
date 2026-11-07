package com.example.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.ui.theme.LocalGamePalette
import kotlin.math.sin

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val p = LocalGamePalette.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1300)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        repeat(28) { i ->
            val x = size.width * (i / 28f)
            val y = size.height * (.1f + t * .85f) + sin(i.toFloat()) * 30f
            val color = listOf(p.accent, p.violet, p.gold)[i % 3].copy(alpha = 1f - t)
            drawRect(color, Offset(x, y), Size(5f + i % 4, 8f))
        }
    }
}
