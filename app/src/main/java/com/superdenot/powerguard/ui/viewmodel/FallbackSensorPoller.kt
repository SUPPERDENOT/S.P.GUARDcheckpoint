package com.superdenot.powerguard.ui.viewmodel

import com.superdenot.powerguard.domain.model.LiveBatteryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Polls direct battery snapshots whenever the foreground service has no fresh sensor data.
 * A failed read is isolated to one tick; the next tick still runs.
 */
internal class FallbackSensorPoller(
    private val intervalMs: Long = 1_500L
) {
    suspend fun runWhileActive(
        isAppForeground: () -> Boolean,
        isBatteryMonitoringEnabled: () -> Boolean,
        isServiceDataFresh: () -> Boolean,
        readSnapshot: () -> LiveBatteryState,
        onSnapshot: suspend (LiveBatteryState) -> Unit,
        onReadError: (Throwable) -> Unit
    ) {
        while (currentCoroutineContext().isActive) {
            if (isAppForeground() && isBatteryMonitoringEnabled() && !isServiceDataFresh()) {
                try {
                    val snapshot = readSnapshot()
                    if (isAppForeground() && isBatteryMonitoringEnabled() && !isServiceDataFresh()) {
                        onSnapshot(snapshot)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    onReadError(error)
                }
            }
            delay(intervalMs)
        }
    }
}
