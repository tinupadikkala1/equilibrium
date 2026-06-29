package com.example.ui.screens.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import kotlin.math.cos
import kotlin.math.sin

enum class ParticleShape { RECT, CIRCLE, TRI }
data class Particle(val x: Float, val y: Float, val color: Color, val sx: Float, val sy: Float, val rot: Float, val rs: Float, val w: Float, val h: Float, val shape: ParticleShape, val alpha: Float = 1f)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var canvasSize by remember { mutableStateOf<Size?>(null) }
    val colors = listOf(Color(0xFF00ECEC), Color(0xFF2ED573), Color(0xFFFF4757), Color(0xFFFFA502), Color(0xFFFF0D87), Color(0xFF4D38EC), Color(0xFFFF9966), Color(0xFF55EFC4))

    LaunchedEffect(canvasSize) {
        val s = canvasSize ?: return@LaunchedEffect
        if (s.width <= 0f || particles.isNotEmpty()) return@LaunchedEffect
        val r = java.util.Random(); val list = mutableListOf<Particle>()
        // Left cannon
        repeat(50) { val a = -r.nextFloat() * 1.05f - 0.26f; val sp = 12f + r.nextFloat() * 18f; list.add(Particle(0f, s.height, colors[r.nextInt(8)], cos(a) * sp, sin(a) * sp, r.nextFloat() * 360f, -10f + r.nextFloat() * 20f, 8f + r.nextFloat() * 14f, 6f + r.nextFloat() * 10f, ParticleShape.values()[r.nextInt(3)])) }
        // Right cannon
        repeat(50) { val a = -r.nextFloat() * 1.05f - 1.83f; val sp = 12f + r.nextFloat() * 18f; list.add(Particle(s.width, s.height, colors[r.nextInt(8)], cos(a) * sp, sin(a) * sp, r.nextFloat() * 360f, -10f + r.nextFloat() * 20f, 8f + r.nextFloat() * 14f, 6f + r.nextFloat() * 10f, ParticleShape.values()[r.nextInt(3)])) }
        // Center burst
        repeat(40) { val a = r.nextFloat() * 6.28f; val sp = 5f + r.nextFloat() * 12f; list.add(Particle(s.width / 2, s.height * 0.45f, colors[r.nextInt(8)], cos(a) * sp, sin(a) * sp, r.nextFloat() * 360f, -10f + r.nextFloat() * 20f, 8f + r.nextFloat() * 12f, 6f + r.nextFloat() * 8f, ParticleShape.values()[r.nextInt(3)])) }
        particles = list
    }

    LaunchedEffect(particles) {
        if (particles.isEmpty()) return@LaunchedEffect
        while (true) { withFrameNanos { _ -> particles = particles.mapNotNull { p -> val na = p.alpha - 0.007f; if (na <= 0f) null else p.copy(x = p.x + p.sx, y = p.y + p.sy, sy = p.sy + 0.28f, rot = p.rot + p.rs, alpha = na) } } }
    }

    Canvas(modifier.fillMaxSize().testTag("confetti_canvas")) {
        if (canvasSize == null || canvasSize != size) canvasSize = size
        particles.forEach { p ->
            rotate(p.rot, Offset(p.x, p.y)) {
                val c = p.color.copy(alpha = p.alpha)
                when (p.shape) {
                    ParticleShape.CIRCLE -> drawCircle(c, p.w / 2, Offset(p.x, p.y))
                    ParticleShape.RECT -> drawRect(c, Offset(p.x - p.w / 2, p.y - p.h / 2), Size(p.w, p.h))
                    ParticleShape.TRI -> drawPath(Path().apply { moveTo(p.x, p.y - p.h / 2); lineTo(p.x - p.w / 2, p.y + p.h / 2); lineTo(p.x + p.w / 2, p.y + p.h / 2); close() }, c)
                }
            }
        }
    }
}
