package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDurationFormatterTest {
    @Test fun formatsHoursAndMinutesCompactlyOnOneLine() {
        assertEquals("3j 30m", formatDurationIndo(12_600L))
    }

    @Test fun formatsMinutesAndSecondsCompactlyOnOneLine() {
        assertEquals("48m 21d", formatDurationIndo(2_901L))
    }

    @Test fun formatsShortDurationsInSeconds() {
        assertEquals("21d", formatDurationIndo(21L))
    }
}
