package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState

/**
 * Detects sustained lack of charging input/progress without trusting the OEM fast-charge label.
 * Uses elapsed realtime supplied by the caller so wall-clock changes cannot skew durations.
 */
internal class ChargingProgressTracker(
    private val noInputThresholdMs: Long = 3 * 60 * 1000L,
    private val stalledProgressThresholdMs: Long = 45 * 60 * 1000L,
    private val lowProgressCurrentThresholdMa: Int = 300,
    private val nearZeroCurrentThresholdMa: Int = 50
) {
    enum class Issue {
        NO_CHARGING_INPUT,
        CHARGING_PROGRESS_STALLED
    }

    private var nearZeroCurrentSinceMs: Long? = null
    private var lowProgressSinceMs: Long? = null
    private var lastObservedLevel: Int? = null

    fun reset() {
        nearZeroCurrentSinceMs = null
        lowProgressSinceMs = null
        lastObservedLevel = null
    }

    fun evaluate(state: LiveBatteryState, nowElapsedMs: Long): Issue? {
        if (!state.isCharging || state.level >= 95) {
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

        val previousLevel = lastObservedLevel
        if (previousLevel == null || state.level != previousLevel) {
            lastObservedLevel = state.level
            lowProgressSinceMs = nowElapsedMs
        }

        if (state.currentMa <= lowProgressCurrentThresholdMa) {
            val since = lowProgressSinceMs ?: nowElapsedMs.also { lowProgressSinceMs = it }
            if (nowElapsedMs - since >= stalledProgressThresholdMs) {
                return Issue.CHARGING_PROGRESS_STALLED
            }
        } else {
            lowProgressSinceMs = nowElapsedMs
        }
        return null
    }
}
