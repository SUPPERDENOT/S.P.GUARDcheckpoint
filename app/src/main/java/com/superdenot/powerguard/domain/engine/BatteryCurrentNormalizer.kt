package com.superdenot.powerguard.domain.engine

/**
 * Preserve battery-side current direction while external power is connected.
 * A negative reading then represents net battery drain, not a guessed protocol.
 * When unplugged, normalize the direction to discharge regardless of OEM raw sign.
 */
object BatteryCurrentNormalizer {
    fun normalize(rawCurrentMicroAmps: Int, isPowerConnected: Boolean): Int {
        if (rawCurrentMicroAmps == 0 || rawCurrentMicroAmps == Int.MIN_VALUE) return 0
        val rawMilliAmps = rawCurrentMicroAmps.toLong() / MICROAMPS_PER_MILLIAMP
        val normalized = if (isPowerConnected) rawMilliAmps else -kotlin.math.abs(rawMilliAmps)
        return normalized.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    private const val MICROAMPS_PER_MILLIAMP = 1_000L
}
