package com.superdenot.powerguard.domain.engine

import kotlin.math.ceil
import kotlin.math.exp

/** Timestamp-weighted EMA over positive battery-side charging current; estimate only. */
internal class ChargingEtaEstimator(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val halfLifeMs: Double = DEFAULT_HALF_LIFE_MS,
    private val minimumSamples: Int = MINIMUM_SAMPLES
) {
    private data class Sample(val currentMa: Int, val elapsedRealtimeMs: Long)
    private val samples = ArrayDeque<Sample>()
    init { require(windowMs > 0L && halfLifeMs > 0.0 && minimumSamples >= 1) }

    fun addSample(currentMa: Int, elapsedRealtimeMs: Long, isCharging: Boolean,
                  isPowerConnected: Boolean, isCurrentAvailable: Boolean) {
        if (!isCharging || !isPowerConnected || !isCurrentAvailable || currentMa <= 0) {
            reset(); return
        }
        val previous = samples.lastOrNull()
        if (previous != null && elapsedRealtimeMs <= previous.elapsedRealtimeMs) return
        samples.addLast(Sample(currentMa, elapsedRealtimeMs))
        prune(elapsedRealtimeMs)
    }

    fun estimateMinutes(level: Int, capacityMah: Int, nowElapsedRealtimeMs: Long): Long? {
        if (level !in 0..99 || capacityMah !in MIN_CAPACITY_MAH..MAX_CAPACITY_MAH) return null
        prune(nowElapsedRealtimeMs)
        if (samples.size < minimumSamples) return null
        val latest = samples.lastOrNull() ?: return null
        if (nowElapsedRealtimeMs - latest.elapsedRealtimeMs !in 0L..MAX_SAMPLE_AGE_MS) return null
        var weightedCurrent = 0.0
        var totalWeight = 0.0
        samples.forEach { sample ->
            val ageMs = (nowElapsedRealtimeMs - sample.elapsedRealtimeMs).coerceAtLeast(0L)
            val weight = exp(-LN_2 * ageMs.toDouble() / halfLifeMs)
            weightedCurrent += sample.currentMa * weight
            totalWeight += weight
        }
        if (totalWeight <= 0.0) return null
        val emaCurrentMa = weightedCurrent / totalWeight
        if (!emaCurrentMa.isFinite() || emaCurrentMa <= MINIMUM_ETA_CURRENT_MA) return null
        val ccMah = if (level < CV_START_PERCENT) capacityMah * (CV_START_PERCENT - level) / 100.0 else 0.0
        val cvMah = capacityMah * (100 - maxOf(level, CV_START_PERCENT)) / 100.0
        val remainingMah = ccMah + cvMah * CV_TAPER_FACTOR
        if (remainingMah <= 0.0) return null
        return ceil(remainingMah * 60.0 / emaCurrentMa).toLong().coerceAtLeast(1L)
    }

    fun reset() { samples.clear() }
    internal fun sampleCount(): Int = samples.size
    private fun prune(nowElapsedRealtimeMs: Long) {
        while (samples.isNotEmpty() && nowElapsedRealtimeMs - samples.first().elapsedRealtimeMs > windowMs) samples.removeFirst()
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 120_000L
        const val DEFAULT_HALF_LIFE_MS = 30_000.0
        private const val LN_2 = 0.6931471805599453
        const val MINIMUM_SAMPLES = 3
        const val MINIMUM_ETA_CURRENT_MA = 50.0
        const val MIN_CAPACITY_MAH = 1_000
        const val MAX_CAPACITY_MAH = 20_000
        const val CV_START_PERCENT = 80
        const val CV_TAPER_FACTOR = 1.25
        const val MAX_SAMPLE_AGE_MS = 5_000L
    }
}
