package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
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

const val WAVE_REAR_PHASE_OFFSET = 1.15f
const val WAVE_REAR_AMPLITUDE_SCALE = 0.68f
const val WAVE_REAR_FILL_ALPHA = 0.28f
const val WAVE_FRONT_FILL_ALPHA = 0.82f

data class WaveAnimationFeatures(
    val animatedSurface: Boolean,
    val diagonalSweep: Boolean,
    val horizontalStrata: Boolean,
    val outerComet: Boolean
)

/** Keeps the four Wave animation choices visually distinct and testable. */
fun waveAnimationFeatures(animation: ChargingArcAnimation): WaveAnimationFeatures = when (animation) {
    ChargingArcAnimation.ENERGY_SWEEP -> WaveAnimationFeatures(true, true, false, false)
    ChargingArcAnimation.SEGMENTED_FLOW -> WaveAnimationFeatures(true, false, true, false)
    ChargingArcAnimation.COMET_HEAD -> WaveAnimationFeatures(true, false, false, true)
    ChargingArcAnimation.STATIC -> WaveAnimationFeatures(false, false, false, false)
}

/** Charging state and animation style never change the measured Wave liquid fraction. */
@Suppress("UNUSED_PARAMETER")
fun waveLiquidFillLevel(progress: Float, isCharging: Boolean, animation: ChargingArcAnimation): Float =
    progress.coerceIn(0f, 1f)

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
    isVisibleOnScreen: Boolean,
    isCriticalThermal: Boolean,
    isWarm: Boolean,
    animation: ChargingArcAnimation,
    forceAnimate: Boolean = false
): Boolean = (isCharging || forceAnimate) &&
    isVisibleOnScreen &&
    !isCriticalThermal &&
    !isWarm &&
    animation != ChargingArcAnimation.STATIC
