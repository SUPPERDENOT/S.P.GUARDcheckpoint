package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity

data class BatteryHealthSummary(val estimatedCapacityMah: Int, val healthPercentage: Float, val designCapacityMah: Int)

/** Both Overview and Insights render the same persisted SOH snapshot. */
object BatteryHealthSummaryMapper {
    fun fromStored(snapshot: BatteryHealthEntity?): BatteryHealthSummary? {
        snapshot ?: return null
        if (snapshot.estimatedCapacityMah !in 1_000..20_000) return null
        if (snapshot.designCapacityMah !in 1_000..20_000) return null
        if (!snapshot.healthPercentage.isFinite() || snapshot.healthPercentage !in 10f..110f) return null
        return BatteryHealthSummary(snapshot.estimatedCapacityMah, snapshot.healthPercentage, snapshot.designCapacityMah)
    }
}
