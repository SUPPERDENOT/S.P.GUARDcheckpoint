package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
data class SessionBatteryHealthEstimate(
    val effectiveCapacityMah: Int,
    val healthPercentage: Float,
    val validSessionCount: Int,
    val designCapacityMah: Int
)

/**
 * Estimates full effective capacity from completed Room session segments.
 * A charger disconnect closes the current session; reconnecting creates a new segment.
 * It is an analytical estimate, not a laboratory battery-capacity measurement.
 */
object BatteryHealthEstimator {
    const val DEFAULT_DESIGN_CAPACITY_MAH = 5_000
    const val MIN_DESIGN_CAPACITY_MAH = 1_000
    const val MAX_DESIGN_CAPACITY_MAH = 20_000
    const val MIN_SOC_DELTA_PERCENT = 15
    const val MIN_SESSION_DURATION_SECONDS = 60L
    const val MIN_CHARGED_MAH = 0.5
    private const val MIN_EFFECTIVE_CAPACITY_MAH = 1_000.0
    private const val MAX_EFFECTIVE_CAPACITY_MAH = 20_000.0

    fun estimate(
        sessions: List<ChargingSessionEntity>,
        designCapacityMah: Int = DEFAULT_DESIGN_CAPACITY_MAH
    ): SessionBatteryHealthEstimate? {
        if (designCapacityMah !in MIN_DESIGN_CAPACITY_MAH..MAX_DESIGN_CAPACITY_MAH) return null

        val capacities = sessions.mapNotNull { session ->
            val endTime = session.endTime ?: return@mapNotNull null
            val endLevel = session.endLevel ?: return@mapNotNull null
            if (session.startTime < 0L || endTime <= session.startTime) return@mapNotNull null
            if ((endTime - session.startTime) / 1_000L < MIN_SESSION_DURATION_SECONDS) return@mapNotNull null
            if (session.startLevel !in 0..100 || endLevel !in 0..100) return@mapNotNull null

            val deltaSoc = endLevel - session.startLevel
            val chargedMah = session.totalMahDelivered
            if (deltaSoc < MIN_SOC_DELTA_PERCENT) return@mapNotNull null
            if (!chargedMah.isFinite() || chargedMah <= MIN_CHARGED_MAH) return@mapNotNull null

            val effectiveCapacity = chargedMah / (deltaSoc / 100.0)
            effectiveCapacity.takeIf {
                it.isFinite() && it in MIN_EFFECTIVE_CAPACITY_MAH..MAX_EFFECTIVE_CAPACITY_MAH
            }
        }.sorted()

        if (capacities.isEmpty()) return null

        // Median limits the influence of one unusually noisy session.
        val middle = capacities.size / 2
        val medianCapacity = if (capacities.size % 2 == 0) {
            (capacities[middle - 1] + capacities[middle]) / 2.0
        } else {
            capacities[middle]
        }
        val health = medianCapacity / designCapacityMah * 100.0
        if (!health.isFinite() || health !in 10.0..110.0) return null

        return SessionBatteryHealthEstimate(
            effectiveCapacityMah = medianCapacity.toInt(),
            healthPercentage = health.toFloat(),
            validSessionCount = capacities.size,
            designCapacityMah = designCapacityMah
        )
    }
}
