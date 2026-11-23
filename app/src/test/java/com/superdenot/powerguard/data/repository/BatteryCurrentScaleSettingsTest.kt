package com.superdenot.powerguard.data.repository
import com.superdenot.powerguard.domain.model.BatteryCurrentScale
import org.junit.Assert.assertEquals
import org.junit.Test
class BatteryCurrentScaleSettingsTest {
    @Test fun missingAndUnknownValueDefaultToOneX() {
        assertEquals(BatteryCurrentScale.STANDARD,BatteryCurrentScaleSettings.resolveStoredValue(null));assertEquals(BatteryCurrentScale.STANDARD,BatteryCurrentScaleSettings.resolveStoredValue("unknown"))
        assertEquals(1.0,BatteryCurrentScaleSettings.resolveStoredValue(null).multiplier,0.0)
    }
    @Test fun supportedValuesMapToExactMultipliers() {
        assertEquals(1.0,BatteryCurrentScaleSettings.resolveStoredValue("STANDARD").multiplier,0.0)
        assertEquals(1000.0,BatteryCurrentScaleSettings.resolveStoredValue("OEM_MILLIAMPS_AS_MICROAMPS").multiplier,0.0)
        assertEquals(0.001,BatteryCurrentScaleSettings.resolveStoredValue("OEM_MICROAMPS_AS_MILLIAMPS").multiplier,0.0)
    }
}
