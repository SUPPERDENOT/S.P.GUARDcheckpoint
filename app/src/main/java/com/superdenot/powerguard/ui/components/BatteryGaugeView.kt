package com.superdenot.powerguard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdenot.powerguard.ui.theme.InstrumentGraphite
import com.superdenot.powerguard.ui.theme.LocalBatterySemanticColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryGaugeView(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    isCharging: Boolean,
    isCriticalThermal: Boolean,
    isWarm: Boolean,
    isVisibleOnScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalBatterySemanticColors.current
    val gaugeColor = resolveBatteryGaugeColor(
        semanticColors = semanticColors,
        isCriticalThermal = isCriticalThermal,
        isWarm = isWarm,
        isCharging = isCharging
    )
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.semantics {
            contentDescription = "Level baterai $percentageLabel"
            progressBarRangeInfo = ProgressBarRangeInfo(safeProgress, 0f..1f)
        },
        contentAlignment = Alignment.Center
    ) {
        if (shouldAnimateBatteryGauge(isCharging, isVisibleOnScreen, isCriticalThermal, isWarm, animation)) {
            AnimatedBatteryGauge(
                progress = safeProgress,
                percentageLabel = percentageLabel,
                model = model,
                animation = animation,
                gaugeColor = gaugeColor
            )
        } else {
            StaticBatteryGauge(
                progress = safeProgress,
                percentageLabel = percentageLabel,
                model = model,
                animation = animation,
                gaugeColor = gaugeColor
            )
        }
    }
}

@Composable
private fun AnimatedBatteryGauge(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color
) {
    val transition = rememberInfiniteTransition(label = "battery-gauge-charging")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battery-gauge-phase"
    )
    BatteryGaugeRenderer(progress, percentageLabel, model, animation, gaugeColor, phase)
}

@Composable
private fun StaticBatteryGauge(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color
) {
    BatteryGaugeRenderer(progress, percentageLabel, model, animation, gaugeColor, null)
}

@Composable
private fun BatteryGaugeRenderer(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color,
    phase: State<Float>?
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    when (model) {
        BatteryGaugeModel.ARC -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArcGauge(progress, gaugeColor, trackColor, phase?.value ?: 0f, animation)
                }
                GaugeCenterLabel(percentageLabel)
            }
        }
        BatteryGaugeModel.CAPSULE -> {
            val textColor = if (progress >= 0.56f) {
                if (gaugeColor.luminance() < 0.18f) Color.White else InstrumentGraphite
            } else onSurfaceColor
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCapsuleGauge(progress, gaugeColor, trackColor, phase?.value ?: 0f, animation)
                }
                Text(
                    percentageLabel,
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        BatteryGaugeModel.SEGMENTED -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawSegmentedGauge(progress, gaugeColor, trackColor, phase?.value ?: 0f, animation)
                }
                GaugeCenterLabel(percentageLabel)
            }
        }
        BatteryGaugeModel.WAVE -> {
            val upperTextColor = if (isDark) onSurfaceColor else InstrumentGraphite
            WaveGauge(
                progress = progress,
                percentageLabel = percentageLabel,
                gaugeColor = gaugeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = trackColor,
                phase = phase,
                animation = animation,
                upperTextColor = upperTextColor
            )
        }
    }
}

@Composable
private fun GaugeCenterLabel(label: String) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcGauge(
    progress: Float,
    color: Color,
    trackColor: Color,
    phase: Float,
    animation: ChargingArcAnimation
) {
    val stroke = 7.dp.toPx()
    val diameter = (size.minDimension - stroke).coerceAtLeast(1f)
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    drawArc(trackColor, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    if (progress <= 0f) return
    val sweep = 270f * progress
    val brush = if (animation == ChargingArcAnimation.ENERGY_SWEEP) {
        Brush.sweepGradient(listOf(color.copy(alpha = 0.35f), color, color.copy(alpha = 0.7f)))
    } else Brush.linearGradient(listOf(color, color))
    if (animation == ChargingArcAnimation.ENERGY_SWEEP) {
        rotate(phase * 57.29578f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawArc(brush, 135f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    } else {
        drawArc(brush, 135f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    when (animation) {
        ChargingArcAnimation.SEGMENTED_FLOW -> {
            val movingSweep = 18f.coerceAtMost(sweep)
            val phaseFraction = phase / (2f * PI).toFloat()
            val movingStart = 135f + phaseFraction * (sweep - movingSweep).coerceAtLeast(0f)
            drawArc(Color.White.copy(alpha = 0.75f), movingStart, movingSweep, false,
                topLeft, arcSize, style = Stroke(stroke * 0.72f, cap = StrokeCap.Round))
        }
        ChargingArcAnimation.COMET_HEAD -> {
            val phaseFraction = phase / (2f * PI).toFloat()
            val angle = Math.toRadians((135f + sweep * phaseFraction).toDouble())
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                Color.White,
                radius = stroke * 0.68f,
                center = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            )
        }
        ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.STATIC -> Unit
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCapsuleGauge(
    progress: Float,
    color: Color,
    trackColor: Color,
    phase: Float,
    animation: ChargingArcAnimation
) {
    val bodyWidth = size.width * 0.48f
    val bodyHeight = size.height * 0.78f
    val left = (size.width - bodyWidth) / 2f
    val top = size.height * 0.15f
    val body = Path().apply {
        addRoundRect(RoundRect(left, top, left + bodyWidth, top + bodyHeight, CornerRadius(bodyWidth * 0.22f)))
    }
    val terminalWidth = bodyWidth * 0.36f
    val terminalHeight = 6.dp.toPx()
    drawRoundRect(
        trackColor,
        Offset((size.width - terminalWidth) / 2f, top - terminalHeight),
        Size(terminalWidth, terminalHeight),
        CornerRadius(terminalHeight / 2f)
    )
    val terminalCenter = Offset(size.width / 2f, top - terminalHeight / 2f)
    val plusHalfWidth = 2.dp.toPx()
    val plusHalfHeight = 1.5.dp.toPx()
    drawLine(
        color,
        Offset(terminalCenter.x - plusHalfWidth, terminalCenter.y),
        Offset(terminalCenter.x + plusHalfWidth, terminalCenter.y),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color,
        Offset(terminalCenter.x, terminalCenter.y - plusHalfHeight),
        Offset(terminalCenter.x, terminalCenter.y + plusHalfHeight),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawPath(body, trackColor, style = Stroke(2.dp.toPx()))
    val innerInset = 2.dp.toPx()
    val fillHeight = (bodyHeight - innerInset * 2f) * progress
    val fillTop = top + bodyHeight - innerInset - fillHeight
    clipPath(body) {
        drawRect(
            color,
            Offset(left + innerInset, fillTop),
            Size(bodyWidth - innerInset * 2f, fillHeight)
        )
        when (animation) {
            ChargingArcAnimation.ENERGY_SWEEP -> {
                val stripeY = top + innerInset + ((phase / (2f * PI).toFloat()) * bodyHeight)
                if (stripeY in fillTop..(top + bodyHeight)) {
                    drawRect(Color.White.copy(alpha = 0.22f), Offset(left, stripeY), Size(bodyWidth, 5.dp.toPx()))
                }
            }
            ChargingArcAnimation.SEGMENTED_FLOW -> {
                var y = top + bodyHeight - 8.dp.toPx()
                while (y > fillTop) {
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(left + 3.dp.toPx(), y),
                        Offset(left + bodyWidth - 3.dp.toPx(), y), 1.dp.toPx())
                    y -= 9.dp.toPx()
                }
                val movingY = top + bodyHeight - (phase / (2f * PI).toFloat()) * fillHeight
                if (movingY in fillTop..(top + bodyHeight)) {
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(left + 3.dp.toPx(), movingY),
                        Offset(left + bodyWidth - 3.dp.toPx(), movingY), 2.dp.toPx())
                }
            }
            ChargingArcAnimation.COMET_HEAD -> {
                val cometX = size.width / 2f + sin(phase) * bodyWidth * 0.18f
                drawCircle(Color.White.copy(alpha = 0.9f), 3.dp.toPx(), Offset(cometX, fillTop))
            }
            ChargingArcAnimation.STATIC -> Unit
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegmentedGauge(
    progress: Float,
    color: Color,
    trackColor: Color,
    phase: Float,
    animation: ChargingArcAnimation
) {
    val stroke = 7.dp.toPx()
    val diameter = (size.minDimension - stroke).coerceAtLeast(1f)
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val gap = 3.5f
    val segmentSweep = (270f - 19f * gap) / 20f
    val litCount = segmentedGaugeLitCount((progress * 100f).toInt())
    repeat(20) { index ->
        val start = 135f + index * (segmentSweep + gap)
        drawArc(trackColor, start, segmentSweep, false, topLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Butt))
        if (index < litCount) {
            val movingIndex = if (litCount == 0) -1 else
                ((phase / (2f * PI).toFloat()) * litCount).toInt() % litCount
            val sweepPosition = (index / 20f + phase / (2f * PI).toFloat()) % 1f
            val segmentColor = when (animation) {
                ChargingArcAnimation.ENERGY_SWEEP -> color.copy(alpha = 0.55f + sweepPosition * 0.45f)
                ChargingArcAnimation.SEGMENTED_FLOW -> if (index == movingIndex) Color.White else color
                ChargingArcAnimation.COMET_HEAD -> color
                ChargingArcAnimation.STATIC -> color
            }
            drawArc(segmentColor, start, segmentSweep, false, topLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt))
        }
    }
    if (animation == ChargingArcAnimation.COMET_HEAD && litCount > 0) {
        val phaseFraction = phase / (2f * PI).toFloat()
        val angle = Math.toRadians((135f + 270f * progress * phaseFraction).toDouble())
        val radius = diameter / 2f
        drawCircle(Color.White, stroke * 0.7f,
            Offset(size.width / 2f + cos(angle).toFloat() * radius, size.height / 2f + sin(angle).toFloat() * radius))
    }
}

@Composable
private fun WaveGauge(
    progress: Float,
    percentageLabel: String,
    gaugeColor: Color,
    trackColor: Color,
    borderColor: Color,
    phase: State<Float>?,
    animation: ChargingArcAnimation,
    upperTextColor: Color
) {
    Box(Modifier.fillMaxSize().clip(CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f
            val circle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(left, top, left + diameter, top + diameter))
            }
            clipPath(circle) {
                drawRect(trackColor)
                val wavePhase = if (animation == ChargingArcAnimation.STATIC) 0f else phase?.value ?: 0f?.value ?: 0f
                val liquid = waveFillPath(size.width, size.height, progress, wavePhase)
                drawPath(liquid, gaugeColor)
                if (animation == ChargingArcAnimation.SEGMENTED_FLOW) {
                    val levelY = size.height * (1f - progress)
                    drawLine(Color.White.copy(alpha = 0.24f), Offset(0f, levelY + 7.dp.toPx()),
                        Offset(size.width, levelY + 7.dp.toPx()), 1.dp.toPx())
                }
                if (animation == ChargingArcAnimation.COMET_HEAD) {
                    val centerY = waveY(size.width / 2f, size.width, size.height, progress, wavePhase)
                    drawCircle(Color.White, 3.dp.toPx(), Offset(size.width / 2f, centerY))
                }
            }
            drawCircle(borderColor, diameter / 2f, Offset(size.width / 2f, size.height / 2f),
                style = Stroke(1.5.dp.toPx()))
        }
        val wavePhase = if (animation == ChargingArcAnimation.STATIC) 0f else phase
        Box(
            Modifier.fillMaxSize().drawWithContent {
                val path = waveTextClipPath(size.width, size.height, progress, wavePhase, above = true)
                clipPath(path) { this@drawWithContent.drawContent() }
            },
            contentAlignment = Alignment.Center
        ) {
            Text(percentageLabel, color = upperTextColor, fontSize = 23.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Box(
            Modifier.fillMaxSize().drawWithContent {
                val path = waveTextClipPath(size.width, size.height, progress, wavePhase, above = false)
                clipPath(path) { this@drawWithContent.drawContent() }
            },
            contentAlignment = Alignment.Center
        ) {
            Text(percentageLabel, color = Color.White, fontSize = 23.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private fun waveY(x: Float, width: Float, height: Float, progress: Float, phase: Float): Float {
    val normalized = if (width <= 0f) 0f else x / width
    val amplitude = height * 0.055f * sin(progress * PI).toFloat()
    val baseline = height * (1f - progress)
    return baseline + amplitude * sin(normalized * (2f * PI).toFloat() * 1.25f + phase)
}

private fun waveFillPath(width: Float, height: Float, progress: Float, phase: Float): Path =
    Path().apply {
        moveTo(0f, waveY(0f, width, height, progress, phase))
        for (step in 1..48) {
            val x = width * step / 48f
            lineTo(x, waveY(x, width, height, progress, phase))
        }
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }

private fun waveTextClipPath(
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    above: Boolean
): Path = Path().apply {
    if (above) {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        lineTo(width, waveY(width, width, height, progress, phase))
        for (step in 48 downTo 0) {
            val x = width * step / 48f
            lineTo(x, waveY(x, width, height, progress, phase))
        }
    } else {
        moveTo(0f, waveY(0f, width, height, progress, phase))
        for (step in 1..48) {
            val x = width * step / 48f
            lineTo(x, waveY(x, width, height, progress, phase))
        }
        lineTo(width, height)
        lineTo(0f, height)
    }
    close()
}
