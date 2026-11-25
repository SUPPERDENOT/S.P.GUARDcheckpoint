package com.superdenot.powerguard.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val POWER_BUTTON_PRESSED_SCALE = 0.95f
internal const val POWER_BUTTON_GLOW_PEAK_ALPHA = 0.20f
internal const val POWER_BUTTON_GLOW_PRESS_DURATION_MS = 150
internal const val POWER_BUTTON_GLOW_RELEASE_DURATION_MS = 180
internal const val POWER_BUTTON_CLOSE_DELAY_MS = 90L

@Composable
fun Modifier.powerButtonClickEffect(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = POWER_BUTTON_PRESSED_SCALE
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "power-button-bounce"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) POWER_BUTTON_GLOW_PEAK_ALPHA else 0f,
        animationSpec = tween(
            durationMillis = if (isPressed) POWER_BUTTON_GLOW_PRESS_DURATION_MS
            else POWER_BUTTON_GLOW_RELEASE_DURATION_MS
        ),
        label = "power-button-glow"
    )
    val accent = MaterialTheme.colorScheme.primary
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .drawWithContent {
            drawContent()
            if (glowAlpha > 0f) {
                val inset = 1.5.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = glowAlpha),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
}

@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = POWER_BUTTON_PRESSED_SCALE
): Modifier = powerButtonClickEffect(interactionSource, pressedScale)

@Composable
fun Modifier.pressScaleClickable(
    pressedScale: Float = POWER_BUTTON_PRESSED_SCALE,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return powerButtonClickEffect(interactionSource, pressedScale)
        .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
}

@Composable
fun PowerActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    clickDelayMillis: Long = 0L,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            if (clickDelayMillis > 0L) {
                coroutineScope.launch {
                    delay(clickDelayMillis)
                    onClick()
                }
            } else onClick()
        },
        modifier = modifier.height(36.dp).powerButtonClickEffect(interactionSource),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun PowerSaveButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    clickDelayMillis: Long = 0L,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            if (clickDelayMillis > 0L) {
                coroutineScope.launch {
                    delay(clickDelayMillis)
                    onClick()
                }
            } else onClick()
        },
        modifier = modifier.height(36.dp).powerButtonClickEffect(interactionSource),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}
