package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertSame
import org.junit.Test

class DashboardLiveStateSelectorTest {

    @Test
    fun usesForegroundServiceStateWhileServiceIsRunning() {
        val serviceState = LiveBatteryState(level = 88, isCharging = true)
        val fallbackState = LiveBatteryState(level = 61, isCharging = false)

        val selected = selectDashboardLiveState(
            isServiceRunning = true,
            serviceState = serviceState,
            fallbackState = fallbackState
        )

        assertSame(serviceState, selected)
    }

    @Test
    fun switchesToFreshFallbackStateAfterServiceStops() {
        val serviceState = LiveBatteryState(level = 88, isCharging = true)
        val fallbackState = LiveBatteryState(level = 61, isCharging = false)

        val selected = selectDashboardLiveState(
            isServiceRunning = false,
            serviceState = serviceState,
            fallbackState = fallbackState
        )

        assertSame(fallbackState, selected)
    }
}
