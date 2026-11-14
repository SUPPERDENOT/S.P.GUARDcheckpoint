package com.superdenot.powerguard.domain.engine

import kotlin.math.abs

/**
 * BatteryManager.BATTERY_PROPERTY_CURRENT_NOW is specified in microamps.
 * Normalize its magnitude to mA and its direction to app semantics:
 * positive while charging/full, negative while not charging.
 */
object BatteryCurrentNormalizer {
    fun normalize(rawCurrentMicroAmps: Int, isCharging: Boolean): Int {
        if (rawCurrentMicroAmps == 0 || rawCurrentMicroAmps == Int.MIN_VALUE) return 0
        val magnitudeMa = abs(rawCurrentMicroAmps) / MICROAMPS_PER_MILLIAMP
        return if (isCharging) magnitudeMa else -magnitudeMa
    }

    private const val MICROAMPS_PER_MILLIAMP = 1_000
}
