package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity

enum class HistorySessionFilter(val label: String) {
    MICRO("< 1 mnt"),
    ALL("Semua"),
    ANOMALY("Anomali"),
    LABELED("Berlabel"),
    GAIN_10_PERCENT("Kenaikan ≥10%")
}

object HistorySessionFilterMatcher {
    fun matches(session: ChargingSessionEntity, filter: HistorySessionFilter, liveLevel: Int, nowEpochMs: Long): Boolean {
        val presentation = SessionPresentationMapper.map(session, liveLevel, nowEpochMs)
        return when (filter) {
            HistorySessionFilter.MICRO -> !SessionMicroFilter.shouldHide(
                presentation.durationSeconds, presentation.deltaPercent ?: 0, session.totalMahDelivered
            )
            HistorySessionFilter.ALL -> true
            HistorySessionFilter.ANOMALY -> presentation.anomalies.isNotEmpty()
            HistorySessionFilter.LABELED -> !session.accessoryLabel.isNullOrBlank()
            HistorySessionFilter.GAIN_10_PERCENT -> presentation.deltaPercent?.let { it >= 10 } == true
        }
    }

    fun filter(sessions: List<ChargingSessionEntity>, filter: HistorySessionFilter, liveLevel: Int, nowEpochMs: Long) =
        sessions.filter { matches(it, filter, liveLevel, nowEpochMs) }
}