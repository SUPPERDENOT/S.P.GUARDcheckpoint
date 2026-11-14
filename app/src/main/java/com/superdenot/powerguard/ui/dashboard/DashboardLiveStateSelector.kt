package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.domain.model.LiveBatteryState

/**
 * Selects the authoritative live battery state for the dashboard.
 * The foreground service owns the state while it is running; otherwise
 * the direct-sensor fallback is authoritative.
 */
internal fun selectDashboardLiveState(
    isServiceRunning: Boolean,
    serviceState: LiveBatteryState,
    fallbackState: LiveBatteryState
): LiveBatteryState = if (isServiceRunning) serviceState else fallbackState
