package com.superdenot.powerguard.ui.viewmodel

import com.superdenot.powerguard.domain.model.LiveBatteryState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackSensorPollerTest {

    @Test
    fun retriesAfterOneSensorReadFailure() = runTest {
        val poller = FallbackSensorPoller()
        var reads = 0
        val levels = mutableListOf<Int>()
        val errors = mutableListOf<String>()

        backgroundScope.launch {
            poller.runWhileActive(
                isServiceRunning = { false },
                readSnapshot = {
                    reads++
                    if (reads == 1) error("temporary sensor failure")
                    LiveBatteryState(level = 73)
                },
                onSnapshot = { levels += it.level },
                onReadError = { errors += it.message.orEmpty() }
            )
        }

        runCurrent()
        assertEquals(1, reads)
        assertEquals(listOf("temporary sensor failure"), errors)
        assertEquals(emptyList<Int>(), levels)

        advanceTimeBy(1_500L)
        runCurrent()
        assertEquals(2, reads)
        assertEquals(listOf(73), levels)
    }

    @Test
    fun pausesReadsWhileForegroundServiceOwnsLiveState() = runTest {
        val poller = FallbackSensorPoller()
        var serviceRunning = true
        var reads = 0

        backgroundScope.launch {
            poller.runWhileActive(
                isServiceRunning = { serviceRunning },
                readSnapshot = {
                    reads++
                    LiveBatteryState(level = 81)
                },
                onSnapshot = {},
                onReadError = { throw AssertionError("Unexpected sensor error", it) }
            )
        }

        runCurrent()
        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(0, reads)

        serviceRunning = false
        advanceTimeBy(1_500L)
        runCurrent()
        assertEquals(1, reads)
    }

    @Test
    fun forwardsFreshSnapshotsOnEveryTick() = runTest {
        val poller = FallbackSensorPoller()
        var nextLevel = 40
        val levels = mutableListOf<Int>()

        backgroundScope.launch {
            poller.runWhileActive(
                isServiceRunning = { false },
                readSnapshot = { LiveBatteryState(level = nextLevel++) },
                onSnapshot = { levels += it.level },
                onReadError = { throw AssertionError("Unexpected sensor error", it) }
            )
        }

        runCurrent()
        advanceTimeBy(1_500L)
        runCurrent()
        advanceTimeBy(1_500L)
        runCurrent()

        assertEquals(listOf(40, 41, 42), levels)
    }
}
