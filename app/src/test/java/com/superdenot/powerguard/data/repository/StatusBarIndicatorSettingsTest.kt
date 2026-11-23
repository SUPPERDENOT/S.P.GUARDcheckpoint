package com.superdenot.powerguard.data.repository
import com.superdenot.powerguard.domain.model.StatusBarParameter
import org.junit.Assert.*
import org.junit.Test
class StatusBarIndicatorSettingsTest {
    @Test fun freshInstallDefaultsOffAndToBatterySidePower() {
        val s=StatusBarIndicatorSettings.resolveSetting(null,null);assertFalse(s.enabled);assertEquals(StatusBarParameter.POWER,s.parameter)
    }
    @Test fun explicitSavedMetricAndToggleRemainPreserved() {
        assertTrue(StatusBarIndicatorSettings.resolveSetting(true,"VOLTAGE").enabled);assertFalse(StatusBarIndicatorSettings.resolveSetting(false,"VOLTAGE").enabled)
        assertEquals(StatusBarParameter.VOLTAGE,StatusBarIndicatorSettings.resolveSetting(true,"VOLTAGE").parameter)
    }
    @Test fun legacyChargingStatusAndUnknownMetricSafelyFallbackToPower() {
        assertEquals(StatusBarParameter.POWER,StatusBarIndicatorSettings.resolveSetting(true,"CHARGING_STATUS").parameter)
        assertEquals(StatusBarParameter.POWER,StatusBarIndicatorSettings.resolveSetting(true,"UNKNOWN").parameter)
    }
    @Test fun onlyFiveRequestedMetricsAreSelectable() {
        assertEquals(listOf(StatusBarParameter.POWER,StatusBarParameter.CURRENT,StatusBarParameter.VOLTAGE,StatusBarParameter.TEMPERATURE,StatusBarParameter.BATTERY_PERCENTAGE),StatusBarParameter.selectableValues)
    }
}
