package com.superdenot.powerguard.ui.analytics

import java.time.LocalDate
import com.superdenot.powerguard.domain.analytics.ChargingPhaseIndicator
import com.superdenot.powerguard.domain.analytics.ChargingThermalSummary
import com.superdenot.powerguard.ui.components.formatThermalDistributionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPresentationFormatterTest {
    @Test
    fun curveHeaderUsesCompactSessionCount() {
        assertEquals("Sesi 3/12", formatCurveSessionCount(3, 12))
    }

    @Test
    fun thermalDistributionIsPresentedAsOneCompactLine() {
        val summary = ChargingThermalSummary(17, 15, 12, ChargingPhaseIndicator.UNKNOWN)
        assertEquals("17m Aman · 15m Hangat · 12m Tinggi", formatThermalDistributionSummary(summary))
    }

    @Test
    fun unavailableThermalDistributionStaysExplicit() {
        val summary = ChargingThermalSummary(0, 0, 0, ChargingPhaseIndicator.UNKNOWN)
        assertEquals("Distribusi suhu tidak tersedia", formatThermalDistributionSummary(summary))
    }

    @Test
    fun selectedDailyRollupMatchesTheDateSelectedInTheChart() {
        assertTrue(isDailyRollupSelected("2026-11-27", LocalDate.parse("2026-11-27")))
        assertFalse(isDailyRollupSelected("2026-11-26", LocalDate.parse("2026-11-27")))
        assertFalse(isDailyRollupSelected("2026-11-27", null))
    }
}
