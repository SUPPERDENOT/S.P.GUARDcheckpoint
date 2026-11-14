package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingSessionDurationTrackerTest {
    @Test
    fun durationUsesElapsedRealtimeRatherThanLoopCount() {
        val tracker = ChargingSessionDurationTracker()
        tracker.startOrResume(elapsedRealtimeMs = 10_000L, persistedDurationSec = 0L)

        assertEquals(0L, tracker.elapsedSeconds(10_500L))
        assertEquals(3L, tracker.elapsedSeconds(13_400L))
        assertEquals(8L, tracker.elapsedSeconds(18_900L))
    }

    @Test
    fun resumedSessionContinuesFromPersistedDuration() {
        val tracker = ChargingSessionDurationTracker()
        tracker.startOrResume(elapsedRealtimeMs = 50_000L, persistedDurationSec = 125L)

        assertEquals(125L, tracker.elapsedSeconds(50_999L))
        assertEquals(127L, tracker.elapsedSeconds(52_100L))
    }

    @Test
    fun backwardOrRepeatedClockSampleNeverReducesDuration() {
        val tracker = ChargingSessionDurationTracker()
        tracker.startOrResume(elapsedRealtimeMs = 20_000L, persistedDurationSec = 10L)

        assertEquals(12L, tracker.elapsedSeconds(22_000L))
        assertEquals(12L, tracker.elapsedSeconds(19_000L))
        assertEquals(13L, tracker.elapsedSeconds(23_000L))
    }

    @Test
    fun resetClearsPreviousSessionBaseline() {
        val tracker = ChargingSessionDurationTracker()
        tracker.startOrResume(elapsedRealtimeMs = 1_000L, persistedDurationSec = 80L)
        tracker.reset()

        assertEquals(0L, tracker.elapsedSeconds(50_000L))
        tracker.startOrResume(elapsedRealtimeMs = 50_000L, persistedDurationSec = 0L)
        assertEquals(2L, tracker.elapsedSeconds(52_000L))
    }
}
