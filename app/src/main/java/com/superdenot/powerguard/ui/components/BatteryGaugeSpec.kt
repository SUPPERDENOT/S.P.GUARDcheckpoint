package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.sin

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

const val FRONT_WAVE_AMPLITUDE_RATIO = 0.038f
const val MAX_FRONT_WAVE_AMPLITUDE_DP = 5.2f
const val WAVE_CYCLE_DURATION_MILLIS = 3_000
const val DEFAULT_GAUGE_CYCLE_DURATION_MILLIS = 2_400
const val WAVE_SAMPLE_STEP_DP = 2f
const val WAVE_RIM_STROKE_DP = 1.5f
const val BACK_WAVE_ELEVATION_DP = 2.6f
const val BACK_WAVE_AMPLITUDE_RATIO = 0.82f
const val WAVE_ENVELOPE_ZERO_START = 0.02f
const val WAVE_ENVELOPE_FULL_START = 0.12f
const val WAVE_ENVELOPE_FULL_END = 0.88f
const val WAVE_ENVELOPE_ZERO_END = 0.98f
const val WAVE_REAR_PHASE_OFFSET_DEGREES = 135f
const val WAVE_REAR_PHASE_OFFSET = 2.3561945f
const val WAVE_REAR_DIRECTION = -1f
const val WAVE_REAR_FILL_ALPHA = 0.30f
const val WAVE_REAR_CREST_ALPHA = 0.25f
const val WAVE_REAR_CREST_STROKE_DP = 0.75f
const val WAVE_FRONT_SURFACE_ALPHA = 0.95f
const val WAVE_FRONT_BOTTOM_ALPHA = 0.76f
const val WAVE_FRONT_HIGHLIGHT_BLEND = 0.32f
const val WAVE_MENISCUS_STROKE_DP = 1.2f
const val WAVE_SECOND_HARMONIC_PHASE_OFFSET = 0.7853982f
const val WAVE_BUBBLE_COUNT = 7
const val WAVE_COMET_TAIL_DEGREES = 110f
const val WAVE_COMET_HALO_STROKE_DP = 6f
const val WAVE_COMET_TAIL_STROKE_DP = 3f
const val WAVE_COMET_HEAD_RADIUS_DP = 3.5f
const val WAVE_COMET_GLOW_RADIUS_DP = 6f
const val WAVE_COMET_TAIL_HIGHLIGHT_BLEND = 0.25f
const val WAVE_COMET_HEAD_HIGHLIGHT_BLEND = 0.48f

const val CAPSULE_BUBBLE_COUNT = 6
const val CAPSULE_BUBBLE_MIN_RADIUS_DP = 1.6f
const val CAPSULE_BUBBLE_MAX_RADIUS_DP = 3f
const val CAPSULE_BUBBLE_MIN_ALPHA = 0.28f
const val CAPSULE_BUBBLE_MAX_ALPHA = 0.58f

data class WaveLayerColors(
    val rear: Color,
    val rearCrest: Color,
    val frontHighlight: Color,
    val frontSurface: Color,
    val frontBottom: Color,
    val meniscus: Color
)

fun waveMeniscusColor(accentColor: Color): Color =
    if (accentColor.luminance() > 0.75f) {
        accentColor.copy(alpha = 1f)
    } else {
        lerp(accentColor, Color.White, 0.18f).copy(alpha = 0.90f)
    }

fun waveLayerColors(accentColor: Color): WaveLayerColors = WaveLayerColors(
    rear = accentColor.copy(alpha = WAVE_REAR_FILL_ALPHA),
    rearCrest = accentColor.copy(alpha = WAVE_REAR_CREST_ALPHA),
    frontHighlight = lerp(accentColor, Color.White, WAVE_FRONT_HIGHLIGHT_BLEND).copy(alpha = WAVE_FRONT_SURFACE_ALPHA),
    frontSurface = accentColor.copy(alpha = WAVE_FRONT_SURFACE_ALPHA),
    frontBottom = accentColor.copy(alpha = WAVE_FRONT_BOTTOM_ALPHA),
    meniscus = waveMeniscusColor(accentColor)
)

/** One level envelope controls both wave amplitudes and rear-layer elevation. */
fun waveSurfaceEnvelope(level: Float): Float {
    val safeLevel = level.coerceIn(0f, 1f)
    val fadeIn = smoothStep01(
        (safeLevel - WAVE_ENVELOPE_ZERO_START) /
            (WAVE_ENVELOPE_FULL_START - WAVE_ENVELOPE_ZERO_START)
    )
    val fadeOut = smoothStep01(
        (safeLevel - WAVE_ENVELOPE_FULL_END) /
            (WAVE_ENVELOPE_ZERO_END - WAVE_ENVELOPE_FULL_END)
    )
    return (fadeIn * (1f - fadeOut)).coerceIn(0f, 1f)
}

private fun smoothStep01(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun backWaveElevationTaper(level: Float): Float = waveSurfaceEnvelope(level)

fun backWaveElevationOffsetDp(level: Float): Float =
    BACK_WAVE_ELEVATION_DP * waveSurfaceEnvelope(level)

fun waveFrontAmplitudePx(diameterPx: Float, maxAmplitudePx: Float): Float =
    min(diameterPx.coerceAtLeast(0f) * FRONT_WAVE_AMPLITUDE_RATIO, maxAmplitudePx.coerceAtLeast(0f))

/** Integer harmonics keep the sampled surface exactly periodic at 360 degrees. */
fun waveHarmonicOffset(angleRad: Float, phaseRad: Float): Float =
    0.82f * sin(angleRad + phaseRad) +
        0.18f * sin(2f * angleRad - phaseRad + WAVE_SECOND_HARMONIC_PHASE_OFFSET)

/** Rear phase runs opposite the front phase; the 0/2π endpoints are cycle-equivalent. */
fun waveRearPhase(frontPhase: Float): Float =
    WAVE_REAR_DIRECTION * frontPhase + WAVE_REAR_PHASE_OFFSET


fun waveSampleCount(widthPx: Float, sampleStepPx: Float): Int =
    if (widthPx <= 0f || sampleStepPx <= 0f) 1
    else ceil(widthPx / sampleStepPx).toInt().coerceAtLeast(1)

fun waveCometOrbitRadius(radiusPx: Float, rimStrokePx: Float): Float =
    (radiusPx - rimStrokePx / 2f).coerceAtLeast(0f)

data class CapsuleBubbleSpec(
    val xFraction: Float,
    val riseFraction: Float,
    val radiusDp: Float,
    val alpha: Float
)

fun capsuleBubbleSpec(index: Int, phaseFraction: Float): CapsuleBubbleSpec {
    val safeIndex = index.coerceIn(0, CAPSULE_BUBBLE_COUNT - 1)
    val xFraction = when (safeIndex) {
        0 -> 0.26f
        1 -> 0.70f
        2 -> 0.43f
        3 -> 0.78f
        4 -> 0.22f
        else -> 0.57f
    }
    val radius = when (safeIndex % 3) {
        0 -> CAPSULE_BUBBLE_MIN_RADIUS_DP
        1 -> 2.3f
        else -> CAPSULE_BUBBLE_MAX_RADIUS_DP
    }
    val alpha = when (safeIndex % 4) {
        0 -> CAPSULE_BUBBLE_MIN_ALPHA
        1 -> 0.38f
        2 -> 0.48f
        else -> CAPSULE_BUBBLE_MAX_ALPHA
    }
    val staggeredPhase = (phaseFraction.coerceIn(0f, 1f) + safeIndex / CAPSULE_BUBBLE_COUNT.toFloat()) % 1f
    return CapsuleBubbleSpec(xFraction, staggeredPhase, radius, alpha)
}

/** Smoothly fades bubbles into the meniscus during the final 28% of their rise. */
fun capsuleBubbleSurfaceFade(riseFraction: Float): Float {
    val t = ((riseFraction.coerceIn(0f, 1f) - 0.72f) / 0.28f).coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 1f - smooth
}

fun capsuleBubbleRippleProgress(riseFraction: Float): Float {
    val t = ((riseFraction.coerceIn(0f, 1f) - 0.72f) / 0.28f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

data class CapsuleAnimationFeatures(
    val risingBubbles: Boolean,
    val upwardSweep: Boolean,
    val verticalComet: Boolean,
    val animatedSurface: Boolean
)

fun capsuleAnimationFeatures(animation: ChargingArcAnimation): CapsuleAnimationFeatures =
    CapsuleAnimationFeatures(
        risingBubbles = animation == ChargingArcAnimation.SEGMENTED_FLOW,
        upwardSweep = animation == ChargingArcAnimation.ENERGY_SWEEP,
        verticalComet = animation == ChargingArcAnimation.COMET_HEAD,
        animatedSurface = animation != ChargingArcAnimation.STATIC
    )

data class WaveCometStyle(
    val clipToLiquid: Boolean,
    val tailSweepDegrees: Float,
    val haloStrokeDp: Float,
    val tailStrokeDp: Float,
    val headRadiusDp: Float,
    val glowRadiusDp: Float
)

val WAVE_COMET_STYLE = WaveCometStyle(
    clipToLiquid = false,
    tailSweepDegrees = WAVE_COMET_TAIL_DEGREES,
    haloStrokeDp = WAVE_COMET_HALO_STROKE_DP,
    tailStrokeDp = WAVE_COMET_TAIL_STROKE_DP,
    headRadiusDp = WAVE_COMET_HEAD_RADIUS_DP,
    glowRadiusDp = WAVE_COMET_GLOW_RADIUS_DP
)

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

@Suppress("UNUSED_PARAMETER")
fun resolveBatteryGaugeAccent(
    themeAccent: Color,
    isCharging: Boolean
): Color = themeAccent

fun resolveBatteryGaugeColor(
    semanticColors: BatterySemanticColors,
    isCriticalThermal: Boolean,
    isWarm: Boolean,
    isCharging: Boolean
): Color = when {
    isCriticalThermal -> semanticColors.criticalThermal
    isWarm -> semanticColors.warm
    isCharging -> semanticColors.charging
    else -> semanticColors.normal
}

fun shouldAnimateBatteryGauge(
    isCharging: Boolean,
    animationStyle: ChargingArcAnimation,
    forceAnimate: Boolean = false
): Boolean = (isCharging || forceAnimate) &&
    animationStyle != ChargingArcAnimation.STATIC
