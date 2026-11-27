package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
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
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier,
    forceAnimate: Boolean = false
) {
    val gaugeColor = resolveBatteryGaugeAccent(
        themeAccent = MaterialTheme.colorScheme.primary,
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
            animationStyle = animation,
            forceAnimate = forceAnimate
        )
        val phase = rememberGaugePhase(
            shouldAnimate = animateEffects,
            cycleDurationMillis = if (model == BatteryGaugeModel.WAVE) {
                WAVE_CYCLE_DURATION_MILLIS
            } else {
                DEFAULT_GAUGE_CYCLE_DURATION_MILLIS
            }
        )
        BatteryGaugeRenderer(
            progress = safeProgress,
            percentageLabel = percentageLabel,
            model = model,
            animation = animation,
            gaugeColor = gaugeColor,
            phase = phase,
            animateEffects = animateEffects,
            isCharging = isCharging
        )
    }
}

@Composable
internal fun rememberGaugePhase(
    shouldAnimate: Boolean,
    cycleDurationMillis: Int = DEFAULT_GAUGE_CYCLE_DURATION_MILLIS
): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(shouldAnimate, cycleDurationMillis) {
        if (!shouldAnimate) {
            phase.floatValue = 0f
            return@LaunchedEffect
        }

        val safeCycleDurationMillis = cycleDurationMillis.coerceAtLeast(1)
        var lastFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val deltaMs = ((frameNanos - lastFrameNanos) / 1_000_000L).coerceIn(0L, 64L)
            lastFrameNanos = frameNanos
            phase.floatValue = advanceGaugePhase(
                currentPhase = phase.floatValue,
                deltaMs = deltaMs,
                cycleDurationMillis = safeCycleDurationMillis,
                shouldAnimate = shouldAnimate
            )
        }
    }
    return phase
}

private val GAUGE_PHASE_RADIANS_PER_CYCLE = (2f * PI).toFloat()

@Composable
private fun BatteryGaugeRenderer(

    progress: Float,
    percentageLabel: String,
    model: BatteryGaugeModel,
    animation: ChargingArcAnimation,
    gaugeColor: Color,
    phase: State<Float>,
    animateEffects: Boolean,
    isCharging: Boolean
) {
    val renderAnimation = if (animateEffects) animation else ChargingArcAnimation.STATIC
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val capsuleOutlineColor = MaterialTheme.colorScheme.outline.let { outline ->
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) outline.copy(alpha = 0.85f) else outline
    }
    when (model) {
        BatteryGaugeModel.ARC -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArcGauge(progress, gaugeColor, trackColor, phase.value * GAUGE_PHASE_RADIANS_PER_CYCLE, renderAnimation)
                }
                GaugeCenterLabel(percentageLabel)
            }
        }
        BatteryGaugeModel.CAPSULE -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCapsuleGauge(progress, gaugeColor, trackColor, capsuleOutlineColor, phase.value * GAUGE_PHASE_RADIANS_PER_CYCLE, renderAnimation)
                }
                GaugePercentageLabel(percentageLabel, 15.sp)
            }
        }
        BatteryGaugeModel.SEGMENTED -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawSegmentedGauge(progress, gaugeColor, trackColor, phase.value * GAUGE_PHASE_RADIANS_PER_CYCLE, renderAnimation)
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
    GaugePercentageLabel(label, 23.sp)
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
    val right = left + bodyWidth
    val outerCornerRadius = bodyWidth * 0.22f
    val body = Path().apply {
        addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(outerCornerRadius)))
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
        outlineColor,
        Offset(terminalLeft, terminalTop),
        Size(terminalWidth, terminalHeight),
        CornerRadius(terminalHeight / 2f),
        style = Stroke(CAPSULE_OUTER_STROKE_DP.dp.toPx())
    )

    val innerGeometry = capsuleInnerGeometry(
        outerCornerRadiusPx = outerCornerRadius,
        strokeWidthPx = CAPSULE_OUTER_STROKE_DP.dp.toPx(),
        innerGapPx = CAPSULE_INNER_GAP_DP.dp.toPx(),
        minimumCornerRadiusPx = CAPSULE_MIN_INNER_CORNER_RADIUS_DP.dp.toPx()
    )
    val innerLeft = left + innerGeometry.insetPx
    val innerTop = top + innerGeometry.insetPx
    val innerRight = right - innerGeometry.insetPx
    val innerBottom = bottom - innerGeometry.insetPx
    val innerWidth = (innerRight - innerLeft).coerceAtLeast(0f)
    val innerHeight = (innerBottom - innerTop).coerceAtLeast(0f)
    val innerRoundedRectPath = Path().apply {
        addRoundRect(
            RoundRect(
                innerLeft, innerTop, innerRight, innerBottom,
                CornerRadius(innerGeometry.cornerRadiusPx)
            )
        )
    }
    val fillHeight = innerHeight * progress.coerceIn(0f, 1f)
    val fillTop = innerBottom - fillHeight
    val phaseFraction = gaugeAnimationPhaseFraction(phase)

    // Track, liquid, and every liquid effect share the exact same concentric inner clip.
    clipPath(innerRoundedRectPath) {
        drawPath(innerRoundedRectPath, trackColor.copy(alpha = 0.78f))
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
            if (capsuleGlossEnabled(animation)) {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.White.copy(alpha = 0.16f))
                    ),
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
                    val features = capsuleAnimationFeatures(animation)
                    if (features.risingBubbles && innerWidth > 0f && fillHeight > 0f) {
                        repeat(CAPSULE_BUBBLE_COUNT) { index ->
                            val bubble = capsuleBubbleSpec(index, phaseFraction)
                            val drift = sin(phaseFraction * 2f * PI.toFloat() + index).toFloat() * 1.2f.dp.toPx()
                            val bubbleCenter = Offset(
                                innerLeft + innerWidth * bubble.xFraction + drift,
                                capsuleCometY(fillTop, innerBottom, bubble.riseFraction)
                            )
                            val radius = bubble.radiusDp.dp.toPx()
                            val bubbleAlpha = bubble.alpha * capsuleBubbleSurfaceFade(bubble.riseFraction)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(color.copy(alpha = bubbleAlpha * 0.42f), Color.Transparent),
                                    center = bubbleCenter,
                                    radius = radius * 2.6f
                                ),
                                radius = radius * 2.6f,
                                center = bubbleCenter
                            )
                            drawCircle(
                                color.copy(alpha = bubbleAlpha),
                                radius = radius,
                                center = bubbleCenter
                            )
                            drawCircle(
                                Color.White.copy(alpha = bubbleAlpha * 0.52f),
                                radius = radius * 0.34f,
                                center = bubbleCenter
                            )

                            val rippleAlpha = bubble.alpha * capsuleBubbleRippleProgress(bubble.riseFraction) * 0.48f
                            if (rippleAlpha > 0f) {
                                val rippleWidth = 8.dp.toPx()
                                val rippleHeight = 3.dp.toPx()
                                drawArc(
                                    Color.White.copy(alpha = rippleAlpha),
                                    0f,
                                    180f,
                                    false,
                                    Offset(bubbleCenter.x - rippleWidth / 2f, fillTop - rippleHeight / 2f),
                                    Size(rippleWidth, rippleHeight),
                                    style = Stroke(0.8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
                ChargingArcAnimation.COMET_HEAD -> {
                    val cometY = capsuleCometY(fillTop, innerBottom, phaseFraction)
                    val tailBottom = capsuleCometBeamTailBottom(cometY, innerBottom, 28.dp.toPx())
                    val beamHeight = (tailBottom - cometY).coerceAtLeast(1.dp.toPx())
                    val centerX = size.width / 2f
                    val glowWidth = 8.dp.toPx()
                    val coreWidth = 2.2.dp.toPx()
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.82f), color.copy(alpha = 0.66f), Color.Transparent),
                            startY = cometY,
                            endY = tailBottom.coerceAtLeast(cometY + 1f)
                        ),
                        Offset(centerX - glowWidth / 2f, cometY),
                        Size(glowWidth, beamHeight)
                    )
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0.72f), Color.Transparent),
                            startY = cometY,
                            endY = tailBottom.coerceAtLeast(cometY + 1f)
                        ),
                        Offset(centerX - coreWidth / 2f, cometY),
                        Size(coreWidth, beamHeight)
                    )
                    drawLine(
                        color.copy(alpha = 0.72f),
                        Offset(innerLeft + 2.dp.toPx(), fillTop),
                        Offset(innerLeft + innerWidth - 2.dp.toPx(), fillTop),
                        strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.96f),
                        Offset(innerLeft + 3.dp.toPx(), fillTop),
                        Offset(innerLeft + innerWidth - 3.dp.toPx(), fillTop),
                        strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round
                    )
                }
                ChargingArcAnimation.STATIC -> Unit
            }
        }
        if (animation != ChargingArcAnimation.STATIC) {
            drawArc(
                Color.White.copy(alpha = 0.18f), -125f, 70f, false,
                Offset(innerLeft, innerTop), Size(innerWidth, innerHeight),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
    drawPath(body, outlineColor, style = Stroke(CAPSULE_OUTER_STROKE_DP.dp.toPx()))
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
        val headAngle = Math.toRadians(
            (segmentedGaugeStartAngle(headIndex) + SEGMENTED_GAUGE_STEP_DEGREES / 2f).toDouble()
        )
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
    phase: State<Float>,
    animation: ChargingArcAnimation,
    animateEffects: Boolean,
    isCharging: Boolean
) {
    val features = waveAnimationFeatures(animation)
    val waveColors = waveLayerColors(gaugeColor)
    val cometBleed = WAVE_COMET_STYLE.glowRadiusDp.dp

    // The drawing surface extends beyond the gauge bounds so the outer comet glow is not clipped.
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .requiredSize(maxWidth + cometBleed * 2f, maxHeight + cometBleed * 2f)
        ) {
            val bleedPx = WAVE_COMET_STYLE.glowRadiusDp.dp.toPx()
            val diameter = (size.minDimension - bleedPx * 2f).coerceAtLeast(1f)
            val waveAmplitudePx = waveFrontAmplitudePx(
                diameterPx = diameter,
                maxAmplitudePx = MAX_FRONT_WAVE_AMPLITUDE_DP.dp.toPx()
            )
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f
            val circleTopLeft = Offset(left, top)
            val circleSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val circle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(left, top, left + diameter, top + diameter))
            }
            val wavePhase = if (animateEffects && features.animatedSurface) {
                phase.value * GAUGE_PHASE_RADIANS_PER_CYCLE
            } else {
                0f
            }
            val phaseFraction = gaugeAnimationPhaseFraction(wavePhase)
            val sampleStepPx = WAVE_SAMPLE_STEP_DP.dp.toPx()
            val liquidProgress = waveLiquidFillLevel(progress, isCharging, animation)
            val liquidBottom = top + diameter
            val fillTop = top + diameter * (1f - liquidProgress)

            drawCircle(trackColor, diameter / 2f, center)
            clipPath(circle) {
                val rearWaveOffset = -backWaveElevationOffsetDp(liquidProgress).dp.toPx()
                val rearPhase = waveRearPhase(wavePhase)
                val rearWave = waveFillPath(
                    diameter, diameter, liquidProgress, rearPhase,
                    amplitudePx = waveAmplitudePx,
                    amplitudeScale = BACK_WAVE_AMPLITUDE_RATIO,
                    verticalOffset = rearWaveOffset,
                    sampleStepPx = sampleStepPx,
                    origin = Offset(left, top)
                )
                drawPath(rearWave, waveColors.rear)

                val rearCrest = waveCrestPath(
                    diameter, diameter, liquidProgress, rearPhase,
                    amplitudePx = waveAmplitudePx,
                    amplitudeScale = BACK_WAVE_AMPLITUDE_RATIO,
                    verticalOffset = rearWaveOffset,
                    sampleStepPx = sampleStepPx,
                    origin = Offset(left, top)
                )
                drawPath(
                    rearCrest,
                    waveColors.rearCrest,
                    style = Stroke(WAVE_REAR_CREST_STROKE_DP.dp.toPx(), cap = StrokeCap.Round)
                )

                val frontWave = waveFillPath(
                    diameter, diameter, liquidProgress, wavePhase,
                    amplitudePx = waveAmplitudePx,
                    sampleStepPx = sampleStepPx,
                    origin = Offset(left, top)
                )
                drawPath(
                    frontWave,
                    Brush.verticalGradient(
                        colors = listOf(
                            waveColors.frontSurface,
                            waveColors.frontBottom
                        ),
                        startY = fillTop.coerceAtLeast(top),
                        endY = liquidBottom
                    )
                )

                // Sweep remains a rising light ribbon; Wave Segmen keeps its own seven-bubble treatment.
                if (animateEffects && liquidProgress > 0f && features.upwardSweep) {
                    clipPath(frontWave) {
                        val availableHeight = (liquidBottom - fillTop).coerceAtLeast(0f)
                        val sweepCenterY = liquidBottom - availableHeight * phaseFraction
                        val bandHalfHeight = 10.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    waveColors.frontHighlight.copy(alpha = 0.24f),
                                    waveColors.frontHighlight.copy(alpha = 0.62f),
                                    waveColors.frontHighlight.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                startY = sweepCenterY - bandHalfHeight,
                                endY = sweepCenterY + bandHalfHeight
                            ),
                            topLeft = Offset(left, sweepCenterY - bandHalfHeight),
                            size = Size(diameter, bandHalfHeight * 2f)
                        )
                    }
                }
                if (animateEffects && liquidProgress > 0f && features.risingBubbles) {
                    clipPath(frontWave) {
                        repeat(WAVE_BUBBLE_COUNT) { index ->
                            val stagger = (phaseFraction + index * 0.173f) % 1f
                            val lane = index % 4
                            val laneX = 0.20f + lane * 0.20f
                            val x = left + diameter * (laneX + sin(index * 1.7f) * 0.025f)
                            val availableHeight = (liquidBottom - fillTop).coerceAtLeast(0f)
                            val y = liquidBottom - availableHeight * stagger
                            val radius = (1.35f + (index % 3) * 0.42f).dp.toPx()
                            val bubbleCenter = Offset(x, y)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(waveColors.frontHighlight.copy(alpha = 0.54f), gaugeColor.copy(alpha = 0.24f), Color.Transparent),
                                    center = bubbleCenter,
                                    radius = radius * 2.5f
                                ),
                                radius = radius * 2.5f,
                                center = bubbleCenter
                            )
                            drawCircle(
                                waveColors.frontHighlight.copy(alpha = 0.76f),
                                radius = radius * 0.58f,
                                center = bubbleCenter
                            )
                            drawCircle(
                                gaugeColor.copy(alpha = 0.42f),
                                radius = radius * 1.45f,
                                center = bubbleCenter,
                                style = Stroke(0.8.dp.toPx())
                            )
                            drawCircle(
                                waveColors.frontHighlight.copy(alpha = 0.22f),
                                radius = radius * 2.05f,
                                center = bubbleCenter,
                                style = Stroke(0.65.dp.toPx())
                            )
                        }
                    }
                }

                val crest = waveCrestPath(
                    diameter, diameter, liquidProgress, wavePhase,
                    amplitudePx = waveAmplitudePx,
                    sampleStepPx = sampleStepPx,
                    origin = Offset(left, top)
                )
                val crestPulse = if (animateEffects && animation == ChargingArcAnimation.ENERGY_SWEEP) {
                    (0.68f + 0.28f * ((sin(wavePhase * 2f) + 1f) / 2f)).coerceIn(0f, 1f)
                } else {
                    0.9f
                }
                drawPath(
                    crest,
                    waveColors.meniscus.copy(alpha = waveColors.meniscus.alpha * crestPulse),
                    style = Stroke(WAVE_MENISCUS_STROKE_DP.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    waveColors.frontHighlight.copy(alpha = 0.16f), 28f, 58f, false,
                    circleTopLeft, circleSize,
                    style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            val rimStrokePx = WAVE_RIM_STROKE_DP.dp.toPx()
            drawCircle(
                Brush.linearGradient(
                    listOf(waveColors.frontHighlight.copy(alpha = 0.72f), borderColor.copy(alpha = 0.72f), waveColors.frontHighlight.copy(alpha = 0.28f)),
                    start = Offset(left, top), end = Offset(left + diameter, top + diameter)
                ),
                (diameter / 2f - rimStrokePx / 2f).coerceAtLeast(0f),
                center,
                style = Stroke(rimStrokePx)
            )

            // Outer Comet is intentionally rendered after the liquid clip and on the glass-ring radius.
            if (animateEffects && features.outerComet && !WAVE_COMET_STYLE.clipToLiquid) {
                val orbitRadius = waveCometOrbitRadius(diameter / 2f, rimStrokePx)
                val orbitTopLeft = Offset(center.x - orbitRadius, center.y - orbitRadius)
                val orbitSize = Size(orbitRadius * 2f, orbitRadius * 2f)
                val orbitStart = -90f + phaseFraction * 360f
                val tailColor = androidx.compose.ui.graphics.lerp(
                    gaugeColor, Color.White, WAVE_COMET_TAIL_HIGHLIGHT_BLEND
                )
                val headColor = androidx.compose.ui.graphics.lerp(
                    gaugeColor, Color.White, WAVE_COMET_HEAD_HIGHLIGHT_BLEND
                )
                drawArc(
                    gaugeColor.copy(alpha = 0.28f),
                    orbitStart,
                    WAVE_COMET_STYLE.tailSweepDegrees,
                    false,
                    orbitTopLeft,
                    orbitSize,
                    style = Stroke(WAVE_COMET_STYLE.haloStrokeDp.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    Brush.sweepGradient(
                        listOf(Color.Transparent, tailColor.copy(alpha = 0.32f), tailColor, Color.Transparent),
                        center = center
                    ),
                    orbitStart,
                    WAVE_COMET_STYLE.tailSweepDegrees,
                    false,
                    orbitTopLeft,
                    orbitSize,
                    style = Stroke(WAVE_COMET_STYLE.tailStrokeDp.dp.toPx(), cap = StrokeCap.Round)
                )
                val headAngle = Math.toRadians((orbitStart + WAVE_COMET_STYLE.tailSweepDegrees).toDouble())
                val head = Offset(
                    center.x + cos(headAngle).toFloat() * orbitRadius,
                    center.y + sin(headAngle).toFloat() * orbitRadius
                )
                val glowRadius = WAVE_COMET_STYLE.glowRadiusDp.dp.toPx()
                drawCircle(
                    Brush.radialGradient(
                        listOf(headColor.copy(alpha = 0.72f), tailColor.copy(alpha = 0.24f), Color.Transparent),
                        center = head,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = head
                )
                drawCircle(headColor, WAVE_COMET_STYLE.headRadiusDp.dp.toPx(), head)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GaugePercentageLabel(percentageLabel, 21.sp)
        }
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
    amplitudePx: Float,
    amplitudeScale: Float = 1f,
    verticalOffset: Float = 0f
): Float {
    val normalized = if (width <= 0f) 0f else x / width
    val level = progress.coerceIn(0f, 1f)
    val envelope = waveSurfaceEnvelope(level)
    val amplitude = amplitudePx * envelope * amplitudeScale
    val baseline = height * (1f - level)
    val angle = normalized * (2f * PI).toFloat()
    return baseline + verticalOffset + amplitude * waveHarmonicOffset(angle, phase)
}

private fun waveFillPath(
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    amplitudePx: Float,
    amplitudeScale: Float = 1f,
    verticalOffset: Float = 0f,
    sampleStepPx: Float,
    origin: Offset = Offset.Zero
): Path = Path().apply {
    val sampleCount = waveSampleCount(width, sampleStepPx)
    moveTo(
        origin.x,
        origin.y + waveY(0f, width, height, progress, phase, amplitudePx, amplitudeScale, verticalOffset)
    )
    for (step in 1..sampleCount) {
        val x = width * step / sampleCount
        lineTo(
            origin.x + x,
            origin.y + waveY(x, width, height, progress, phase, amplitudePx, amplitudeScale, verticalOffset)
        )
    }
    lineTo(origin.x + width, origin.y + height)
    lineTo(origin.x, origin.y + height)
    close()
}

private fun waveCrestPath(
    width: Float,
    height: Float,
    progress: Float,
    phase: Float,
    amplitudePx: Float,
    amplitudeScale: Float = 1f,
    verticalOffset: Float = 0f,
    sampleStepPx: Float,
    origin: Offset = Offset.Zero
): Path = Path().apply {
    val sampleCount = waveSampleCount(width, sampleStepPx)
    moveTo(
        origin.x,
        origin.y + waveY(0f, width, height, progress, phase, amplitudePx, amplitudeScale, verticalOffset)
    )
    for (step in 1..sampleCount) {
        val x = width * step / sampleCount
        lineTo(
            origin.x + x,
            origin.y + waveY(x, width, height, progress, phase, amplitudePx, amplitudeScale, verticalOffset)
        )
    }
}
