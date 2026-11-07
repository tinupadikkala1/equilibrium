package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalGamePalette

@Composable
fun GameBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = LocalGamePalette.current
    Box(modifier.background(p.background)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(p.violet.copy(alpha = if (p.dark) .20f else .12f), Color.Transparent), center = Offset(size.width * .85f, 0f), radius = size.width), size.width, Offset(size.width * .85f, 0f))
            if (p.dark) {
                repeat(3) { i -> drawCircle(p.violet.copy(alpha = .09f), size.width * (.46f + i * .20f), Offset(size.width * 1.04f, size.height * .25f), style = Stroke(1.dp.toPx())) }
            } else {
                drawCircle(p.gold.copy(alpha = .19f), size.width * .35f, Offset(0f, size.height * .8f))
                drawCircle(p.accent.copy(alpha = .10f), size.width * .38f, Offset(size.width, size.height * .35f))
            }
        }
        content()
    }
}

@Composable
fun GamePanel(modifier: Modifier = Modifier, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalGamePalette.current
    val shape = RoundedCornerShape(24.dp)
    val depth = if (p.dark) 0.dp else 4.dp
    Column(
        modifier.padding(bottom = depth).drawBehind {
            if (!p.dark) drawRoundRect(p.shadow, topLeft = Offset(2.dp.toPx(), depth.toPx()), cornerRadius = CornerRadius(24.dp.toPx()))
        }.shadow(if (p.dark) 10.dp else 0.dp, shape).clip(shape)
            .background(Brush.linearGradient(listOf(if (accent) p.panelRaised else p.panel, p.panel)))
            .border(if (p.dark) 1.dp else 2.dp, p.line, shape).padding(18.dp),
        content = content,
    )
}

enum class ActionStyle { PRIMARY, SECONDARY, GOLD, DANGER }

@Composable
fun GameButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: ImageVector? = null, style: ActionStyle = ActionStyle.PRIMARY,
    enabled: Boolean = true, subtitle: String? = null, stacked: Boolean = false,
) {
    val p = LocalGamePalette.current
    val background = when (style) { ActionStyle.PRIMARY -> p.accent; ActionStyle.GOLD -> p.gold; else -> p.panelRaised }
    val foreground = when (style) { ActionStyle.PRIMARY -> p.onAccent; ActionStyle.GOLD -> p.onGold; ActionStyle.DANGER -> p.danger; else -> p.ink }
    val radius = if (stacked) 20.dp else 18.dp
    val shape = RoundedCornerShape(radius)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val displacement by animateFloatAsState(if (pressed) 3f else 0f, spring(stiffness = 900f), label = "button depth")
    Box(modifier.padding(bottom = 5.dp).drawBehind {
        if (!p.dark) drawRoundRect(p.shadow, topLeft = Offset(1.dp.toPx(), 4.dp.toPx()), cornerRadius = CornerRadius(radius.toPx()))
    }) {
        val face = Modifier.fillMaxWidth().graphicsLayer { translationY = displacement.dp.toPx(); alpha = if (enabled) 1f else .46f }
            .clip(shape).background(Brush.verticalGradient(listOf(background, background.copy(alpha = .94f))))
            .border(if (p.dark) 1.dp else 2.dp, if (p.dark && style == ActionStyle.PRIMARY) p.accent else p.line, shape)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interactions, indication = null, onClick = onClick)
        if (stacked) {
            Column(face.heightIn(min = 92.dp).padding(horizontal = 5.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (icon != null) Icon(icon, null, Modifier.size(23.dp), tint = foreground)
                Text(text, style = MaterialTheme.typography.labelMedium, color = foreground)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = foreground)
            }
        } else {
            Row(face.heightIn(min = 56.dp).padding(horizontal = 17.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, Modifier.size(21.dp), tint = foreground); Spacer(Modifier.width(10.dp)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text, style = MaterialTheme.typography.labelLarge, color = foreground)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = foreground)
                }
            }
        }
    }
}

@Composable
fun GameIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalGamePalette.current
    Surface(onClick = onClick, modifier = modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = p.panel,
        border = BorderStroke(if (p.dark) 1.dp else 2.dp, p.line)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(22.dp), tint = p.ink) }
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(java.util.Locale.ROOT), modifier, color = LocalGamePalette.current.muted, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun GameDialog(tag: String, onDismiss: () -> Unit, dismissible: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            GamePanel(Modifier.widthIn(max = 440.dp).fillMaxWidth().testTag(tag)) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
    }
}

@Composable
fun BalanceEmblem(modifier: Modifier = Modifier) {
    val p = LocalGamePalette.current
    Box(modifier.size(72.dp).clip(if (p.dark) CircleShape else RoundedCornerShape(23.dp))
        .background(Brush.linearGradient(listOf(p.violet, if (p.dark) p.panelRaised else p.violet))), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(if (p.dark) p.accent else p.gold)) }
            } }
        }
    }
}

fun formatTime(seconds: Int): String = "%02d:%02d".format(java.util.Locale.ROOT, seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)
