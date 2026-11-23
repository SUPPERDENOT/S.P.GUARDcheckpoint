package com.superdenot.powerguard.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CurveSamplePresentationTest {
    @Test
    fun keepsMeasuredZeroDistinctFromUnavailableReadings() {
        assertEquals("0 mA", formatCurveReading(isAvailable = true, value = "0 mA"))
        assertEquals("—", formatCurveReading(isAvailable = false, value = "0 mA"))
    }
}
