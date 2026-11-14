package com.superdenot.powerguard.ui.viewmodel

import com.superdenot.powerguard.domain.model.LiveBatteryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Polls direct battery snapshots while the foreground monitor service is inactive.
 * A failed read is isolated to one tick; the next tick still runs.
 */
internal class FallbackSensorPoller(
    private val intervalMs: Long = 1_500L
) {
    suspend fun runWhileActive(
        isServiceRunning: () -> Boolean,
        readSnapshot: () -> LiveBatteryState,
        onSnapshot: (LiveBatteryState) -> Unit,
        onReadError: (Throwable) -> Unit
    ) {
        while (currentCoroutineContext().isActive) {
            if (!isServiceRunning()) {
                try {
                    onSnapshot(readSnapshot())
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
