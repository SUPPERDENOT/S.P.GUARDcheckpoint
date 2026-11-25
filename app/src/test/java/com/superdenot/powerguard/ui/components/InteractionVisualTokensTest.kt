package com.superdenot.powerguard.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionVisualTokensTest {
    @Test
    fun powerButtonMotionUsesApprovedScaleAndGlowTiming() {
        assertEquals(0.95f, POWER_BUTTON_PRESSED_SCALE, 0f)
        assertEquals(0.20f, POWER_BUTTON_GLOW_PEAK_ALPHA, 0f)
        assertEquals(150, POWER_BUTTON_GLOW_PRESS_DURATION_MS)
        assertEquals(180, POWER_BUTTON_GLOW_RELEASE_DURATION_MS)
        assertEquals(90L, POWER_BUTTON_CLOSE_DELAY_MS)
    }

    @Test
    fun gaugePercentageOutlineUsesThinStroke() {
        assertEquals(1.8f, GAUGE_PERCENTAGE_OUTLINE_WIDTH_DP, 0f)
    }
}
