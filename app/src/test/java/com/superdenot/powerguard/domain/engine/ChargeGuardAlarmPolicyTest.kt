package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeGuardAlarmPolicyTest {
    @Test fun chargeLimitNotifiesOncePerSessionAndTarget() {
        val config = ChargeGuardConfiguration(chargeLimitEnabled = true, targetLevelPercent = 85)
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 10L, true, 85, true, 35f, true, 2_000_000L)
        assertEquals(ChargeGuardAlertType.CHARGE_LIMIT_REACHED, first.alerts.single().type)
        val repeated = ChargeGuardAlarmPolicy.evaluate(config, first.state, 10L, true, 92, true, 35f, true, 2_001_000L)
        assertTrue(repeated.alerts.isEmpty())
        val nextSession = ChargeGuardAlarmPolicy.evaluate(config, repeated.state, 11L, true, 86, true, 35f, true, 2_002_000L)
        assertEquals(1, nextSession.alerts.size)
    }

    @Test fun chargeLimitRequiresChargingAndAvailableLevel() {
        val config = ChargeGuardConfiguration(chargeLimitEnabled = true, targetLevelPercent = 80)
        val unplugged = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 1L, false, 90, true, 30f, true, 2_000_000L)
        val unavailable = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 1L, true, 90, false, 30f, true, 2_000_000L)
        assertTrue(unplugged.alerts.isEmpty())
        assertTrue(unavailable.alerts.isEmpty())
    }

    @Test fun highTemperatureNotifiesOnceUntilTemperatureReturnsBelowHysteresis() {
        val config = ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 40)
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 1L, true, 70, true, 40f, true, 2_000_000L)
        assertEquals(ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED, first.alerts.single().type)
        val sustained = ChargeGuardAlarmPolicy.evaluate(config, first.state, 1L, true, 71, true, 44f, true, 2_100_000L)
        assertTrue(sustained.alerts.isEmpty())
        assertFalse(sustained.state.temperatureArmed)
        val cooled = ChargeGuardAlarmPolicy.evaluate(config, sustained.state, 1L, true, 72, true, 37f, true, 2_200_000L)
        assertTrue(cooled.state.temperatureArmed)
    }

    @Test fun fiftyDegreeThresholdAlertsAtFiftyAndOnlyRearmsBelowFortyEight() {
        assertTrue(50 in ChargeGuardConfiguration.SUPPORTED_TEMPERATURE_THRESHOLDS)
        val config = ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 50)
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 1L, true, 70, true, 50f, true, 3_000_000L)
        assertEquals(ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED, first.alerts.single().type)
        assertFalse(first.state.temperatureArmed)
        val atFortyEight = ChargeGuardAlarmPolicy.evaluate(config, first.state, 1L, true, 70, true, 48f, true, 3_100_000L)
        assertFalse(atFortyEight.state.temperatureArmed)
        val belowFortyEight = ChargeGuardAlarmPolicy.evaluate(config, atFortyEight.state, 1L, true, 70, true, 47.9f, true, 3_200_000L)
        assertTrue(belowFortyEight.state.temperatureArmed)
    }

    @Test fun temperatureCooldownPreventsRepeatedEventsFromSpamming() {
        val config = ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 42)
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 1L, true, 70, true, 43f, true, 2_000_000L)
        val cooled = ChargeGuardAlarmPolicy.evaluate(config, first.state, 1L, true, 70, true, 39f, true, 2_100_000L)
        val stillCoolingDown = ChargeGuardAlarmPolicy.evaluate(config, cooled.state, 1L, true, 70, true, 43f, true, 2_200_000L)
        assertTrue(stillCoolingDown.alerts.isEmpty())
        val afterCooldown = ChargeGuardAlarmPolicy.evaluate(
            config, stillCoolingDown.state, 1L, true, 70, true, 43f, true,
            2_000_000L + ChargeGuardAlarmPolicy.TEMPERATURE_COOLDOWN_MS
        )
        assertEquals(1, afterCooldown.alerts.size)
    }

    @Test fun chargeAndTemperatureAlertsCanTriggerInTheSameSample() {
        val config = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 85,
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 40
        )
        val result = ChargeGuardAlarmPolicy.evaluate(
            config, ChargeGuardAlarmState(), 5L, true, 90, true, 42f, true, 2_000_000L
        )
        assertEquals(2, result.alerts.size)
        assertTrue(result.alerts.any { it.type == ChargeGuardAlertType.CHARGE_LIMIT_REACHED })
        assertTrue(result.alerts.any { it.type == ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED })
    }


    @Test fun onceModeNeverRepeatsAnActiveChargeIncident() {
        val config = ChargeGuardConfiguration(chargeLimitEnabled = true, targetLevelPercent = 80)
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 7L, true, 85, true, 35f, true, 1_000L)
        val later = ChargeGuardAlarmPolicy.evaluate(config, first.state, 7L, true, 90, true, 35f, true, 1_000L + 600_000L)
        assertEquals(1, first.alerts.size)
        assertTrue(later.alerts.isEmpty())
    }

    @Test fun oneMinuteRepeatWaitsForFullIntervalAndStopsAfterFiveRepeats() {
        val config = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 80,
            repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
        )
        var result = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 8L, true, 85, true, 35f, true, 10_000L)
        assertEquals(1, result.alerts.size)
        val early = ChargeGuardAlarmPolicy.evaluate(config, result.state, 8L, true, 85, true, 35f, true, 69_999L)
        assertTrue(early.alerts.isEmpty())
        result = early
        repeat(ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT) { index ->
            result = ChargeGuardAlarmPolicy.evaluate(
                config, result.state, 8L, true, 85, true, 35f, true,
                70_000L + index * ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
            )
            assertEquals("repeat ${index + 1}", 1, result.alerts.size)
        }
        val capped = ChargeGuardAlarmPolicy.evaluate(
            config, result.state, 8L, true, 85, true, 35f, true,
            result.state.chargeRepeatLastAtEpochMs + ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
        )
        assertTrue(capped.alerts.isEmpty())
        assertEquals(ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT, capped.state.chargeRepeatCount)
    }

    @Test fun threeMinuteRepeatUsesThreeMinuteInterval() {
        val config = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 85,
            repeatMode = ChargeGuardRepeatMode.EVERY_3_MINUTES
        )
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 9L, true, 90, true, 35f, true, 1_000L)
        val early = ChargeGuardAlarmPolicy.evaluate(
            config, first.state, 9L, true, 90, true, 35f, true,
            1_000L + ChargeGuardRepeatMode.EVERY_3_MINUTES.intervalMs - 1L
        )
        val due = ChargeGuardAlarmPolicy.evaluate(
            config, early.state, 9L, true, 90, true, 35f, true,
            1_000L + ChargeGuardRepeatMode.EVERY_3_MINUTES.intervalMs
        )
        assertTrue(early.alerts.isEmpty())
        assertEquals(1, due.alerts.size)
    }


    @Test fun temperatureIncidentAlsoStopsAfterFiveRepeats() {
        val config = ChargeGuardConfiguration(
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 40,
            repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
        )
        var result = ChargeGuardAlarmPolicy.evaluate(
            config, ChargeGuardAlarmState(), 13L, true, 80, true, 41f, true, 5_000L
        )
        assertEquals(1, result.alerts.size)
        repeat(ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT) { index ->
            result = ChargeGuardAlarmPolicy.evaluate(
                config, result.state, 13L, true, 80, true, 41f, true,
                65_000L + index * ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
            )
            assertEquals(1, result.alerts.size)
        }
        val capped = ChargeGuardAlarmPolicy.evaluate(
            config, result.state, 13L, true, 80, true, 41f, true,
            result.state.temperatureRepeatLastAtEpochMs + ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
        )
        assertTrue(capped.alerts.isEmpty())
        assertEquals(ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT, capped.state.temperatureRepeatCount)
    }

    @Test fun dismissedChargeAndTemperatureIncidentsDoNotRepeat() {
        val config = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 80,
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 40,
            repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
        )
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 10L, true, 85, true, 42f, true, 20_000L)
        val dismissed = first.state.copy(
            chargeRepeatDismissed = true,
            temperatureRepeatDismissed = true
        )
        val later = ChargeGuardAlarmPolicy.evaluate(
            config, dismissed, 10L, true, 85, true, 43f, true,
            20_000L + ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
        )
        assertTrue(later.alerts.isEmpty())
        assertTrue(later.state.chargeRepeatDismissed)
        assertTrue(later.state.temperatureRepeatDismissed)
    }

    @Test fun unpluggingClearsBothActiveRepeatIncidents() {
        val config = ChargeGuardConfiguration(
            chargeLimitEnabled = true,
            targetLevelPercent = 80,
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 40,
            repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
        )
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 11L, true, 90, true, 43f, true, 30_000L)
        val unplugged = ChargeGuardAlarmPolicy.evaluate(
            config, first.state, 11L, false, 90, true, 43f, true, 31_000L
        )
        assertNull(unplugged.state.chargeRepeatSessionId)
        assertEquals(0, unplugged.state.chargeRepeatCount)
        assertEquals(0, unplugged.state.temperatureRepeatCount)
        assertEquals(0L, unplugged.state.temperatureRepeatLastAtEpochMs)
        assertTrue(unplugged.alerts.isEmpty())
    }

    @Test fun temperatureRepeatStopsOnlyAfterStrictlyCrossingHysteresis() {
        val config = ChargeGuardConfiguration(
            highTemperatureEnabled = true,
            highTemperatureThresholdCelsius = 50,
            repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
        )
        val first = ChargeGuardAlarmPolicy.evaluate(config, ChargeGuardAlarmState(), 12L, true, 80, true, 50f, true, 40_000L)
        val atHysteresis = ChargeGuardAlarmPolicy.evaluate(
            config, first.state, 12L, true, 80, true, 48f, true,
            40_000L + ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
        )
        assertFalse(atHysteresis.state.temperatureArmed)
        assertEquals(1, atHysteresis.alerts.size)
        val belowHysteresis = ChargeGuardAlarmPolicy.evaluate(
            config, atHysteresis.state, 12L, true, 80, true, 47.9f, true,
            atHysteresis.state.temperatureRepeatLastAtEpochMs + ChargeGuardRepeatMode.EVERY_1_MINUTE.intervalMs
        )
        assertTrue(belowHysteresis.state.temperatureArmed)
        assertEquals(0, belowHysteresis.state.temperatureRepeatCount)
        assertTrue(belowHysteresis.alerts.isEmpty())
    }

    @Test fun unavailableTemperatureAndDisabledSettingsNeverNotify() {
        val unavailable = ChargeGuardAlarmPolicy.evaluate(
            ChargeGuardConfiguration(highTemperatureEnabled = true), ChargeGuardAlarmState(),
            1L, true, 80, true, 50f, false, 2_000_000L
        )
        val notCharging = ChargeGuardAlarmPolicy.evaluate(
            ChargeGuardConfiguration(highTemperatureEnabled = true), ChargeGuardAlarmState(),
            1L, false, 80, true, 50f, true, 2_000_000L
        )
        val disabled = ChargeGuardAlarmPolicy.evaluate(
            ChargeGuardConfiguration(), ChargeGuardAlarmState(), 1L, true, 100, true, 50f, true, 2_000_000L
        )
        assertTrue(unavailable.alerts.isEmpty())
        assertTrue(notCharging.alerts.isEmpty())
        assertTrue(disabled.alerts.isEmpty())
    }
}
