package com.superdenot.powerguard.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentChartScaleTest {
    @Test fun ignoresExtremeSinglePointWhenCalculatingDisplayAxis() {
        val raw = listOf(420, 680, 910, 31_500)
        val scale = CurrentChartScaleCalculator.calculate(raw)
        assertEquals(0f, scale.minimumMilliAmps)
        assertEquals(910f, scale.maximumMilliAmps)
        assertEquals(31_500, raw.last())
    }

    @Test fun keepsNegativeBatteryCurrentAndUsesSafeFallbackWhenAllPointsAreOutliers() {
        val normal = CurrentChartScaleCalculator.calculate(listOf(-1_200, -800, 500))
        assertEquals(-1_200f, normal.minimumMilliAmps)
        assertEquals(500f, normal.maximumMilliAmps)
        val fallback = CurrentChartScaleCalculator.calculate(listOf(30_000, -30_000))
        assertEquals(0f, fallback.minimumMilliAmps)
        assertEquals(500f, fallback.maximumMilliAmps)
    }
}
