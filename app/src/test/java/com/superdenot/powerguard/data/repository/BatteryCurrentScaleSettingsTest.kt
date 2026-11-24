package com.superdenot.powerguard.data.repository

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.model.BatteryCurrentScale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class BatteryCurrentScaleSettingsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before fun clearPreferences() {
        context.getSharedPreferences("powerguard_sensor_calibration", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun freshInstallDefaultsToOneXAndSelectedScalePersists() {
        assertEquals(BatteryCurrentScale.STANDARD, BatteryCurrentScaleSettings.read(context))
        BatteryCurrentScaleSettings.set(context, BatteryCurrentScale.OEM_MILLIAMPS_AS_MICROAMPS)
        assertEquals(BatteryCurrentScale.OEM_MILLIAMPS_AS_MICROAMPS, BatteryCurrentScaleSettings.read(context))
    }

    @Test fun unknownStoredValueDefaultsToOneX() {
        context.getSharedPreferences("powerguard_sensor_calibration", Context.MODE_PRIVATE)
            .edit().putString("battery_current_scale", "unknown").commit()
        assertEquals(BatteryCurrentScale.STANDARD, BatteryCurrentScaleSettings.read(context))
    }

    @Test fun allScaleChoicesHaveExactMultipliers() {
        assertEquals(1.0, BatteryCurrentScaleSettings.resolveStoredValue("STANDARD").multiplier, 0.0)
        assertEquals(1000.0, BatteryCurrentScaleSettings.resolveStoredValue("OEM_MILLIAMPS_AS_MICROAMPS").multiplier, 0.0)
        assertEquals(0.001, BatteryCurrentScaleSettings.resolveStoredValue("OEM_MICROAMPS_AS_MILLIAMPS").multiplier, 0.0)
    }
}
