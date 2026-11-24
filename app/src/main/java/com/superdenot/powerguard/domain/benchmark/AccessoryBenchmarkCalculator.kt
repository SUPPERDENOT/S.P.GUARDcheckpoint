package com.superdenot.powerguard.domain.benchmark

import java.util.Locale

/**
 * Pure Kotlin aggregation over measured battery-side session samples.
 *
 * Values are averaged per completed session so a longer recording does not dominate a group.
 * Missing metrics remain null; they are never represented as measured zero.
 */
object AccessoryBenchmarkCalculator {

    const val MIN_VALID_DURATION_MILLIS = 5 * 60 * 1_000L
    private const val START_LEVEL_WARNING_PERCENT = 15.0
    private const val START_TEMPERATURE_WARNING_CELSIUS = 4.0

    fun calculate(sessions: List<AccessoryBenchmarkSession>): AccessoryBenchmarkResult {
        val completed = sessions.mapNotNull(::toCompletedSession)
        val grouped = completed.groupBy { it.label.trim().lowercase(Locale.ROOT) }

        val groups = grouped.values.map { labeledSessions ->
            val label = labeledSessions.first().label.trim()
            val metrics = labeledSessions.map(::measureSession)
            AccessoryBenchmarkGroup(
                label = label,
                sessionCount = labeledSessions.size,
                averageBatteryPowerW = metrics.mapNotNull { it.averagePowerW }.averageOrNull(),
                averageCurrentMa = metrics.mapNotNull { it.averageCurrentMa }.averageOrNull(),
                averageChargeRatePercentPerHour = metrics.mapNotNull { it.chargeRatePercentPerHour }.averageOrNull(),
                averageTemperatureRiseCelsius = metrics.mapNotNull { it.temperatureRiseCelsius }.averageOrNull(),
                averageStartLevelPercent = labeledSessions.map { it.startLevelPercent.toDouble() }.averageOrNull(),
                averageStartTemperatureCelsius = metrics.mapNotNull { it.startTemperatureCelsius }.averageOrNull(),
                averagePeakTemperatureCelsius = metrics.mapNotNull { it.peakTemperatureCelsius }.averageOrNull()
            )
        }.sortedBy { it.label.lowercase(Locale.ROOT) }

        return AccessoryBenchmarkResult(
            groups = groups,
            comparabilityNotes = buildComparabilityNotes(groups)
        )
    }

    private fun toCompletedSession(session: AccessoryBenchmarkSession): CompletedSession? {
        val label = session.accessoryLabel.trim()
        val endTime = session.endTimeMillis ?: return null
        val endLevel = session.endLevelPercent ?: return null
        val duration = endTime - session.startTimeMillis
        if (label.isEmpty() || duration < 0L) return null
        if (session.startLevelPercent !in 0..100 || endLevel !in 0..100) return null

        val samples = session.samples
            .asSequence()
            .filter { it.timestampMillis in session.startTimeMillis..endTime }
            .sortedBy { it.timestampMillis }
            .toList()

        return CompletedSession(
            label = label,
            startTimeMillis = session.startTimeMillis,
            endTimeMillis = endTime,
            startLevelPercent = session.startLevelPercent,
            endLevelPercent = endLevel,
            averageCurrentMa = session.averageCurrentMa,
            maxTemperatureCelsius = session.maxTemperatureCelsius,
            samples = samples
        )
    }

    private fun measureSession(session: CompletedSession): SessionMetrics {
        val chargingSamples = session.samples.filter { !it.isChargingStatusAvailable || it.isCharging }
        val powerSamples = chargingSamples.filter { it.isPowerAvailable && it.powerWatt.isFinite() }
        val temperatureSamples = chargingSamples.filter { it.isTemperatureAvailable && it.tempCelsius.isFinite() }

        val durationMillis = session.endTimeMillis - session.startTimeMillis
        val chargeRate = if (durationMillis >= MIN_VALID_DURATION_MILLIS) {
            val durationHours = durationMillis / 3_600_000.0
            (session.endLevelPercent - session.startLevelPercent) / durationHours
        } else {
            null
        }

        val firstTemperature = temperatureSamples.firstOrNull()?.tempCelsius?.toDouble()
        val peakTemperature = session.maxTemperatureCelsius?.takeIf { it.isFinite() }?.toDouble()
        val temperatureRise = if (firstTemperature != null && peakTemperature != null) {
            peakTemperature - firstTemperature
        } else {
            null
        }

        return SessionMetrics(
            averagePowerW = powerSamples.map { it.powerWatt.toDouble() }.averageOrNull(),
            averageCurrentMa = session.averageCurrentMa?.toDouble(),
            chargeRatePercentPerHour = chargeRate,
            temperatureRiseCelsius = temperatureRise,
            startTemperatureCelsius = firstTemperature,
            peakTemperatureCelsius = peakTemperature
        )
    }

    private fun buildComparabilityNotes(groups: List<AccessoryBenchmarkGroup>): List<String> {
        if (groups.size < 2) return emptyList()
        val notes = mutableListOf<String>()

        for (leftIndex in 0 until groups.lastIndex) {
            for (rightIndex in leftIndex + 1 until groups.size) {
                val left = groups[leftIndex]
                val right = groups[rightIndex]
                val startLevelDifference = differenceOrNull(
                    left.averageStartLevelPercent,
                    right.averageStartLevelPercent
                )
                if (startLevelDifference != null && startLevelDifference > START_LEVEL_WARNING_PERCENT) {
                    notes += String.format(
                        Locale.US,
                        "Level awal %s dan %s berbeda %.1f poin persentase (>15).",
                        left.label,
                        right.label,
                        startLevelDifference
                    )
                }

                val startTemperatureDifference = differenceOrNull(
                    left.averageStartTemperatureCelsius,
                    right.averageStartTemperatureCelsius
                )
                if (startTemperatureDifference != null &&
                    startTemperatureDifference > START_TEMPERATURE_WARNING_CELSIUS
                ) {
                    notes += String.format(
                        Locale.US,
                        "Suhu awal %s dan %s berbeda %.1f°C (>4°C).",
                        left.label,
                        right.label,
                        startTemperatureDifference
                    )
                }
            }
        }
        return notes
    }

    private fun differenceOrNull(left: Double?, right: Double?): Double? =
        if (left == null || right == null) null else kotlin.math.abs(left - right)

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average().takeIf { it.isFinite() }

    private data class CompletedSession(
        val label: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val startLevelPercent: Int,
        val endLevelPercent: Int,
        val averageCurrentMa: Int?,
        val maxTemperatureCelsius: Float?,
        val samples: List<AccessoryBenchmarkSample>
    )

    private data class SessionMetrics(
        val averagePowerW: Double?,
        val averageCurrentMa: Double?,
        val chargeRatePercentPerHour: Double?,
        val temperatureRiseCelsius: Double?,
        val startTemperatureCelsius: Double?,
        val peakTemperatureCelsius: Double?
    )
}
