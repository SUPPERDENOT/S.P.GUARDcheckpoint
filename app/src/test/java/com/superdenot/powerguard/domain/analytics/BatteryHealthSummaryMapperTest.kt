package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthSummaryMapperTest {
    @Test fun mapsOneStoredSnapshotForOverviewAndInsightsWithoutRecalculation() {
        val snapshot = BatteryHealthEntity(
            timestamp = 1_000L, sessionId = 7L, estimatedCapacityMah = 4_250,
            designCapacityMah = 5_000, healthPercentage = 85f
        )
        val summary = BatteryHealthSummaryMapper.fromStored(snapshot)
        assertEquals(4_250, summary?.estimatedCapacityMah)
        assertEquals(85f, summary?.healthPercentage)
        assertEquals(5_000, summary?.designCapacityMah)
    }

    @Test fun rejectsInvalidStoredSnapshotValues() {
        val snapshot = BatteryHealthEntity(
            timestamp = 1_000L, sessionId = 7L, estimatedCapacityMah = 900,
            designCapacityMah = 5_000, healthPercentage = 85f
        )
        assertNull(BatteryHealthSummaryMapper.fromStored(snapshot))
    }
}
