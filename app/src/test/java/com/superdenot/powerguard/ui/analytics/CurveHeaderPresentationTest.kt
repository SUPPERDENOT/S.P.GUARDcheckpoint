package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class CurveHeaderPresentationTest {
    @Test
    fun formatsVisibleAndTotalSessionCountsForCompactCurveHeader() {
        assertEquals("Sesi 25/54", formatCurveSessionCount(25, 54))
        assertEquals("Sesi 0/0", formatCurveSessionCount(0, 0))
    }
}
