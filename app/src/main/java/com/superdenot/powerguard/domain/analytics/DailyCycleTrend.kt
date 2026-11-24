package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyCycleTrendPoint(
    val date: LocalDate,
    val totalCycles: Double,
    val peakTemperatureCelsius: Double?,
    val hasData: Boolean
)

object DailyCycleTrend {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale("id", "ID"))

    fun aggregate(rollups: List<DailyRollupEntity>, today: LocalDate): List<DailyCycleTrendPoint> {
        val byDate = rollups.mapNotNull { rollup ->
            runCatching { LocalDate.parse(rollup.dateKey) }.getOrNull()?.let { it to rollup }
        }.toMap()
        return (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            val rollup = byDate[date]
            DailyCycleTrendPoint(
                date = date,
                totalCycles = rollup?.totalCycles?.toDouble()?.takeIf { it.isFinite() } ?: 0.0,
                peakTemperatureCelsius = rollup?.maxTempCelsius?.toDouble()
                    ?.takeIf { it.isFinite() && it > 0.0 },
                hasData = rollup != null
            )
        }
    }

    fun formatDateLabel(dateKey: String, today: LocalDate): String {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
        val prefix = when (date) {
            today -> "Hari Ini"
            today.minusDays(1) -> "Kemarin"
            else -> null
        }
        val formatted = dateFormatter.format(date)
        return if (prefix == null) formatted else "$prefix · $formatted"
    }
}
