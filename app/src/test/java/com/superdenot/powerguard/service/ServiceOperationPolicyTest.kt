package com.superdenot.powerguard.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceOperationPolicyTest {
    @Test
    fun fourToggleCombinationsResolveToExpectedModesAndKeepAlive() {
        val cases = listOf(
            Triple(false, false, ServiceOperationMode.STOPPED),
            Triple(false, true, ServiceOperationMode.HUD_ONLY),
            Triple(true, false, ServiceOperationMode.MONITORING_ACTIVE),
            Triple(true, true, ServiceOperationMode.MONITORING_ACTIVE)
        )
        cases.forEach { (monitoring, indicator, expectedMode) ->
            assertEquals(expectedMode, ServiceOperationPolicy.resolveMode(monitoring, indicator, false))
            assertEquals(monitoring || indicator, ServiceOperationPolicy.shouldKeepServiceAlive(monitoring, indicator, false))
        }
    }

    @Test
    fun unpluggedMonitoringUsesHudOnlyWhenIndicatorIsEnabledOtherwiseStandby() {
        assertEquals(
            ServiceOperationMode.HUD_ONLY,
            ServiceOperationPolicy.resolveMode(true, true, false, isCharging = false)
        )
        assertEquals(
            ServiceOperationMode.MONITORING_STANDBY,
            ServiceOperationPolicy.resolveMode(true, false, false, isCharging = false)
        )
        assertEquals(
            ServiceOperationMode.MONITORING_ACTIVE,
            ServiceOperationPolicy.resolveMode(true, false, false, isCharging = true)
        )
    }

    @Test
    fun recoveryHoldOverridesBothTogglesAndKeepsServiceAlive() {
        listOf(false, true).forEach { monitoring ->
            listOf(false, true).forEach { indicator ->
                assertEquals(
                    ServiceOperationMode.RECOVERY_HOLD,
                    ServiceOperationPolicy.resolveMode(monitoring, indicator, true)
                )
                assertTrue(ServiceOperationPolicy.shouldKeepServiceAlive(monitoring, indicator, true))
            }
        }
    }

    @Test
    fun monitoringOnToOffFinalizesAnExistingSessionOnlyForTheActualTransition() {
        assertTrue(ServiceOperationPolicy.shouldFinalizeSessionOnMonitoringDisable(true, false, true))
        assertFalse(ServiceOperationPolicy.shouldFinalizeSessionOnMonitoringDisable(false, false, true))
        assertFalse(ServiceOperationPolicy.shouldFinalizeSessionOnMonitoringDisable(true, true, true))
        assertFalse(ServiceOperationPolicy.shouldFinalizeSessionOnMonitoringDisable(true, false, false))
    }

    @Test
    fun chargerTransitionsInHudOnlyNeverEnableRoomRecording() {
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.HUD_ONLY, true, true))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.HUD_ONLY, false, true))
        assertTrue(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.MONITORING_ACTIVE, true, true))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.MONITORING_ACTIVE, false, true))
    }

    @Test
    fun roomRecordingRequiresMonitoringChargingAndAnActiveSession() {
        assertTrue(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.MONITORING_ACTIVE, true, true))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.HUD_ONLY, true, true))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.MONITORING_ACTIVE, false, true))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.MONITORING_ACTIVE, true, false))
        assertFalse(ServiceOperationPolicy.mayRecordRoom(ServiceOperationMode.RECOVERY_HOLD, true, true))
    }
}

class PersistentNotificationRenderGateTest {
    @Test
    fun identicalSignatureIsSuppressed() {
        val gate = PersistentNotificationRenderGate()
        assertTrue(gate.evaluate("icon-a", "SPG", "1000mA", 1_000L).shouldNotify)
        assertFalse(gate.evaluate("icon-a", "SPG", "1000mA", 2_000L).shouldNotify)
    }

    @Test
    fun bodyChangesAreThrottledButIconChangesAreImmediate() {
        val gate = PersistentNotificationRenderGate()
        gate.evaluate("icon-a", "SPG", "1000mA", 1_000L)
        val throttled = gate.evaluate("icon-a", "SPG", "1100mA", 2_000L)
        assertFalse(throttled.shouldNotify)
        assertEquals("1000mA", throttled.body)

        val iconChanged = gate.evaluate("icon-b", "SPG", "1100mA", 2_500L)
        assertTrue(iconChanged.shouldNotify)
        assertEquals("1000mA", iconChanged.body)
    }

    @Test
    fun bodyUpdatesAfterThrottleAndResetForcesImmediateNewRender() {
        val gate = PersistentNotificationRenderGate()
        gate.evaluate("icon-a", "SPG", "1000mA", 1_000L)
        val updated = gate.evaluate("icon-a", "SPG", "1100mA", 5_000L)
        assertTrue(updated.shouldNotify)
        assertEquals("1100mA", updated.body)

        gate.reset()
        assertTrue(gate.evaluate("icon-a", "SPG", "900mA", 5_100L).shouldNotify)
    }
}
