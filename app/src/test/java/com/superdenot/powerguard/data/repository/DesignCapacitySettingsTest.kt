package com.superdenot.powerguard.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class DesignCapacitySettingsTest {

    @Test fun autoMonitoringDefaultsToEnabledForExistingInstallations() {
        assertTrue(AutoMonitoringSettings.resolveEnabled(null))
    }

    @Test fun autoMonitoringPreferenceCanBeEnabledOrDisabled() {
        assertTrue(AutoMonitoringSettings.resolveEnabled(true))
        assertFalse(AutoMonitoringSettings.resolveEnabled(false))
    }

    @Test fun powerConnectedAutoStartRequiresEnabledPreferenceAndBatteryExemption() {
        assertTrue(AutoMonitoringSettings.canStartFromPowerConnected(true, true))
        assertFalse(AutoMonitoringSettings.canStartFromPowerConnected(false, true))
        assertFalse(AutoMonitoringSettings.canStartFromPowerConnected(true, false))
        assertFalse(AutoMonitoringSettings.canStartFromPowerConnected(false, false))
    }

    @Test
    fun manualOverrideHasPriorityOverAutomaticCapacity() {
        val result = DesignCapacitySettings.resolveSetting(4800, 5000)

        assertEquals(4800, result.capacityMah ?: -1)
        assertTrue(result.isManualOverride)
    }

    @Test
    fun automaticCapacityIsUsedWhenManualOverrideIsAbsent() {
        val result = DesignCapacitySettings.resolveSetting(null, 5000)

        assertEquals(5000, result.capacityMah ?: -1)
        assertFalse(result.isManualOverride)
    }

    @Test
    fun unavailableAutomaticCapacityRemainsUnavailable() {
        val result = DesignCapacitySettings.resolveSetting(null, 0)

        assertNull(result.capacityMah)
        assertFalse(result.isManualOverride)
    }

    @Test
    fun invalidManualValueDoesNotOverrideValidAutomaticCapacity() {
        val result = DesignCapacitySettings.resolveSetting(999, 5000)

        assertEquals(5000, result.capacityMah)
        assertFalse(result.isManualOverride)
    }

    @Test
    fun invalidAutomaticValueIsNotPresentedAsDetectedCapacity() {
        val result = DesignCapacitySettings.resolveSetting(null, 20000)

        assertEquals(null, result.capacityMah)
        assertFalse(result.isManualOverride)
    }
}
