package com.superdenot.powerguard.ui.analytics

import java.time.LocalDate
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
    fun selectedDailyRollupMatchesTheDateSelectedInTheChart() {
        assertTrue(isDailyRollupSelected("2026-11-27", LocalDate.parse("2026-11-27")))
        assertFalse(isDailyRollupSelected("2026-11-26", LocalDate.parse("2026-11-27")))
        assertFalse(isDailyRollupSelected("2026-11-27", null))
    }
}
