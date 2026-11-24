package com.superdenot.powerguard.domain.analytics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class ChargingThermalSample(
    val timestampMillis: Long,
    val temperatureCelsius: Float,
    val isTemperatureAvailable: Boolean,
    val currentMa: Int,
    val isCurrentAvailable: Boolean,
    val voltageMv: Int,
    val isVoltageAvailable: Boolean,
    val isChargingStatusAvailable: Boolean,
    val isCharging: Boolean
)

enum class ChargingPhaseIndicator(val label: String) {
    CC("Arus Penuh · indikasi CC"),
    CV("Top-Off · indikasi CV"),
    UNKNOWN("Fase belum dapat dipastikan")
}

data class ChargingThermalSummary(
    val safeMinutes: Int,
    val warmMinutes: Int,
    val highMinutes: Int,
    val phase: ChargingPhaseIndicator
) {
    val hasTemperatureData: Boolean
        get() = safeMinutes + warmMinutes + highMinutes > 0
}

/** Battery-side sample heuristic; this is not charger-protocol detection. */
object ChargingThermalPhaseAnalyzer {
    const val WARM_TEMPERATURE_CELSIUS = 38f
    const val HIGH_TEMPERATURE_CELSIUS = 40f
    private const val MAX_SAMPLE_GAP_MILLIS = 120_000L
    private const val MIN_PHASE_SAMPLES = 6
    private const val CC_MIN_VOLTAGE_RISE_MV = 40
    private const val CC_MAX_CURRENT_CHANGE_RATIO = 0.15
    private const val CV_MAX_VOLTAGE_CHANGE_MV = 20
    private const val CV_MIN_CURRENT_DROP_MA = 80

    fun analyze(samples: List<ChargingThermalSample>): ChargingThermalSummary {
        val ordered = samples.sortedBy { it.timestampMillis }
        var safeMillis = 0L
        var warmMillis = 0L
        var highMillis = 0L
        ordered.zipWithNext().forEach { (sample, next) ->
            val interval = next.timestampMillis - sample.timestampMillis
            if (interval <= 0L || interval > MAX_SAMPLE_GAP_MILLIS) return@forEach
            if (sample.isChargingStatusAvailable && !sample.isCharging) return@forEach
            if (!sample.isTemperatureAvailable || !sample.temperatureCelsius.isFinite()) return@forEach
            when {
                sample.temperatureCelsius >= HIGH_TEMPERATURE_CELSIUS -> highMillis += interval
                sample.temperatureCelsius >= WARM_TEMPERATURE_CELSIUS -> warmMillis += interval
                else -> safeMillis += interval
            }
        }
        return ChargingThermalSummary(
            safeMinutes = (safeMillis / 60_000.0).roundToInt(),
            warmMinutes = (warmMillis / 60_000.0).roundToInt(),
            highMinutes = (highMillis / 60_000.0).roundToInt(),
            phase = classifyPhase(ordered)
        )
    }

    private fun classifyPhase(samples: List<ChargingThermalSample>): ChargingPhaseIndicator {
        val valid = samples.filter {
            (!it.isChargingStatusAvailable || it.isCharging) &&
                it.isCurrentAvailable && it.isVoltageAvailable &&
                it.currentMa != Int.MIN_VALUE && it.voltageMv > 0
        }
        if (valid.size < MIN_PHASE_SAMPLES) return ChargingPhaseIndicator.UNKNOWN
        val split = valid.size / 2
        val first = valid.take(split)
        val last = valid.drop(split)
        val firstCurrent = median(first.map { abs(it.currentMa).toDouble() })
        val lastCurrent = median(last.map { abs(it.currentMa).toDouble() })
        val firstVoltage = median(first.map { it.voltageMv.toDouble() })
        val lastVoltage = median(last.map { it.voltageMv.toDouble() })
        val currentChange = abs(lastCurrent - firstCurrent) / firstCurrent.coerceAtLeast(1.0)
        val voltageChange = abs(lastVoltage - firstVoltage)
        return when {
            lastVoltage - firstVoltage >= CC_MIN_VOLTAGE_RISE_MV &&
                currentChange <= CC_MAX_CURRENT_CHANGE_RATIO -> ChargingPhaseIndicator.CC
            voltageChange <= CV_MAX_VOLTAGE_CHANGE_MV &&
                firstCurrent - lastCurrent >= max(CV_MIN_CURRENT_DROP_MA.toDouble(), firstCurrent * 0.20) ->
                ChargingPhaseIndicator.CV
            else -> ChargingPhaseIndicator.UNKNOWN
        }
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0 else sorted[middle]
    }
}
