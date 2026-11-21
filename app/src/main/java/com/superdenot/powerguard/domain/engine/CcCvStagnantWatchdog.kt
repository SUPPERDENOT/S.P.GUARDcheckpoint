package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState

/** CC/CV stagnant charging heuristic based on battery-side current and observed SoC. */
internal class CcCvStagnantWatchdog(
    private val belowEightyThresholdMa: Int = BELOW_EIGHTY_CURRENT_MA,
    private val cvThresholdMa: Int = CV_CURRENT_MA,
    private val belowEightyDurationMs: Long = BELOW_EIGHTY_DURATION_MS,
    private val cvDurationMs: Long = CV_DURATION_MS,
    private val recoveryCurrentMa: Int = RECOVERY_CURRENT_MA,
    private val recoveryDurationMs: Long = RECOVERY_DURATION_MS
) {
    private var highestObservedLevel: Int? = null
    private var lowCurrentSinceMs: Long? = null
    private var recoverySinceMs: Long? = null
    init { require(belowEightyThresholdMa > 0 && cvThresholdMa > 0 && belowEightyDurationMs > 0L && cvDurationMs > 0L && recoveryCurrentMa > 0 && recoveryDurationMs > 0L) }

    fun evaluate(state: LiveBatteryState, nowElapsedMs: Long): Int? {
        if (!state.isCharging || !state.isPowerConnected || !state.isLevelAvailable ||
            !state.isCurrentAvailable || state.level !in 0..99 || nowElapsedMs < 0L
        ) { reset(); return null }
        val thresholdMa = if (state.level < 80) belowEightyThresholdMa else cvThresholdMa
        val previousHigh = highestObservedLevel
        if (previousHigh == null || state.level > previousHigh) {
            highestObservedLevel = state.level
            lowCurrentSinceMs = if (state.currentMa >= 0 && state.currentMa < thresholdMa) nowElapsedMs else null
            recoverySinceMs = null
            return null
        }
        if (state.currentMa >= recoveryCurrentMa) {
            val recoveredAt = recoverySinceMs ?: nowElapsedMs.also { recoverySinceMs = it }
            if (nowElapsedMs - recoveredAt >= recoveryDurationMs) {
                highestObservedLevel = state.level
                lowCurrentSinceMs = null
                recoverySinceMs = null
            }
            return null
        }
        recoverySinceMs = null
        if (state.currentMa >= 0 && state.currentMa < thresholdMa) {
            val stagnantAt = lowCurrentSinceMs ?: nowElapsedMs.also { lowCurrentSinceMs = it }
            val requiredMs = if (state.level < 80) belowEightyDurationMs else cvDurationMs
            if (nowElapsedMs - stagnantAt >= requiredMs) {
                return if (state.level < 80) (belowEightyDurationMs / MILLIS_PER_MINUTE).toInt()
                else (cvDurationMs / MILLIS_PER_MINUTE).toInt()
            }
        } else lowCurrentSinceMs = null
        return null
    }

    fun reset() { highestObservedLevel = null; lowCurrentSinceMs = null; recoverySinceMs = null }

    companion object {
        const val BELOW_EIGHTY_CURRENT_MA = 150
        const val CV_CURRENT_MA = 120
        const val BELOW_EIGHTY_DURATION_MS = 10 * 60_000L
        const val CV_DURATION_MS = 20 * 60_000L
        const val RECOVERY_CURRENT_MA = 300
        const val RECOVERY_DURATION_MS = 30_000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
