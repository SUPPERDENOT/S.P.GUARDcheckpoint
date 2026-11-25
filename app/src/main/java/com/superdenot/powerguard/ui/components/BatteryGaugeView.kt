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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.superdenot.powerguard.ui.theme.LocalBatterySemanticColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal const val GAUGE_PERCENTAGE_OUTLINE_WIDTH_DP = 1.2f

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
    modifier: Modifier = Modifier,
    forceAnimate: Boolean = false
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
        val animateEffects = shouldAnimateBatteryGauge(
            isCharging = isCharging,
            isVisibleOnScreen = isVisibleOnScreen,
            isCriticalThermal = isCriticalThermal,
            isWarm = isWarm,
            animation = animation,
            forceAnimate = forceAnimate
        )
        if (animateEffects) {
            AnimatedBatteryGauge(
                progress = safeProgress,
                percentageLabel = percentageLabel,
                model = model,
                animation = animation,
                gaugeColor = gaugeColor,
                isCharging = isCharging
            )
        } else {
            StaticBatteryGauge(
                progress = safeProgress,
                percentageLabel = percentageLabel,
                model = model,
                animation = animation,
                gaugeColor = gaugeColor,
                isCharging = isCharging
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
    gaugeColor: Color,
    isCharging: Boolean
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
    BatteryGaugeRenderer(progress, percentageLabel, model, animation, gaugeColor, phase, animateEffects = true, isCharging = isCharging)
}

@Composable
private fun StaticBatteryGauge(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color,
    isCharging: Boolean
) {
    BatteryGaugeRenderer(progress, percentageLabel, model, animation, gaugeColor, null, animateEffects = false, isCharging = isCharging)
}

@Composable
private fun BatteryGaugeRenderer(
    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color,
    phase: State<Float>?,
    animateEffects: Boolean,
    isCharging: Boolean
) {
    val renderAnimation = if (animateEffects) animation else ChargingArcAnimation.STATIC
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    when (model) {
        BatteryGaugeModel.ARC -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArcGauge(progress, gaugeColor, trackColor, phase?.value ?: 0f, renderAnimation)
                }
                GaugeCenterLabel(percentageLabel)
            }
        }
        BatteryGaugeModel.CAPSULE -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCapsuleGauge(progress, gaugeColor, trackColor, outlineColor, phase?.value ?: 0f, renderAnimation)
                }
                GaugePercentageLabel(percentageLabel, 15.sp)
            }
        }
        BatteryGaugeModel.SEGMENTED -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawSegmentedGauge(progress, gaugeColor, trackColor, phase?.value ?: 0f, renderAnimation)
                }
                GaugeCenterLabel(percentageLabel)
            }
        }
        BatteryGaugeModel.WAVE -> {
            WaveGauge(
                progress = progress,
                percentageLabel = percentageLabel,
                gaugeColor = gaugeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = outlineColor,
                phase = phase,
                animation = animation,
                animateEffects = animateEffects,
                isCharging = isCharging
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
    val stroke = 8.dp.toPx()
    val diameter = (size.minDimension - stroke * 1.7f).coerceAtLeast(1f)
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val startAngle = -225f
    val fullSweep = 270f
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = diameter / 2f
    drawArc(trackColor.copy(alpha = 0.58f), startAngle, fullSweep, false, topLeft, arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round))
    if (progress <= 0f) return

    val activeSweep = fullSweep * progress.coerceIn(0f, 1f)
    val activeBrush = Brush.linearGradient(
        colors = listOf(color.copy(alpha = 0.66f), color, Color.White.copy(alpha = 0.94f), color),
        start = Offset(topLeft.x, topLeft.y + diameter),
        end = Offset(topLeft.x + diameter, topLeft.y)
    )
    drawArc(activeBrush, startAngle, activeSweep, false, topLeft, arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round))

    val phaseFraction = gaugeAnimationPhaseFraction(phase)
    when (animation) {
        ChargingArcAnimation.ENERGY_SWEEP -> {
            val sweepStart = startAngle + (phaseFraction * activeSweep)
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.72f), Color.Transparent)),
                sweepStart - 12f, 24f, false, topLeft, arcSize,
                style = Stroke(stroke * 0.48f, cap = StrokeCap.Round)
            )
        }
        ChargingArcAnimation.SEGMENTED_FLOW -> {
            val movingSweep = 22f.coerceAtMost(activeSweep)
            val movingStart = startAngle + (activeSweep - movingSweep) * phaseFraction
            drawArc(Color.White.copy(alpha = 0.78f), movingStart, movingSweep, false,
                topLeft, arcSize, style = Stroke(stroke * 0.58f, cap = StrokeCap.Round))
        }
        ChargingArcAnimation.COMET_HEAD -> Unit
        ChargingArcAnimation.STATIC -> Unit
    }

    if (animation == ChargingArcAnimation.COMET_HEAD || animation == ChargingArcAnimation.ENERGY_SWEEP) {
        val headAngle = Math.toRadians((startAngle + activeSweep * phaseFraction).toDouble())
        val head = Offset(
            center.x + cos(headAngle).toFloat() * radius,
            center.y + sin(headAngle).toFloat() * radius
        )
        drawCircle(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.56f), color.copy(alpha = 0.24f), Color.Transparent),
                center = head,
                radius = stroke * 2.8f
            ),
            radius = stroke * 2.8f,
            center = head
        )
        drawCircle(Color.White.copy(alpha = 0.96f), radius = stroke * 0.48f, center = head)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCapsuleGauge(
    progress: Float,
    color: Color,
    trackColor: Color,
    outlineColor: Color,
    phase: Float,
    animation: ChargingArcAnimation
) {
    val bodyWidth = size.width * 0.48f
    val bodyHeight = size.height * 0.78f
    val left = (size.width - bodyWidth) / 2f
    val top = size.height * 0.15f
    val bottom = top + bodyHeight
    val body = Path().apply {
        addRoundRect(RoundRect(left, top, left + bodyWidth, bottom, CornerRadius(bodyWidth * 0.22f)))
    }
    val terminalWidth = bodyWidth * 0.36f
    val terminalHeight = 6.dp.toPx()
    val terminalGap = 2.dp.toPx()
    val terminalTop = top - terminalHeight - terminalGap
    val terminalLeft = (size.width - terminalWidth) / 2f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.72f), trackColor, outlineColor.copy(alpha = 0.65f))),
        Offset(terminalLeft, terminalTop),
        Size(terminalWidth, terminalHeight),
        CornerRadius(terminalHeight / 2f)
    )
    drawRoundRect(
        outlineColor.copy(alpha = 0.8f),
        Offset(terminalLeft, terminalTop),
        Size(terminalWidth, terminalHeight),
        CornerRadius(terminalHeight / 2f),
        style = Stroke(1.2.dp.toPx())
    )
    drawPath(body, trackColor.copy(alpha = 0.78f))
    val innerInset = 3.dp.toPx()
    val innerLeft = left + innerInset
    val innerWidth = bodyWidth - innerInset * 2f
    val innerTop = top + innerInset
    val innerBottom = bottom - innerInset
    val fillHeight = (innerBottom - innerTop) * progress.coerceIn(0f, 1f)
    val fillTop = innerBottom - fillHeight
    val phaseFraction = gaugeAnimationPhaseFraction(phase)

    clipPath(body) {
        if (fillHeight > 0f) {
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.72f), color, color.copy(alpha = 0.9f)),
                    startY = fillTop,
                    endY = innerBottom
                ),
                Offset(innerLeft, fillTop),
                Size(innerWidth, fillHeight)
            )
            drawRect(
                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.White.copy(alpha = 0.16f))),
                Offset(innerLeft, fillTop),
                Size(innerWidth, fillHeight)
            )
        }
        when (animation) {
            ChargingArcAnimation.ENERGY_SWEEP -> {
                val stripeY = capsuleCometY(fillTop, innerBottom, phaseFraction)
                val stripeHeight = 7.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.72f), Color.Transparent),
                        startY = stripeY - stripeHeight,
                        endY = stripeY + stripeHeight
                    ),
                    Offset(innerLeft, stripeY - stripeHeight),
                    Size(innerWidth, stripeHeight * 2f)
                )
            }
            ChargingArcAnimation.SEGMENTED_FLOW -> {
                var y = innerBottom - 8.dp.toPx()
                while (y > fillTop) {
                    drawLine(Color.White.copy(alpha = 0.32f), Offset(innerLeft + 2.dp.toPx(), y),
                        Offset(innerLeft + innerWidth - 2.dp.toPx(), y), 1.dp.toPx())
                    y -= 9.dp.toPx()
                }
                val movingY = capsuleCometY(fillTop, innerBottom, phaseFraction)
                drawLine(Color.White.copy(alpha = 0.88f), Offset(innerLeft + 2.dp.toPx(), movingY),
                    Offset(innerLeft + innerWidth - 2.dp.toPx(), movingY), 1.8.dp.toPx())
            }
            ChargingArcAnimation.COMET_HEAD -> {
                val cometY = capsuleCometY(fillTop, innerBottom, phaseFraction)
                val tailLength = (innerBottom - fillTop).coerceAtMost(22.dp.toPx())
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.36f), Color.Transparent),
                        startY = cometY,
                        endY = (cometY + tailLength).coerceAtLeast(cometY + 1f)
                    ),
                    Offset(innerLeft + innerWidth * 0.22f, cometY),
                    Size(innerWidth * 0.56f, tailLength)
                )
                drawCircle(Color.White, 2.8.dp.toPx(), Offset(size.width / 2f, cometY))
            }
            ChargingArcAnimation.STATIC -> Unit
        }
    }
    drawPath(
        body,
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.78f), outlineColor, Color.White.copy(alpha = 0.28f), outlineColor),
            start = Offset(left, top),
            end = Offset(left + bodyWidth, bottom)
        ),
        style = Stroke(2.dp.toPx())
    )
    drawArc(
        Color.White.copy(alpha = 0.22f),
        -125f, 70f, false,
        Offset(left, top), Size(bodyWidth, bodyHeight),
        style = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegmentedGauge(
    progress: Float,
    color: Color,
    trackColor: Color,
    phase: Float,
    animation: ChargingArcAnimation
) {
    val stroke = 7.5.dp.toPx()
    val diameter = (size.minDimension - stroke * 1.7f).coerceAtLeast(1f)
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val gapDegrees = 4.5f
    val segmentSweep = SEGMENTED_GAUGE_STEP_DEGREES - gapDegrees
    val litCount = segmentedGaugeLitCount((progress.coerceIn(0f, 1f) * 100f).roundToInt())
    val phaseFraction = gaugeAnimationPhaseFraction(phase)
    val headIndex = if (litCount == 0) -1 else
        (phaseFraction * litCount).toInt().coerceIn(0, litCount - 1)

    repeat(SEGMENTED_GAUGE_BLOCK_COUNT) { index ->
        val start = segmentedGaugeStartAngle(index)
        drawArc(trackColor.copy(alpha = 0.58f), start, segmentSweep, false, topLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Butt))
        if (index < litCount) {
            val activeColor = when (animation) {
                ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.COMET_HEAD ->
                    color.copy(alpha = segmentedGaugeTrailAlpha(index, headIndex, litCount))
                ChargingArcAnimation.SEGMENTED_FLOW ->
                    if (index == headIndex) Color.White else color
                ChargingArcAnimation.STATIC -> color
            }
            drawArc(activeColor, start, segmentSweep, false, topLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt))
        }
    }

    if (litCount > 0 && (animation == ChargingArcAnimation.COMET_HEAD || animation == ChargingArcAnimation.ENERGY_SWEEP)) {
        val headAngle = Math.toRadians((-90f + (headIndex + 0.5f) * SEGMENTED_GAUGE_STEP_DEGREES).toDouble())
        val radius = diameter / 2f
        val head = Offset(
            size.width / 2f + cos(headAngle).toFloat() * radius,
            size.height / 2f + sin(headAngle).toFloat() * radius
        )
        if (animation == ChargingArcAnimation.COMET_HEAD) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.58f), color.copy(alpha = 0.28f), Color.Transparent),
                    center = head,
                    radius = stroke * 2.6f
                ),
                radius = stroke * 2.6f,
                center = head
            )
        }
        drawCircle(Color.White.copy(alpha = 0.98f), stroke * 0.48f, head)
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
    animateEffects: Boolean,
    isCharging: Boolean
) {
    Box(Modifier.fillMaxSize().clip(CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f
            val circleTopLeft = Offset(left, top)
            val circleSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val circle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(left, top, left + diameter, top + diameter))
            }
            val wavePhase = if (animateEffects) phase?.value ?: 0f else 0f
            val liquidProgress = waveLiquidFillLevel(progress, isCharging, animation)

            drawCircle(trackColor, diameter / 2f, center)
            clipPath(circle) {
                val layers = waveGaugeLayerSpecs(wavePhase)
                val rearLayer = layers[0]
                val frontLayer = layers[1]
                val rearWave = waveFillPath(
                    size.width, size.height, liquidProgress, rearLayer.phaseOffset,
                    amplitudeScale = rearLayer.amplitudeScale,
                    verticalOffset = -size.height * 0.012f
                )
                drawPath(rearWave, gaugeColor.copy(alpha = rearLayer.alpha))

                val frontWave = waveFillPath(
                    size.width, size.height, liquidProgress, frontLayer.phaseOffset,
                    amplitudeScale = frontLayer.amplitudeScale
                )
                val fillTop = size.height * (1f - liquidProgress)
                drawPath(
                    frontWave,
                    Brush.verticalGradient(
                        colors = listOf(
                            gaugeColor.copy(alpha = WAVE_FRONT_FILL_ALPHA),
                            gaugeColor.copy(alpha = WAVE_FRONT_FILL_ALPHA),
                            gaugeColor.copy(alpha = 0.92f)
                        ),
                        startY = fillTop.coerceAtLeast(0f),
                        endY = size.height
                    )
                )
                val crest = waveCrestPath(
                    size.width, size.height, liquidProgress, frontLayer.phaseOffset,
                    amplitudeScale = frontLayer.amplitudeScale
                )
                drawPath(crest, Color.White.copy(alpha = 0.9f), style = Stroke(1.25.dp.toPx(), cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f), Color.Transparent)
                    ),
                    -150f, 105f, false, circleTopLeft, circleSize,
                    style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    Color.White.copy(alpha = 0.16f),
                    28f, 58f, false, circleTopLeft, circleSize,
                    style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (animateEffects && animation != ChargingArcAnimation.STATIC) {
                val orbitStart = -90f + wavePhase * 57.29578f
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, gaugeColor.copy(alpha = 0.68f), Color.White.copy(alpha = 0.92f), Color.Transparent)
                    ),
                    orbitStart, 30f, false, circleTopLeft, circleSize,
                    style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            val borderStroke = 1.5.dp.toPx()
            drawCircle(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.72f), borderColor.copy(alpha = 0.72f), Color.White.copy(alpha = 0.28f)),
                    start = Offset(left, top),
                    end = Offset(left + diameter, top + diameter)
                ),
                (diameter / 2f - borderStroke / 2f).coerceAtLeast(0f),
                center,
                style = Stroke(borderStroke)
            )
        }
        GaugePercentageLabel(percentageLabel, 21.sp)
    }
}

@Composable
private fun GaugePercentageLabel(label: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val textMeasurer = rememberTextMeasurer()
    val outlineWidth = with(LocalDensity.current) { GAUGE_PERCENTAGE_OUTLINE_WIDTH_DP.dp.toPx() }
    val textStyle = remember(fontSize) { TextStyle(fontSize = fontSize, fontWeight = FontWeight.ExtraBold) }
    val layout = remember(textMeasurer, label, textStyle) {
        textMeasurer.measure(text = label, style = textStyle, softWrap = false, maxLines = 1)
    }
    Canvas(Modifier.fillMaxSize()) {
        val origin = Offset(
            x = (size.width - layout.size.width) / 2f,
            y = (size.height - layout.size.height) / 2f
        )
        drawText(layout, topLeft = origin, color = com.superdenot.powerguard.ui.theme.GaugePercentageOutline,
            drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round))
        drawText(layout, topLeft = origin, color = Color.White, drawStyle = Fill)
    }
}

private fun waveY(
    x: Float,
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    amplitudeScale: Float = 1f,
    verticalOffset: Float = 0f
): Float {
    val normalized = if (width <= 0f) 0f else x / width
    val amplitude = height * 0.045f * amplitudeScale
    val baseline = height * (1f - progress.coerceIn(0f, 1f))
    return baseline + verticalOffset + amplitude * sin(normalized * (2f * PI).toFloat() * 1.25f + phase)
}

private fun waveFillPath(
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    amplitudeScale: Float = 1f,
    verticalOffset: Float = 0f
): Path = Path().apply {
    moveTo(0f, waveY(0f, width, height, progress, phase, amplitudeScale, verticalOffset))
    for (step in 1..48) {
        val x = width * step / 48f
        lineTo(x, waveY(x, width, height, progress, phase, amplitudeScale, verticalOffset))
    }
    lineTo(width, height)
    lineTo(0f, height)
    close()
}

private fun waveCrestPath(
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    amplitudeScale: Float = 1f
): Path = Path().apply {
    moveTo(0f, waveY(0f, width, height, progress, phase, amplitudeScale))
    for (step in 1..48) {
        val x = width * step / 48f
        lineTo(x, waveY(x, width, height, progress, phase, amplitudeScale))
    }
}
