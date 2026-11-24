package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity

data class BatteryHealthSummary(val estimatedCapacityMah: Int, val healthPercentage: Float, val designCapacityMah: Int)

/** Both Overview and Insights render the same persisted SOH snapshot. */
object BatteryHealthSummaryMapper {
    fun fromStored(snapshot: BatteryHealthEntity?): BatteryHealthSummary? {
        snapshot ?: return null
        if (snapshot.estimatedCapacityMah <= 0 || snapshot.designCapacityMah <= 0) return null
        if (!snapshot.healthPercentage.isFinite() || snapshot.healthPercentage <= 0f) return null
        return BatteryHealthSummary(snapshot.estimatedCapacityMah, snapshot.healthPercentage, snapshot.designCapacityMah)
    }
}
