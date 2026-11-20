package com.superdenot.powerguard.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoStartAttemptGateTest {

    @Test
    fun keepsFirstDetectionTimestampWithinOneChargingEpoch() {
        val gate = AutoStartAttemptGate()

        val first = gate.recordChargingDetection(1_000L)
        val repeated = gate.recordChargingDetection(8_000L)

        assertTrue(first.isNew)
        assertFalse(repeated.isNew)
        assertEquals(first.epochId, repeated.epochId)
        assertEquals(1_000L, repeated.detectedAtElapsedRealtime)
    }

    @Test
    fun createsNewEpochAfterChargingDisconnect() {
        val gate = AutoStartAttemptGate()

        val first = gate.recordChargingDetection(1_000L)
        gate.resetChargingEpoch()
        val second = gate.recordChargingDetection(9_000L)

        assertTrue(second.isNew)
        assertTrue(second.epochId > first.epochId)
        assertEquals(9_000L, second.detectedAtElapsedRealtime)
    }

    @Test
    fun allowsOnlyOneConcurrentRequest() {
        val gate = AutoStartAttemptGate()

        assertTrue(gate.tryAcquire())
        assertFalse(gate.tryAcquire())

        gate.release()

        assertTrue(gate.tryAcquire())
        gate.release()
    }

    @Test
    fun generatesIncreasingAttemptIds() {
        val gate = AutoStartAttemptGate()

        assertEquals(1L, gate.nextAttemptId())
        assertEquals(2L, gate.nextAttemptId())
    }

    @Test
    fun powerConnectedBackgroundTriggerRequiresBatteryOptimizationExemption() {
        assertFalse(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.POWER_CONNECTED,
                appForeground = false,
                batteryOptimizationIgnored = false
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.POWER_CONNECTED,
                appForeground = false,
                batteryOptimizationIgnored = true
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.POWER_CONNECTED,
                appForeground = true,
                batteryOptimizationIgnored = false
            )
        )
    }

    @Test
    fun workManagerChargingTriggerRequiresBatteryOptimizationExemptionInBackground() {
        assertFalse(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.WORK_MANAGER_CHARGING,
                appForeground = false,
                batteryOptimizationIgnored = false
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.WORK_MANAGER_CHARGING,
                appForeground = false,
                batteryOptimizationIgnored = true
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.WORK_MANAGER_CHARGING,
                appForeground = true,
                batteryOptimizationIgnored = false
            )
        )
    }

    @Test
    fun bootTriggerRetainsItsBackgroundStartPath() {
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.BOOT_COMPLETED,
                appForeground = false,
                batteryOptimizationIgnored = false
            )
        )
    }

    @Test
    fun appDrivenTriggersRequireForegroundActivity() {
        assertFalse(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.APP_FOREGROUND_CHARGING,
                appForeground = false,
                batteryOptimizationIgnored = true
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.MONITORING_ENABLED,
                appForeground = true,
                batteryOptimizationIgnored = false
            )
        )
        assertTrue(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.STATUS_BAR_INDICATOR_ENABLED,
                appForeground = true,
                batteryOptimizationIgnored = false
            )
        )
        assertFalse(
            AutoStartPolicy.isAllowed(
                AutoStartTrigger.STATUS_BAR_INDICATOR_ENABLED,
                appForeground = false,
                batteryOptimizationIgnored = true
            )
        )
    }
}
