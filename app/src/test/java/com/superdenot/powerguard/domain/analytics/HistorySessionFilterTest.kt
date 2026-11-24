package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySessionFilterTest {
    private fun session(id: Long, durationMs: Long = 300_000L, start: Int = 20, end: Int = 25,
        mah: Double = 100.0, anomaly: Boolean = false, label: String? = null) = ChargingSessionEntity(
        id = id, startTime = 1_000L, endTime = 1_000L + durationMs, startLevel = start, endLevel = end,
        totalMahDelivered = mah, chargerType = "USB Port", isAnomaly = anomaly,
        anomalyReason = if (anomaly) "SLOW_CHARGING" else null, accessoryLabel = label
    )

    @Test fun microFilterIsDefaultAndHidesShortSessions() {
        val short = session(1, durationMs = 59_000L)
        assertFalse(HistorySessionFilterMatcher.matches(short, HistorySessionFilter.MICRO, 25, 61_000L))
        assertTrue(HistorySessionFilterMatcher.matches(short, HistorySessionFilter.ALL, 25, 61_000L))
    }
    @Test fun anomalyFilterUsesDomainEvaluator() {
        assertTrue(HistorySessionFilterMatcher.matches(session(2, anomaly = true), HistorySessionFilter.ANOMALY, 25, 301_000L))
        assertFalse(HistorySessionFilterMatcher.matches(session(3), HistorySessionFilter.ANOMALY, 25, 301_000L))
    }
    @Test fun labeledFilterRequiresNonBlankLabel() {
        assertTrue(HistorySessionFilterMatcher.matches(session(4, label = "USB-C"), HistorySessionFilter.LABELED, 25, 301_000L))
        assertFalse(HistorySessionFilterMatcher.matches(session(5, label = "  "), HistorySessionFilter.LABELED, 25, 301_000L))
    }
    @Test fun gainFilterRequiresTenPercentagePoints() {
        assertTrue(HistorySessionFilterMatcher.matches(session(6, start = 20, end = 30), HistorySessionFilter.GAIN_10_PERCENT, 30, 301_000L))
        assertFalse(HistorySessionFilterMatcher.matches(session(7, start = 20, end = 29), HistorySessionFilter.GAIN_10_PERCENT, 29, 301_000L))
    }
    @Test fun filterPreservesInputOrder() {
        val values = listOf(session(8, anomaly = true), session(9), session(10, anomaly = true))
        assertEquals(listOf(8L, 10L), HistorySessionFilterMatcher.filter(values, HistorySessionFilter.ANOMALY, 25, 301_000L).map { it.id })
    }
}