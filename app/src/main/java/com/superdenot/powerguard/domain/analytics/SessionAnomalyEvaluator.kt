package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity

enum class SessionAnomaly(val label: String) {
    CHARGE_SPIKE("Lonjakan mAh"),
    HIGH_TEMPERATURE("Suhu Tinggi"),
    ZERO_PERCENT_GAIN("0% Kenaikan"),
    RECOVERY("Pemulihan"),
    RECORDED("Anomali tercatat")
}

/** Session-level heuristics describe recorded patterns, not confirmed hardware faults or protocol identity. */
object SessionAnomalyEvaluator {
    const val RECOVERY_MARKER = "RECOVERY_CHECKPOINT"
    const val MAH_PER_PERCENT_LIMIT = 120.0
    const val ZERO_GAIN_MAH_LIMIT = 250.0
    const val HIGH_TEMPERATURE_CELSIUS = 40.0f
    const val ZERO_GAIN_MIN_DURATION_SECONDS = 120L

    fun evaluate(session: ChargingSessionEntity, endLevel: Int?, nowEpochMs: Long): List<SessionAnomaly> {
        val endTime = session.endTime ?: nowEpochMs
        val durationSeconds = ((endTime - session.startTime) / 1_000L).coerceAtLeast(0L)
        val deltaPercent = endLevel?.minus(session.startLevel)
        val mah = session.totalMahDelivered
        val result = mutableListOf<SessionAnomaly>()

        if (mah.isFinite() && deltaPercent != null && (
            (deltaPercent > 0 && mah / deltaPercent.toDouble() > MAH_PER_PERCENT_LIMIT) ||
                (deltaPercent == 0 && mah > ZERO_GAIN_MAH_LIMIT)
        )) result += SessionAnomaly.CHARGE_SPIKE

        val temperatureAvailable = session.hasTemperatureData || session.maxTempCelsius > 0f
        if (temperatureAvailable && session.maxTempCelsius.isFinite() &&
            session.maxTempCelsius >= HIGH_TEMPERATURE_CELSIUS
        ) result += SessionAnomaly.HIGH_TEMPERATURE

        if (durationSeconds >= ZERO_GAIN_MIN_DURATION_SECONDS && endLevel != null && endLevel <= session.startLevel) {
            result += SessionAnomaly.ZERO_PERCENT_GAIN
        }

        if (session.anomalyReason.orEmpty().split(',').any { it.trim() == RECOVERY_MARKER }) {
            result += SessionAnomaly.RECOVERY
        }
        if (session.isAnomaly) result += SessionAnomaly.RECORDED
        return result.distinct()
    }
}