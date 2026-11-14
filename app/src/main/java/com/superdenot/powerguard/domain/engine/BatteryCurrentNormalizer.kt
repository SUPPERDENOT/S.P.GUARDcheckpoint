package com.superdenot.powerguard.domain.engine

import kotlin.math.abs

/**
 * Normalizes vendor-dependent CURRENT_NOW sign conventions into app semantics:
 * positive while charging/full, negative while not charging.
 */
object BatteryCurrentNormalizer {
    fun normalize(rawCurrent: Int, isCharging: Boolean): Int {
        if (rawCurrent == 0 || rawCurrent == Int.MIN_VALUE) return 0
        val magnitudeMa = if (abs(rawCurrent) >= 10_000) abs(rawCurrent) / 1_000 else abs(rawCurrent)
        return if (isCharging) magnitudeMa else -magnitudeMa
    }
}
