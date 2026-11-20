package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLevelRangeFormatterTest {
    @Test
    fun formatsStartAndEndBatteryLevelsWithExplicitRightArrow() {
        assertEquals("55% → 56%", formatSessionLevelRange(55, 56))
    }
}
