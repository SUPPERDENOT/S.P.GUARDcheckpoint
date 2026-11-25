package com.superdenot.powerguard.domain.engine

import kotlin.math.ceil

/** Resolves one ETA for Overview, Charging, and the foreground notification. */
internal object ChargingEtaSanityCheck {
    fun resolveMinutes(
        osEtaMillis: Long?,
        level: Int,
        designCapacityMah: Int,
        activeCurrentMa: Int,
        isCharging: Boolean,
        estimatorFallbackMinutes: Long?
    ): Long? {
        if (!isCharging || level !in 0..99) return null
        val validOsEta = osEtaMillis?.takeIf { it > 0L }
        val implausiblyLow = validOsEta != null &&
            validOsEta < MIN_OS_ETA_MILLIS && level < OS_LOW_ETA_LEVEL_LIMIT
        if (validOsEta != null && !implausiblyLow) {
            return ceil(validOsEta / MILLIS_PER_MINUTE).toLong().coerceAtLeast(1L)
        }
        if (implausiblyLow) {
            estimateFromRemainingCapacity(level, designCapacityMah, activeCurrentMa)?.let { return it }
        }
        return estimatorFallbackMinutes?.takeIf { it > 0L }
    }

    private fun estimateFromRemainingCapacity(level: Int, designCapacityMah: Int, activeCurrentMa: Int): Long? {
        if (designCapacityMah !in ChargingEtaEstimator.MIN_CAPACITY_MAH..ChargingEtaEstimator.MAX_CAPACITY_MAH) return null
        if (activeCurrentMa <= ChargingEtaEstimator.MINIMUM_ETA_CURRENT_MA) return null
        val remainingMah = (100 - level) / 100.0 * designCapacityMah
        if (!remainingMah.isFinite() || remainingMah <= 0.0) return null
        return ceil(remainingMah * 60.0 / activeCurrentMa).toLong().coerceAtLeast(1L)
    }

    private const val MILLIS_PER_MINUTE = 60_000.0
    const val MIN_OS_ETA_MILLIS = 600_000L
    const val OS_LOW_ETA_LEVEL_LIMIT = 90
}
