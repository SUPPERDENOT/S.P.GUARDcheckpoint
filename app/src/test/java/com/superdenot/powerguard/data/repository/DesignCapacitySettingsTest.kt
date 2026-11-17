package com.superdenot.powerguard.data.repository

import com.superdenot.powerguard.domain.model.StatusBarParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class DesignCapacitySettingsTest {

    @Test fun freshInstallBatteryMonitoringDefaultsToOff() {
        assertFalse(BatteryMonitoringSettings.resolveEnabled(null, null))
    }

    @Test fun migrationPreservesExplicitLegacyPreference() {
        assertTrue(BatteryMonitoringSettings.resolveEnabled(null, true))
        assertFalse(BatteryMonitoringSettings.resolveEnabled(null, false))
    }

    @Test fun currentMasterPreferenceTakesPrecedenceOverLegacyValue() {
        assertTrue(BatteryMonitoringSettings.resolveEnabled(true, false))
        assertFalse(BatteryMonitoringSettings.resolveEnabled(false, true))
    }

    @Test fun powerConnectedStartRequiresEnabledPreferenceAndBatteryExemption() {
        assertTrue(BatteryMonitoringSettings.canStartFromPowerConnected(true, true))
        assertFalse(BatteryMonitoringSettings.canStartFromPowerConnected(false, true))
        assertFalse(BatteryMonitoringSettings.canStartFromPowerConnected(true, false))
        assertFalse(BatteryMonitoringSettings.canStartFromPowerConnected(false, false))
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
    @Test
    fun statusBarIndicatorDefaultsToDisabledBatteryPercentage() {
        val setting = StatusBarIndicatorSettings.resolveSetting(null, null)

        assertFalse(setting.enabled)
        assertEquals(StatusBarParameter.BATTERY_PERCENTAGE, setting.parameter)
    }

    @Test
    fun statusBarIndicatorResolvesStoredParameterAndEnabledState() {
        val setting = StatusBarIndicatorSettings.resolveSetting(false, "VOLTAGE")

        assertFalse(setting.enabled)
        assertEquals(StatusBarParameter.VOLTAGE, setting.parameter)
    }

    @Test
    fun invalidStoredStatusBarParameterFallsBackToBatteryPercentage() {
        val setting = StatusBarIndicatorSettings.resolveSetting(true, "UNKNOWN")

        assertTrue(setting.enabled)
        assertEquals(StatusBarParameter.BATTERY_PERCENTAGE, setting.parameter)
    }

}
