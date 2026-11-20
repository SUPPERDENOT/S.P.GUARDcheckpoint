package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState

/**
 * Detects sustained lack of charging input/progress without trusting an OEM fast-charge label.
 * Battery current is measured on the battery side, not directly at the charger input.
 */
internal class ChargingProgressTracker(
    private val noInputThresholdMs: Long = 3 * 60 * 1000L,
    private val stalledProgressThresholdMs: Long = 45 * 60 * 1000L,
    private val noLevelProgressThresholdMs: Long = 90 * 60 * 1000L,
    private val lowProgressCurrentThresholdMa: Int = 300,
    private val nearZeroCurrentThresholdMa: Int = 50
) {
    enum class Issue {
        NO_CHARGING_INPUT,
        CHARGING_PROGRESS_STALLED,
        CHARGING_PROGRESS_NOT_OBSERVED
    }

    private var nearZeroCurrentSinceMs: Long? = null
    private var lowProgressSinceMs: Long? = null
    private var noLevelProgressSinceMs: Long? = null
    private var highestObservedLevel: Int? = null

    fun reset() {
        nearZeroCurrentSinceMs = null
        lowProgressSinceMs = null
        noLevelProgressSinceMs = null
        highestObservedLevel = null
    }

    fun evaluate(state: LiveBatteryState, nowElapsedMs: Long): Issue? {
        if ((!state.isCharging && !state.isPowerConnected) || state.level >= 95) {
            reset()
            return null
        }
        // Missing sensor data is not evidence of a faulty charger.
        if (!state.isCurrentAvailable || !state.isLevelAvailable) {
            reset()
            return null
        }

        if (state.currentMa <= nearZeroCurrentThresholdMa) {
            val since = nearZeroCurrentSinceMs ?: nowElapsedMs.also { nearZeroCurrentSinceMs = it }
            if (nowElapsedMs - since >= noInputThresholdMs) return Issue.NO_CHARGING_INPUT
        } else {
            nearZeroCurrentSinceMs = null
        }

        val highestLevel = highestObservedLevel
        if (highestLevel == null || state.level > highestLevel) {
            // Only a new high-water mark proves net battery-level progress.
            highestObservedLevel = state.level
            lowProgressSinceMs = nowElapsedMs
            noLevelProgressSinceMs = nowElapsedMs
        }

        if (state.currentMa <= lowProgressCurrentThresholdMa) {
            val since = lowProgressSinceMs ?: nowElapsedMs.also { lowProgressSinceMs = it }
            if (nowElapsedMs - since >= stalledProgressThresholdMs) {
                return Issue.CHARGING_PROGRESS_STALLED
            }
        } else {
            lowProgressSinceMs = nowElapsedMs
        }

        val noProgressSince = noLevelProgressSinceMs ?: nowElapsedMs.also { noLevelProgressSinceMs = it }
        if (nowElapsedMs - noProgressSince >= noLevelProgressThresholdMs) {
            return Issue.CHARGING_PROGRESS_NOT_OBSERVED
        }
        return null
    }
}
