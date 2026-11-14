package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.domain.model.LiveBatteryState

private const val LIVE_STATE_MAX_AGE_MS = 5_000L

/**
 * Prefer service data only while its sensor heartbeat is fresh.
 * A stale-but-running service must not suppress the direct-sensor fallback.
 */
internal fun selectDashboardLiveState(
    isServiceRunning: Boolean,
    serviceState: LiveBatteryState,
    fallbackState: LiveBatteryState,
    serviceUpdatedAtElapsedRealtime: Long,
    fallbackUpdatedAtElapsedRealtime: Long,
    nowElapsedRealtime: Long,
    maxAgeMs: Long = LIVE_STATE_MAX_AGE_MS
): LiveBatteryState {
    fun isFresh(timestamp: Long): Boolean {
        if (timestamp <= 0L || maxAgeMs < 0L) return false
        val age = nowElapsedRealtime - timestamp
        return age in 0L..maxAgeMs
    }

    val serviceFresh = isServiceRunning && isFresh(serviceUpdatedAtElapsedRealtime)
    val fallbackFresh = isFresh(fallbackUpdatedAtElapsedRealtime)

    val fallbackWithSessionContext = fallbackState.copy(
        sessionMahDelivered = serviceState.sessionMahDelivered,
        sessionDurationSec = serviceState.sessionDurationSec,
        anomalyAlert = serviceState.anomalyAlert
    )

    return when {
        serviceFresh -> serviceState
        fallbackFresh -> fallbackWithSessionContext
        isServiceRunning -> serviceState
        else -> fallbackWithSessionContext
    }
}
