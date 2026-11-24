package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionPresentation(
    val endLevel: Int?,
    val deltaPercent: Int?
    val durationSeconds: Long,
    val sourceLabel: String,
    val powerLabel: String,
    val temperatureLabel: String,
    val anomalies: List<SessionAnomaly>
)

object SessionPresentationMapper {
    fun map(session: ChargingSessionEntity, liveLevel: Int, nowEpochMs: Long): SessionPresentation {
        val endLevel = session.endLevel ?: if (session.endTime == null) liveLevel else null
        val endTime = session.endTime ?: nowEpochMs
        val durationSeconds = ((endTime - session.startTime) / 1_000L).coerceAtLeast(0L)
        val temperatureAvailable = session.hasTemperatureData ||
            (session.maxTempCelsius.isFinite() && session.maxTempCelsius > 0f)
        return SessionPresentation(
            endLevel = endLevel,
            deltaPercent = endLevel?.minus(session.startLevel),
            durationSeconds = durationSeconds,
            sourceLabel = session.chargerType.takeIf { it.isNotBlank() } ?: "Sumber —",
            // The entity has no persisted session-average voltage/power pair; never infer W from current alone.
            powerLabel = "— W",
            temperatureLabel = if (temperatureAvailable && session.maxTempCelsius.isFinite()) {
                String.format(Locale.US, "%.1f°C", session.maxTempCelsius)
            } else "—°C",
            anomalies = SessionAnomalyEvaluator.evaluate(session, endLevel, nowEpochMs)
        )
    }

    fun dateHeaderLabel(timestampMs: Long, nowEpochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        return when (date) {
            today -> "Hari Ini"
            today.minusDays(1) -> "Kemarin"
            else -> DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")).format(date)
        }
    }

    fun localDate(timestampMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate()
}