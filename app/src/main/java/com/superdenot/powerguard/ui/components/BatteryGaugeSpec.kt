package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BatterySemanticColors

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

fun segmentedGaugeLitCount(levelPercent: Int): Int =
    (levelPercent.coerceIn(0, 100) / 5).coerceIn(0, 20)

/** Animation eligibility and style never change the measured Wave liquid fraction. */
@Suppress("UNUSED_PARAMETER")
fun waveLiquidFillLevel(progress: Float, animateEffects: Boolean, animation: ChargingArcAnimation): Float =
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
    animation: ChargingArcAnimation
): Boolean = isCharging &&
    isVisibleOnScreen &&
    !isCriticalThermal &&
    !isWarm &&
    animation != ChargingArcAnimation.STATIC
