package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyCycleTrendPoint(
    val date: LocalDate,
    val totalCycles: Double,
    val peakTemperatureCelsius: Double?,
    val totalMahCharged: Double,
    val anomalyCount: Int,
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
                totalMahCharged = rollup?.totalMahCharged?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                anomalyCount = rollup?.anomalyCount?.coerceAtLeast(0) ?: 0,
                hasData = rollup != null
            )
        }
    }

    fun formatWeekdayShort(date: LocalDate): String = when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Sen"
        java.time.DayOfWeek.TUESDAY -> "Sel"
        java.time.DayOfWeek.WEDNESDAY -> "Rab"
        java.time.DayOfWeek.THURSDAY -> "Kam"
        java.time.DayOfWeek.FRIDAY -> "Jum"
        java.time.DayOfWeek.SATURDAY -> "Sab"
        java.time.DayOfWeek.SUNDAY -> "Min"
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
