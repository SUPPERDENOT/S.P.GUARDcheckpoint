package com.superdenot.powerguard.data.repository

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.engine.ChargeGuardConfiguration
import com.superdenot.powerguard.domain.engine.ChargeGuardRepeatMode
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

    @Test fun fiftyDegreeTemperatureThresholdPersistsThroughChargeGuardSettings() {
        val expected = ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 50)
        ChargeGuardSettings.save(context, expected)
        assertEquals(50, ChargeGuardSettings.read(context).highTemperatureThresholdCelsius)
    }

    @Test fun alertSoundDefaultsToSystemNotificationToneWithVibrationEnabled() {
        val setting = ChargeGuardSettings.readAlertSound(context)
        assertEquals(ChargeGuardAlertSoundSetting.DEFAULT, setting.selection)
        assertEquals("Nada Default", setting.title)
        assertEquals(ChargeGuardVibrationPattern.DOUBLE, setting.vibrationPattern)
        assertEquals(listOf(0L, 200L, 120L, 200L), setting.vibrationTimings().toList())
    }

    @Test fun alertSoundUriTitleAndVibrationPersistIndependently() {
        val expected = ChargeGuardAlertSoundSetting(
            selection = "content://media/external/audio/media/123",
            title = "Nada pilihan",
            vibrationPattern = ChargeGuardVibrationPattern.SOS,
            customVibrationTimings = listOf(120L, 90L, 320L)
        )
        ChargeGuardSettings.saveAlertSound(context, expected)
        assertEquals(expected, ChargeGuardSettings.readAlertSound(context))
        assertFalse(
            context.getSharedPreferences("powerguard_sensor_calibration", Context.MODE_PRIVATE)
                .contains("alert_sound_selection")
        )
    }


    @Test fun allBuiltInVibrationPatternsPersistTheirSelection() {
        ChargeGuardVibrationPattern.values().forEach { pattern ->
            val expected = ChargeGuardAlertSoundSetting(vibrationPattern = pattern)
            ChargeGuardSettings.saveAlertSound(context, expected)
            assertEquals(pattern, ChargeGuardSettings.readAlertSound(context).vibrationPattern)
        }
    }

    @Test fun allBuiltInVibrationPatternsResolveToExpectedWaveforms() {
        org.junit.Assert.assertArrayEquals(
            longArrayOf(), ChargeGuardAlertSoundSetting(vibrationPattern = ChargeGuardVibrationPattern.OFF).vibrationTimings()
        )
        org.junit.Assert.assertArrayEquals(
            longArrayOf(0L, 250L), ChargeGuardAlertSoundSetting(vibrationPattern = ChargeGuardVibrationPattern.SHORT).vibrationTimings()
        )
        org.junit.Assert.assertArrayEquals(
            longArrayOf(0L, 200L, 120L, 200L), ChargeGuardAlertSoundSetting(vibrationPattern = ChargeGuardVibrationPattern.DOUBLE).vibrationTimings()
        )
        org.junit.Assert.assertArrayEquals(
            longArrayOf(0L, 120L, 90L, 120L, 90L, 120L, 180L, 320L, 90L, 320L, 90L, 320L, 180L, 120L, 90L, 120L, 90L, 120L),
            ChargeGuardAlertSoundSetting(vibrationPattern = ChargeGuardVibrationPattern.SOS).vibrationTimings()
        )
    }

    @Test fun customVibrationSegmentsAreClampedAndLimitedToEightPulses() {
        val normalized = ChargeGuardAlertSoundSetting(
            vibrationPattern = ChargeGuardVibrationPattern.CUSTOM,
            customVibrationTimings = listOf(1L, 2_000L, 90L, 2_000L, 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1_000L, 1_100L, 1_200L, 1_300L, 1_400L)
        ).normalized()
        assertEquals(15, normalized.customVibrationTimings.size)
        assertEquals(50L, normalized.customVibrationTimings.first())
        assertEquals(1_500L, normalized.customVibrationTimings[1])
        assertEquals(1_100L, normalized.customVibrationTimings.last())
        assertEquals(16, normalized.vibrationTimings().size)
    }

    @Test fun customVibrationTimingsPersist() {
        val expected = ChargeGuardAlertSoundSetting(
            vibrationPattern = ChargeGuardVibrationPattern.CUSTOM,
            customVibrationTimings = listOf(120L, 180L, 240L)
        )
        ChargeGuardSettings.saveAlertSound(context, expected)
        assertEquals(expected, ChargeGuardSettings.readAlertSound(context))
    }


    @Test fun repeatModeDefaultsToOnceAndPersistsAllChoices() {
        assertEquals(ChargeGuardRepeatMode.ONCE, ChargeGuardSettings.read(context).repeatMode)
        ChargeGuardRepeatMode.values().forEach { mode ->
            val config = ChargeGuardConfiguration(repeatMode = mode)
            ChargeGuardSettings.save(context, config)
            assertEquals(mode, ChargeGuardSettings.read(context).repeatMode)
        }
    }

    @Test fun dismissingChargeAlertPersistsItsRepeatStopFlag() {
        ChargeGuardSettings.dismissRepeats(context, com.superdenot.powerguard.domain.engine.ChargeGuardAlertType.CHARGE_LIMIT_REACHED)
        val prefs = context.getSharedPreferences("powerguard_charge_guard", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("charge_repeat_dismissed", false))
    }

    @Test fun dismissingTemperatureAlertPersistsItsRepeatStopFlag() {
        ChargeGuardSettings.dismissRepeats(context, com.superdenot.powerguard.domain.engine.ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED)
        val prefs = context.getSharedPreferences("powerguard_charge_guard", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("temperature_repeat_dismissed", false))
    }


    @Test fun unplugResetClearsBothPersistedRepeatCounters() {
        val prefs = context.getSharedPreferences("powerguard_charge_guard", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("charge_repeat_session_id", 77L)
            .putInt("charge_repeat_target", 80)
            .putInt("charge_repeat_count", 3)
            .putLong("charge_repeat_last_at", 90_000L)
            .putInt("temperature_repeat_count", 2)
            .putLong("temperature_repeat_last_at", 91_000L)
            .commit()
        ChargeGuardSettings.stopRepeats(context)
        assertEquals(Long.MIN_VALUE, prefs.getLong("charge_repeat_session_id", Long.MIN_VALUE))
        assertEquals(0, prefs.getInt("charge_repeat_count", -1))
        assertEquals(0L, prefs.getLong("charge_repeat_last_at", -1L))
        assertEquals(0, prefs.getInt("temperature_repeat_count", -1))
        assertEquals(0L, prefs.getLong("temperature_repeat_last_at", -1L))
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
