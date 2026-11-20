package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyDetectorNetDrainTest {
    @Test
    fun negativeCurrentWhileExternalPowerIsConnectedIsNetDrain() {
        assertTrue(isNetDrainWhilePowerConnected(
            LiveBatteryState(isPowerConnected = true, isCurrentAvailable = true, currentMa = -420)
        ))
    }

    @Test
    fun positiveCurrentWhileExternalPowerIsConnectedIsNotNetDrain() {
        assertFalse(isNetDrainWhilePowerConnected(
            LiveBatteryState(isPowerConnected = true, isCurrentAvailable = true, currentMa = 420)
        ))
    }

    @Test
    fun unavailableCurrentIsNotDiagnosedAsNetDrain() {
        assertFalse(isNetDrainWhilePowerConnected(
            LiveBatteryState(isPowerConnected = true, isCurrentAvailable = false, currentMa = -420)
        ))
    }

    @Test
    fun negativeCurrentWithoutExternalPowerIsOrdinaryDischarging() {
        assertFalse(isNetDrainWhilePowerConnected(
            LiveBatteryState(isPowerConnected = false, isCurrentAvailable = true, currentMa = -420)
        ))
    }
}
