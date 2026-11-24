package com.superdenot.powerguard.domain.engine
/** Calibration applies only to BatteryManager.CURRENT_NOW before the µA → mA conversion. */
object BatteryCurrentNormalizer {
    fun normalize(rawCurrentMicroAmps: Int, isPowerConnected: Boolean, scaleMultiplier: Double = 1.0): Int {
        if (rawCurrentMicroAmps == 0 || rawCurrentMicroAmps == Int.MIN_VALUE) return 0
        val multiplier = scaleMultiplier.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val scaledMa = rawCurrentMicroAmps.toDouble() * multiplier / 1000.0
        if (!scaledMa.isFinite()) return 0
        val rawMa = scaledMa.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toLong()
        val normalized = if (isPowerConnected) rawMa else -kotlin.math.abs(rawMa)
        return normalized.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }
    fun batterySidePowerWatt(voltageMilliVolts: Int, currentMilliAmps: Int): Float =
        (voltageMilliVolts.toDouble() * currentMilliAmps.toDouble() / 1_000_000.0).toFloat()
}
