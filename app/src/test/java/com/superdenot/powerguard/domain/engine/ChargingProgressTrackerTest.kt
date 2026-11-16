package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingProgressTrackerTest {

    @Test
    fun reportsNoInputOnlyAfterSustainedNearZeroCurrent() {
        val tracker = ChargingProgressTracker(noInputThresholdMs = 180_000L)
        val state = LiveBatteryState(isCharging = true, level = 40, currentMa = 0)
        assertNull(tracker.evaluate(state, 1_000L))
        assertNull(tracker.evaluate(state, 180_999L))
        assertEquals(ChargingProgressTracker.Issue.NO_CHARGING_INPUT, tracker.evaluate(state, 181_000L))
    }

    @Test
    fun unavailableCurrentIsNotEvidenceOfChargerFailure() {
        val tracker = ChargingProgressTracker(noInputThresholdMs = 1L, noLevelProgressThresholdMs = 1L)
        val state = LiveBatteryState(isCharging = true, level = 40, currentMa = 0, isCurrentAvailable = false)
        assertNull(tracker.evaluate(state, 1_000L))
        assertNull(tracker.evaluate(state, 600_000L))
    }

    @Test
    fun reportsStalledProgressOnlyAfterLongLowCurrentWindow() {
        val tracker = ChargingProgressTracker(stalledProgressThresholdMs = 2_700_000L)
        val state = LiveBatteryState(isCharging = true, level = 40, currentMa = 200)
        assertNull(tracker.evaluate(state, 10_000L))
        assertNull(tracker.evaluate(state, 2_709_999L))
        assertEquals(ChargingProgressTracker.Issue.CHARGING_PROGRESS_STALLED, tracker.evaluate(state, 2_710_000L))
    }

    @Test
    fun reportsNoLevelProgressAfterNinetyMinutesEvenWhenCurrentIsHigher() {
        val tracker = ChargingProgressTracker(
            stalledProgressThresholdMs = 10_000_000L,
            noLevelProgressThresholdMs = 5_400_000L
        )
        val state = LiveBatteryState(isCharging = true, level = 40, currentMa = 1_800)
        assertNull(tracker.evaluate(state, 1_000L))
        assertNull(tracker.evaluate(state, 5_400_999L))
        assertEquals(
            ChargingProgressTracker.Issue.CHARGING_PROGRESS_NOT_OBSERVED,
            tracker.evaluate(state, 5_401_000L)
        )
    }

    @Test
    fun levelIncreaseRestartsProgressWindows() {
        val tracker = ChargingProgressTracker(
            stalledProgressThresholdMs = 100L,
            noLevelProgressThresholdMs = 200L
        )
        val first = LiveBatteryState(isCharging = true, level = 40, currentMa = 200)
        val increased = first.copy(level = 41)
        assertNull(tracker.evaluate(first, 0L))
        assertNull(tracker.evaluate(increased, 90L))
        assertNull(tracker.evaluate(increased, 150L))
        assertEquals(ChargingProgressTracker.Issue.CHARGING_PROGRESS_STALLED, tracker.evaluate(increased, 190L))
    }

    @Test
    fun levelDecreaseDoesNotResetNetProgressWindow() {
        val tracker = ChargingProgressTracker(
            stalledProgressThresholdMs = 10_000_000L,
            noLevelProgressThresholdMs = 100L
        )
        val first = LiveBatteryState(isCharging = true, level = 40, currentMa = 1_000)
        assertNull(tracker.evaluate(first, 0L))
        assertNull(tracker.evaluate(first.copy(level = 39), 50L))
        assertEquals(
            ChargingProgressTracker.Issue.CHARGING_PROGRESS_NOT_OBSERVED,
            tracker.evaluate(first.copy(level = 39), 100L)
        )
    }

    @Test
    fun resetsWhenChargingStopsOrSensorsAreUnavailable() {
        val tracker = ChargingProgressTracker(noInputThresholdMs = 100L, noLevelProgressThresholdMs = 100L)
        val charging = LiveBatteryState(isCharging = true, level = 40, currentMa = 0)
        assertNull(tracker.evaluate(charging, 0L))
        assertNull(tracker.evaluate(charging.copy(isCharging = false), 200L))
        assertNull(tracker.evaluate(charging, 250L))
        assertNull(tracker.evaluate(charging.copy(isLevelAvailable = false), 400L))
        assertNull(tracker.evaluate(charging, 450L))
    }
}
