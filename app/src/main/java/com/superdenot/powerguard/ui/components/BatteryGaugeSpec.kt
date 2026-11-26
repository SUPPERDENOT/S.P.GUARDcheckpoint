package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import kotlin.math.roundToInt
import kotlin.math.min

enum class BatteryGaugeModel(val key: String, val label: String) {
    ARC("arc", "Busur"),
    CAPSULE("capsule", "Kapsul"),
    SEGMENTED("segmented", "Blok"),
    WAVE("wave", "Wave");

    companion object {
        fun fromKey(raw: String?): BatteryGaugeModel =
            values().firstOrNull { it.key == raw } ?: ARC
    }
}

enum class ChargingArcAnimation(val key: String, val label: String) {
    ENERGY_SWEEP("energy_sweep", "Sweep"),
    SEGMENTED_FLOW("segmented_flow", "Segmen"),
    COMET_HEAD("comet_head", "Comet"),
    STATIC("static", "Statis");

    companion object {
        fun fromKey(raw: String?): ChargingArcAnimation =
            values().firstOrNull { it.key == raw } ?: ENERGY_SWEEP
    }
}

const val SEGMENTED_GAUGE_BLOCK_COUNT = 24
const val SEGMENTED_GAUGE_STEP_DEGREES = 360f / SEGMENTED_GAUGE_BLOCK_COUNT

fun segmentedGaugeLitCount(levelPercent: Int): Int =
    ((levelPercent.coerceIn(0, 100) / 100f) * SEGMENTED_GAUGE_BLOCK_COUNT)
        .roundToInt().coerceIn(0, SEGMENTED_GAUGE_BLOCK_COUNT)

const val SEGMENTED_GAUGE_START_ANGLE_DEGREES = 90f

fun segmentedGaugeStartAngle(index: Int): Float =
    SEGMENTED_GAUGE_START_ANGLE_DEGREES +
        index.coerceIn(0, SEGMENTED_GAUGE_BLOCK_COUNT - 1) * SEGMENTED_GAUGE_STEP_DEGREES

fun gaugeAnimationPhaseFraction(phase: Float): Float =
    (phase / (2f * Math.PI.toFloat())).coerceIn(0f, 1f)

fun capsuleCometY(topY: Float, bottomY: Float, phaseFraction: Float): Float =
    bottomY - (bottomY - topY) * phaseFraction.coerceIn(0f, 1f)

/** Keeps the vertical energy tail inside the capsule's liquid column. */
fun capsuleCometBeamTailBottom(cometY: Float, bottomY: Float, maxLength: Float): Float =
    (cometY + maxLength.coerceAtLeast(0f)).coerceAtMost(bottomY)

fun capsuleGlossEnabled(animation: ChargingArcAnimation): Boolean =
    animation != ChargingArcAnimation.STATIC

/** Returns the brightness of an active block; distance is measured backward from a clockwise head. */
fun segmentedGaugeTrailAlpha(index: Int, headIndex: Int, activeCount: Int): Float {
    if (activeCount <= 0) return 0f
    val safeCount = activeCount.coerceIn(1, SEGMENTED_GAUGE_BLOCK_COUNT)
    val distanceBehindHead = (headIndex - index + safeCount) % safeCount
    val trailLength = min(5, safeCount)
    return if (distanceBehindHead == 0) 1f
    else if (distanceBehindHead <= trailLength) {
        0.42f + 0.58f * (1f - distanceBehindHead.toFloat() / (trailLength + 1f))
    } else 0.82f
}

const val CAPSULE_OUTER_STROKE_DP = 1.8f
const val CAPSULE_INNER_GAP_DP = 1.5f
const val CAPSULE_MIN_INNER_CORNER_RADIUS_DP = 4f

data class CapsuleInnerGeometry(
    val insetPx: Float,
    val cornerRadiusPx: Float
)

/** Computes a concentric inner capsule contour from the outer stroke and breathing gap. */
fun capsuleInnerGeometry(
    outerCornerRadiusPx: Float,
    strokeWidthPx: Float,
    innerGapPx: Float,
    minimumCornerRadiusPx: Float
): CapsuleInnerGeometry {
    val inset = strokeWidthPx + innerGapPx
    return CapsuleInnerGeometry(
        insetPx = inset,
        cornerRadiusPx = (outerCornerRadiusPx - inset).coerceAtLeast(minimumCornerRadiusPx)
    )
}

const val WAVE_AMPLITUDE_DP = 6.5f
const val WAVE_CYCLES = 1.6f
const val WAVE_REAR_PHASE_OFFSET_DEGREES = 135f
const val WAVE_REAR_PHASE_OFFSET = 2.3561945f
const val WAVE_REAR_DIRECTION = -1f
const val WAVE_REAR_AMPLITUDE_SCALE = 0.76f
const val WAVE_REAR_FILL_ALPHA = 0.88f
const val WAVE_FRONT_FILL_ALPHA = 0.88f
const val WAVE_FRONT_HIGHLIGHT_BLEND = 0.32f
const val WAVE_FRONT_SHADOW_BLEND = 0.22f
const val WAVE_MENISCUS_HIGHLIGHT_BLEND = 0.35f
const val WAVE_MENISCUS_ALPHA = 0.75f
const val WAVE_MENISCUS_STROKE_DP = 1.2f
const val WAVE_BUBBLE_COUNT = 7
const val WAVE_COMET_TAIL_DEGREES = 100f

data class WaveLayerColors(
    val rear: Color,
    val frontHighlight: Color,
    val frontBase: Color,
    val frontShadow: Color,
    val meniscus: Color
)

fun waveLayerColors(accentColor: Color): WaveLayerColors = WaveLayerColors(
    rear = lerp(accentColor, Color.Black, 0.42f).copy(alpha = WAVE_REAR_FILL_ALPHA),
    frontHighlight = lerp(accentColor, Color.White, WAVE_FRONT_HIGHLIGHT_BLEND),
    frontBase = accentColor,
    frontShadow = lerp(accentColor, Color.Black, WAVE_FRONT_SHADOW_BLEND),
    meniscus = lerp(accentColor, Color.White, WAVE_MENISCUS_HIGHLIGHT_BLEND)
        .copy(alpha = WAVE_MENISCUS_ALPHA)
)

fun waveRearPhase(frontPhase: Float): Float =
    frontPhase * WAVE_REAR_DIRECTION + WAVE_REAR_PHASE_OFFSET

data class WaveAnimationFeatures(
    val animatedSurface: Boolean,
    val upwardSweep: Boolean,
    val risingBubbles: Boolean,
    val outerComet: Boolean
)

/** Keeps the four Wave animation choices visually distinct and testable. */
fun waveAnimationFeatures(animation: ChargingArcAnimation): WaveAnimationFeatures = when (animation) {
    ChargingArcAnimation.ENERGY_SWEEP -> WaveAnimationFeatures(true, true, false, false)
    ChargingArcAnimation.SEGMENTED_FLOW -> WaveAnimationFeatures(true, false, true, false)
    ChargingArcAnimation.COMET_HEAD -> WaveAnimationFeatures(true, false, false, true)
    ChargingArcAnimation.STATIC -> WaveAnimationFeatures(false, false, false, false)
}

/** Advances a normalized 0..1 gauge phase using a bounded frame delta. */
fun advanceGaugePhase(
    currentPhase: Float,
    deltaMs: Long,
    cycleDurationMillis: Int,
    shouldAnimate: Boolean
): Float {
    if (!shouldAnimate) return 0f
    val safeCycleDurationMillis = cycleDurationMillis.coerceAtLeast(1)
    val safeDeltaMs = deltaMs.coerceIn(0L, 64L)
    return (currentPhase + safeDeltaMs.toFloat() / safeCycleDurationMillis.toFloat()) % 1f
}

/** Charging state and animation style never change the measured Wave liquid fraction. */
@Suppress("UNUSED_PARAMETER")
fun waveLiquidFillLevel(progress: Float, isCharging: Boolean, animation: ChargingArcAnimation): Float =
    progress.coerceIn(0f, 1f)

fun resolveBatteryGaugeAccent(
    semanticColors: BatterySemanticColors,
    isCharging: Boolean
): Color = if (isCharging) semanticColors.charging else semanticColors.normal

fun resolveBatteryGaugeColor(
    semanticColors: BatterySemanticColors,
    isCriticalThermal: Boolean,
    isWarm: Boolean,
    isCharging: Boolean
): Color = when {
    isCriticalThermal -> semanticColors.criticalThermal
    isWarm -> semanticColors.warm
    else -> resolveBatteryGaugeAccent(semanticColors, isCharging)
}

fun shouldAnimateBatteryGauge(
    isCharging: Boolean,
    animationStyle: ChargingArcAnimation,
    forceAnimate: Boolean = false
): Boolean = (isCharging || forceAnimate) &&
    animationStyle != ChargingArcAnimation.STATIC
