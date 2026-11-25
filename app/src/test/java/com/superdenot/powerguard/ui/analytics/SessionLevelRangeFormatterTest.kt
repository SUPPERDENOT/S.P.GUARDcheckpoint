package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SessionLevelRangeFormatterTest {
    @Test
    fun formatsStartAndEndBatteryLevelsWithSingleLineArrow() {
        assertEquals("38% → 38%", formatSessionLevelRange(38, 38))
        assertEquals("55% → 56%", formatSessionLevelRange(55, 56))
        assertEquals("44% → 54%", formatSessionLevelRange(44, 54))
        assertEquals("44% → 62%", formatSessionLevelRange(44, 62))
        assertEquals("69% → 76%", formatSessionLevelRange(69, 76))
        assertEquals("44% → —", formatSessionLevelRange(44, null))
        assertEquals(1, formatSessionLevelRange(44, 54).count { it == '→' })
        assertFalse(formatSessionLevelRange(44, 54).contains("'n"))
        assertFalse(formatSessionLevelRange(44, 54).contains("‘n"))
        assertFalse(formatSessionLevelRange(44, 54).contains("’n"))
    }
}
