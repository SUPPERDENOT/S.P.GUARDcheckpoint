package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun unavailableTemperatureAndDisabledSettingsNeverNotify() {
        val unavailable = ChargeGuardAlarmPolicy.evaluate(
            ChargeGuardConfiguration(highTemperatureEnabled = true), ChargeGuardAlarmState(),
            1L, true, 80, true, 50f, false, 2_000_000L
        )
        val disabled = ChargeGuardAlarmPolicy.evaluate(
            ChargeGuardConfiguration(), ChargeGuardAlarmState(), 1L, true, 100, true, 50f, true, 2_000_000L
        )
        assertTrue(unavailable.alerts.isEmpty())
        assertTrue(disabled.alerts.isEmpty())
    }
}
