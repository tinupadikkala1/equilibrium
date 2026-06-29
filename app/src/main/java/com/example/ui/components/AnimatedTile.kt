package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedTile(
    value: Int,
    progress: Float,
    isHintHighlighted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var clickedScale by remember { mutableStateOf(1.0f) }
    val animatedScale by animateFloatAsState(
        targetValue = clickedScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "TileScale"
    )

    var pulseScale by remember { mutableStateOf(1.0f) }
    LaunchedEffect(isHintHighlighted) {
        if (isHintHighlighted) {
            while (true) { pulseScale = 1.06f; delay(450); pulseScale = 0.94f; delay(450) }
        } else { pulseScale = 1.0f }
    }

    val baseColor = lerp(
        MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
        progress
    )
    val gradientBrush = Brush.radialGradient(
        colors = listOf(baseColor.copy(alpha = 0.95f), baseColor.copy(alpha = 0.6f))
    )
    val borderColor = if (isHintHighlighted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
    val shape = RoundedCornerShape(14.dp)
    val finalScale = (if (isHintHighlighted) pulseScale else 1f) * animatedScale

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .shadow(
                elevation = if (isHintHighlighted) 14.dp else 3.dp,
                shape = shape,
                ambientColor = if (isHintHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
                spotColor = if (isHintHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .graphicsLayer { scaleX = finalScale; scaleY = finalScale }
            .clip(shape)
            .background(gradientBrush)
            .border(if (isHintHighlighted) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch { clickedScale = 0.82f; delay(80); clickedScale = 1.0f }
                onTap()
            }
            .testTag("game_tile_$value")
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (if (targetState > initialState)
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                else
                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                ).using(SizeTransform(clip = false))
            },
            label = "TileNum"
        ) { num ->
            Text(
                text = num.toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
