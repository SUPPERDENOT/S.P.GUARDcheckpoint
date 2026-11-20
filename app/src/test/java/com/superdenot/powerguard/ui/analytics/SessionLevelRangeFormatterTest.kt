package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLevelRangeFormatterTest {
    @Test
    fun formatsStartAndEndBatteryLevelsWithFontSafeAsciiArrow() {
        assertEquals("38% -> 38%", formatSessionLevelRange(38, 38))
        assertEquals("55% -> 56%", formatSessionLevelRange(55, 56))
    }
}
