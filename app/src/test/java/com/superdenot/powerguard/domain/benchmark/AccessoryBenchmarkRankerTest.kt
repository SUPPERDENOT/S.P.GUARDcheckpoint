package com.superdenot.powerguard.domain.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryBenchmarkRankerTest {
    @Test fun ranksFastestNormalizedRateAndAveragePeakTemperature() {
        val ranking = AccessoryBenchmarkRanker.rank(listOf(
            group("Adaptor A", 42.0, 38.5),
            group("Adaptor B", 65.0, 41.2),
            group("Adaptor C", null, null)
        ))
        assertEquals("Adaptor B", ranking.fastestLabel)
        assertTrue("Adaptor B" in ranking.warmLabels)
        assertTrue("Adaptor A" !in ranking.warmLabels)
    }

    @Test fun unavailableMetricsDoNotCreateBadges() {
        val ranking = AccessoryBenchmarkRanker.rank(listOf(group("Unknown", null, null)))
        assertNull(ranking.fastestLabel)
        assertTrue(ranking.warmLabels.isEmpty())
    }

    private fun group(label: String, rate: Double?, peak: Double?) = AccessoryBenchmarkGroup(
        label, 1, null, null, rate, null, null, null, peak
    )
}
