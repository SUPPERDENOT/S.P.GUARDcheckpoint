package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertSame
import org.junit.Test

class DashboardLiveStateSelectorTest {

    @Test
    fun prefersFreshServiceState() {
        val serviceState = LiveBatteryState(level = 88, isCharging = true, sessionMahDelivered = 123.4, sessionDurationSec = 456L, anomalyAlert = "warning")
        val fallbackState = LiveBatteryState(level = 61, isCharging = false)

        val selected = selectDashboardLiveState(
            isServiceRunning = true,
            serviceState = serviceState,
            fallbackState = fallbackState,
            serviceUpdatedAtElapsedRealtime = 9_000L,
            fallbackUpdatedAtElapsedRealtime = 9_500L,
            nowElapsedRealtime = 10_000L
        )

        assertSame(serviceState, selected)
    }

    @Test
    fun usesFallbackWhenServiceIsRunningButItsHeartbeatIsStale() {
        val serviceState = LiveBatteryState(level = 88, isCharging = true)
        val fallbackState = LiveBatteryState(level = 61, isCharging = false)

        val selected = selectDashboardLiveState(
            isServiceRunning = true,
            serviceState = serviceState,
            fallbackState = fallbackState,
            serviceUpdatedAtElapsedRealtime = 4_000L,
            fallbackUpdatedAtElapsedRealtime = 9_500L,
            nowElapsedRealtime = 10_000L
        )

        org.junit.Assert.assertEquals(61, selected.level)
        org.junit.Assert.assertEquals(123.4, selected.sessionMahDelivered, 0.001)
        org.junit.Assert.assertEquals(456L, selected.sessionDurationSec)
        org.junit.Assert.assertEquals("warning", selected.anomalyAlert)
    }

    @Test
    fun doesNotTreatFutureHeartbeatAsFresh() {
        val serviceState = LiveBatteryState(level = 88)
        val fallbackState = LiveBatteryState(level = 61)

        val selected = selectDashboardLiveState(
            isServiceRunning = true,
            serviceState = serviceState,
            fallbackState = fallbackState,
            serviceUpdatedAtElapsedRealtime = 10_100L,
            fallbackUpdatedAtElapsedRealtime = 9_500L,
            nowElapsedRealtime = 10_000L
        )

        assertSame(fallbackState, selected)
    }

    @Test
    fun keepsLastKnownServiceStateWhenBothSourcesAreStaleAndServiceIsRunning() {
        val serviceState = LiveBatteryState(level = 88)
        val fallbackState = LiveBatteryState(level = 61)

        val selected = selectDashboardLiveState(
            isServiceRunning = true,
            serviceState = serviceState,
            fallbackState = fallbackState,
            serviceUpdatedAtElapsedRealtime = 1_000L,
            fallbackUpdatedAtElapsedRealtime = 2_000L,
            nowElapsedRealtime = 10_000L
        )

        assertSame(serviceState, selected)
    }

    @Test
    fun usesFallbackAfterServiceStops() {
        val serviceState = LiveBatteryState(level = 88)
        val fallbackState = LiveBatteryState(level = 61)

        val selected = selectDashboardLiveState(
            isServiceRunning = false,
            serviceState = serviceState,
            fallbackState = fallbackState,
            serviceUpdatedAtElapsedRealtime = 9_900L,
            fallbackUpdatedAtElapsedRealtime = 9_500L,
            nowElapsedRealtime = 10_000L
        )

        org.junit.Assert.assertEquals(61, selected.level)
        org.junit.Assert.assertEquals(456L, selected.sessionDurationSec)
    }
}
