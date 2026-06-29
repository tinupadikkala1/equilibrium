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

enum class ParticleShape { RECTANGLE, CIRCLE, TRIANGLE }

data class ConfettiParticle(
    val x: Float, val y: Float, val color: Color,
    val speedX: Float, val speedY: Float,
    val rotation: Float, val rotationSpeed: Float,
    val width: Float, val height: Float,
    val shape: ParticleShape, val alpha: Float = 1f
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var size by remember { mutableStateOf<Size?>(null) }

    LaunchedEffect(size) {
        val currentSize = size
        if (currentSize != null && currentSize.width > 0f && particles.isEmpty()) {
            val list = mutableListOf<ConfettiParticle>()
            val random = java.util.Random()
            val colors = listOf(
                Color(0xFF00FFCC), Color(0xFF00FF66), Color(0xFFFF0D87),
                Color(0xFFFFCC00), Color(0xFFFF5E62), Color(0xFF2EA671),
                Color(0xFF4D38EC), Color(0xFFFF9966)
            )
            val width = currentSize.width
            val height = currentSize.height

            // Left Cannon
            for (i in 0 until 50) {
                val angleRad = -random.nextFloat() * (Math.PI / 3).toFloat() - (Math.PI / 12).toFloat()
                val speed = 12f + random.nextFloat() * 18f
                list.add(ConfettiParticle(0f, height, colors[random.nextInt(colors.size)], cos(angleRad) * speed, sin(angleRad) * speed, random.nextFloat() * 360f, -10f + random.nextFloat() * 20f, 8f + random.nextFloat() * 14f, 6f + random.nextFloat() * 10f, ParticleShape.values()[random.nextInt(ParticleShape.values().size)]))
            }
            // Right Cannon
            for (i in 0 until 50) {
                val angleRad = -random.nextFloat() * (Math.PI / 3).toFloat() - (Math.PI * 7 / 12).toFloat()
                val speed = 12f + random.nextFloat() * 18f
                list.add(ConfettiParticle(width, height, colors[random.nextInt(colors.size)], cos(angleRad) * speed, sin(angleRad) * speed, random.nextFloat() * 360f, -10f + random.nextFloat() * 20f, 8f + random.nextFloat() * 14f, 6f + random.nextFloat() * 10f, ParticleShape.values()[random.nextInt(ParticleShape.values().size)]))
            }
            // Central Burst
            for (i in 0 until 40) {
                val angleRad = random.nextFloat() * 2 * Math.PI.toFloat()
                val speed = 5f + random.nextFloat() * 12f
                list.add(ConfettiParticle(width / 2, height * 0.45f, colors[random.nextInt(colors.size)], cos(angleRad) * speed, sin(angleRad) * speed, random.nextFloat() * 360f, -10f + random.nextFloat() * 20f, 8f + random.nextFloat() * 12f, 6f + random.nextFloat() * 8f, ParticleShape.values()[random.nextInt(ParticleShape.values().size)]))
            }
            particles = list
        }
    }

    LaunchedEffect(particles) {
        if (particles.isNotEmpty()) {
            val gravity = 0.28f
            while (true) {
                withFrameNanos { _ ->
                    particles = particles.mapNotNull { p ->
                        val nextAlpha = p.alpha - 0.0075f
                        if (nextAlpha <= 0f) null
                        else p.copy(x = p.x + p.speedX, y = p.y + p.speedY, speedY = p.speedY + gravity, rotation = p.rotation + p.rotationSpeed, alpha = nextAlpha)
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize().testTag("confetti_canvas")) {
        if (size == null || size != this.size) { size = this.size }
        particles.forEach { p ->
            rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                val paintColor = p.color.copy(alpha = p.alpha)
                when (p.shape) {
                    ParticleShape.CIRCLE -> drawCircle(paintColor, p.width / 2, Offset(p.x, p.y))
                    ParticleShape.RECTANGLE -> drawRect(paintColor, Offset(p.x - p.width / 2, p.y - p.height / 2), Size(p.width, p.height))
                    ParticleShape.TRIANGLE -> {
                        val path = Path().apply { moveTo(p.x, p.y - p.height / 2); lineTo(p.x - p.width / 2, p.y + p.height / 2); lineTo(p.x + p.width / 2, p.y + p.height / 2); close() }
                        drawPath(path, paintColor)
                    }
                }
            }
        }
    }
}
