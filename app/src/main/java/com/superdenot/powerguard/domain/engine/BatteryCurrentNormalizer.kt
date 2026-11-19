package com.superdenot.powerguard.domain.engine

import kotlin.math.abs

/** Normalize directional battery current only when Android exposes charging status. */
object BatteryCurrentNormalizer {
    fun normalize(
        rawCurrentMicroAmps: Int,
        isCharging: Boolean,
        isChargingStatusAvailable: Boolean = true
    ): Int {
        if (!isChargingStatusAvailable || rawCurrentMicroAmps == 0 ||
            rawCurrentMicroAmps == Int.MIN_VALUE
        ) return 0
        val magnitudeMa = abs(rawCurrentMicroAmps) / MICROAMPS_PER_MILLIAMP
        return if (isCharging) magnitudeMa else -magnitudeMa
    }

    private const val MICROAMPS_PER_MILLIAMP = 1_000
}
