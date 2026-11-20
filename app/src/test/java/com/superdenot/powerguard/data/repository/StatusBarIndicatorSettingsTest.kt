package com.superdenot.powerguard.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarIndicatorSettingsTest {

    @Test
    fun freshInstallDefaultsStatusBarIndicatorToOff() {
        assertFalse(StatusBarIndicatorSettings.resolveSetting(null, null).enabled)
    }

    @Test
    fun explicitExistingPreferenceIsPreserved() {
        assertTrue(StatusBarIndicatorSettings.resolveSetting(true, null).enabled)
        assertFalse(StatusBarIndicatorSettings.resolveSetting(false, null).enabled)
    }
}
