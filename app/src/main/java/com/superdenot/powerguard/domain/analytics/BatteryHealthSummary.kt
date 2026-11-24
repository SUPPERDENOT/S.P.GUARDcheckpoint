package com.superdenot.powerguard.domain.analytics

data class BatteryHealthSummary(val estimatedCapacityMah: Int, val healthPercentage: Float, val designCapacityMah: Int)

/** Shared pure mapping so Overview and Insights render one identical stored SOH snapshot. */
object BatteryHealthSummaryMapper {
    fun map(
        estimatedCapacityMah: Int,
        healthPercentage: Float,
        designCapacityMah: Int
    ): BatteryHealthSummary? {
        if (estimatedCapacityMah <= 0 || designCapacityMah <= 0) return null
        if (!healthPercentage.isFinite() || healthPercentage <= 0f) return null
        return BatteryHealthSummary(estimatedCapacityMah, healthPercentage, designCapacityMah)
    }
}
