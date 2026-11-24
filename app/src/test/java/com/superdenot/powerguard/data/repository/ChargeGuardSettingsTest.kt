package com.superdenot.powerguard.data.repository

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.engine.ChargeGuardConfiguration
import com.superdenot.powerguard.data.repository.ChargeGuardAlertSoundSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class ChargeGuardSettingsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before fun clearChargeGuardPreferences() {
        context.getSharedPreferences("powerguard_charge_guard", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun freshInstallDefaultsBothChargeGuardTogglesOff() {
        val config = ChargeGuardSettings.read(context)
        assertFalse(config.chargeLimitEnabled)
        assertFalse(config.highTemperatureEnabled)
        assertEquals(85, config.targetLevelPercent)
        assertEquals(40, config.highTemperatureThresholdCelsius)
    }

    @Test fun alertSoundDefaultsToSystemNotificationToneWithVibrationEnabled() {
        val setting = ChargeGuardSettings.readAlertSound(context)
        assertEquals(ChargeGuardAlertSoundSetting.DEFAULT, setting.selection)
        assertEquals("Nada Default", setting.title)
        assertTrue(setting.vibrate)
    }

    @Test fun alertSoundUriTitleAndVibrationPersistIndependently() {
        val expected = ChargeGuardAlertSoundSetting(
            selection = "content://media/external/audio/media/123",
            title = "Nada pilihan",
            vibrate = false
        )
        ChargeGuardSettings.saveAlertSound(context, expected)
        assertEquals(expected, ChargeGuardSettings.readAlertSound(context))
        assertFalse(
            context.getSharedPreferences("powerguard_sensor_calibration", Context.MODE_PRIVATE)
                .contains("alert_sound_selection")
        )
    }

    @Test fun independentChargeGuardSettingsPersistInTheirOwnPreferenceFile() {
        val expected = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 90,
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 42
        )
        ChargeGuardSettings.save(context, expected)
        assertEquals(expected, ChargeGuardSettings.read(context))
        assertFalse(
            context.getSharedPreferences("powerguard_sensor_calibration", Context.MODE_PRIVATE)
                .contains("charge_limit_enabled")
        )
    }
}
