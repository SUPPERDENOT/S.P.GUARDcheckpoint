package com.superdenot.powerguard.domain.engine

/**
 * Estimasi kapasitas penuh berbasis energi/kapasitas yang tercatat selama satu sesi charging.
 * Ini bukan pengukuran laboratorium; input yang tidak cukup/masuk akal menghasilkan null.
 */
data class BatteryHealthEstimate(
    val estimatedCapacityMah: Int,
    val healthPercentage: Float
)

object BatteryHealthEstimator {
    const val MIN_DESIGN_CAPACITY_MAH = 1_000
    const val MAX_DESIGN_CAPACITY_MAH = 20_000
    const val MIN_ESTIMATED_CAPACITY_MAH = 1_000
    const val MAX_ESTIMATED_CAPACITY_MAH = 20_000
    const val MIN_CHARGE_DELTA_PERCENT = 40
    const val MIN_DELIVERED_MAH = 300.0

    fun estimate(
        totalMahDelivered: Double,
        startLevel: Int,
        endLevel: Int,
        designCapacityMah: Int
    ): BatteryHealthEstimate? {
        if (!totalMahDelivered.isFinite() || totalMahDelivered <= MIN_DELIVERED_MAH) return null
        if (startLevel !in 0..100 || endLevel !in 0..100) return null
        val deltaPercent = endLevel - startLevel
        if (deltaPercent < MIN_CHARGE_DELTA_PERCENT) return null
        if (designCapacityMah !in MIN_DESIGN_CAPACITY_MAH..MAX_DESIGN_CAPACITY_MAH) return null

        val estimatedFullMah = ((totalMahDelivered / deltaPercent.toDouble()) * 100.0)
        if (!estimatedFullMah.isFinite() ||
            estimatedFullMah < MIN_ESTIMATED_CAPACITY_MAH ||
            estimatedFullMah > MAX_ESTIMATED_CAPACITY_MAH
        ) return null

        val estimatedCapacityMah = estimatedFullMah.toInt()
        val healthPercentage =
            (estimatedCapacityMah.toFloat() / designCapacityMah.toFloat()) * 100f
        if (!healthPercentage.isFinite() || healthPercentage !in 10f..110f) return null

        return BatteryHealthEstimate(
            estimatedCapacityMah = estimatedCapacityMah,
            healthPercentage = healthPercentage
        )
    }

    fun isValidStoredSnapshot(
        estimatedCapacityMah: Int,
        designCapacityMah: Int,
        healthPercentage: Float
    ): Boolean {
        if (estimatedCapacityMah !in MIN_ESTIMATED_CAPACITY_MAH..MAX_ESTIMATED_CAPACITY_MAH) return false
        if (designCapacityMah !in MIN_DESIGN_CAPACITY_MAH..MAX_DESIGN_CAPACITY_MAH) return false
        if (!healthPercentage.isFinite() || healthPercentage !in 10f..110f) return false
        val expected = (estimatedCapacityMah.toFloat() / designCapacityMah.toFloat()) * 100f
        return expected.isFinite() && kotlin.math.abs(expected - healthPercentage) < 0.2f
    }
}
