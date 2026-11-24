package com.superdenot.powerguard.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthSummaryMapperTest {
    @Test fun mapsOneStoredSnapshotForOverviewAndInsightsWithoutRecalculation() {
        val summary = BatteryHealthSummaryMapper.map(
            estimatedCapacityMah = 4_250,
            healthPercentage = 85f,
            designCapacityMah = 5_000
        )
        assertEquals(4_250, summary?.estimatedCapacityMah)
        assertEquals(85f, summary?.healthPercentage)
        assertEquals(5_000, summary?.designCapacityMah)
    }

    @Test fun rejectsInvalidStoredSnapshotValues() {
        assertNull(BatteryHealthSummaryMapper.map(0, 85f, 5_000))
        assertNull(BatteryHealthSummaryMapper.map(4_250, Float.NaN, 5_000))
    }
}
