package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActionNeonCyan
import com.example.ui.theme.ActionNeonCoral
import com.example.ui.theme.ActionNeonMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedTile(
    value: Int,
    progress: Float, // 0f(chaos red) to 1f(equilibrium mint)
    isHintHighlighted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    
    // Smoothly animated spring scale for tactile finger-tap physical response
    var clickedScale by remember { mutableStateOf(1.0f) }
    val animatedScale by animateFloatAsState(
        targetValue = clickedScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "TileTactileTouchBounce"
    )
    
    // Lerp background color based on balance progress
    val activeBgColor = lerp(
        start = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
        stop = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f),
        fraction = progress
    )

    // Pulsing animations for active hints
    var pulseScale by remember { mutableStateOf(1.0f) }
    LaunchedEffect(isHintHighlighted) {
        if (isHintHighlighted) {
            while (true) {
                pulseScale = 1.05f
                delay(400)
                pulseScale = 0.95f
                delay(400)
            }
        } else {
            pulseScale = 1.0f
        }
    }

    // Dynamic tile styling
    val borderStroke = if (isHintHighlighted) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
    }

    val finalScale = if (isHintHighlighted) pulseScale * animatedScale else animatedScale

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = activeBgColor
        ),
        border = borderStroke,
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .shadow(
                elevation = if (isHintHighlighted) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = if (isHintHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
                spotColor = if (isHintHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                scope.launch {
                    clickedScale = 0.82f
                    delay(80)
                    clickedScale = 1.0f
                }
                onTap()
            }
            .testTag("game_tile_${value}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Rolling numbers animation effect (slot machine mechanism)
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "TileNumberScroll"
            ) { targetValue ->
                Text(
                    text = targetValue.toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("tile_text")
                )
            }
        }
    }
}
