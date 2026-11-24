package com.superdenot.powerguard.domain.analytics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CurrentChartScale(val minimumMilliAmps: Float, val maximumMilliAmps: Float)

object CurrentChartScaleCalculator {
    const val OUTLIER_LIMIT_MILLI_AMPS = 25_000

    /** Filters only display-scale inputs; raw samples are never modified. */
    fun calculate(readingsMilliAmps: List<Int>): CurrentChartScale {
        val displayReadings = readingsMilliAmps.filter { abs(it.toLong()) <= OUTLIER_LIMIT_MILLI_AMPS }
        val minimum = if (displayReadings.isNotEmpty()) min(0f, displayReadings.minOf { it.toFloat() }) else 0f
        val maximum = if (displayReadings.isNotEmpty()) max(500f, displayReadings.maxOf { it.toFloat() }) else 500f
        return CurrentChartScale(minimum, maximum)
    }
}
