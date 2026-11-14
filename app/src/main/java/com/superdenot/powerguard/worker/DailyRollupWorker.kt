package com.superdenot.powerguard.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyRollupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).batteryDao()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val allSessions = dao.getAllSessionsFlow().first()
        val todaySessions = allSessions.filter { session ->
            val sessionDate = Instant.ofEpochMilli(session.startTime).atZone(zoneId).toLocalDate()
            sessionDate == today
        }

        if (todaySessions.isNotEmpty()) {
            val totalPercentAdded = todaySessions.sumOf { session ->
                val endLvl = session.endLevel ?: session.startLevel
                (endLvl - session.startLevel).coerceAtLeast(0)
            }
            val totalCycles = totalPercentAdded / 100f
            val totalMah = todaySessions.sumOf { it.totalMahDelivered.coerceAtLeast(0.0) }
            val avgTemp = todaySessions.map { it.maxTempCelsius }.average().toFloat()
            val maxTemp = todaySessions.maxOf { it.maxTempCelsius }
            val anomalyCount = todaySessions.count { it.isAnomaly }

            dao.upsertDailyRollup(
                DailyRollupEntity(
                    dateKey = dateKey,
                    totalCycles = totalCycles,
                    totalMahCharged = totalMah,
                    avgTempCelsius = avgTemp,
                    maxTempCelsius = maxTemp,
                    anomalyCount = anomalyCount
                )
            )
        }

        val sevenDaysAgoMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        dao.deleteOldSamples(sevenDaysAgoMillis)

        return Result.success()
    }
}
