package com.superdenot.powerguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import kotlinx.coroutines.flow.Flow

enum class SessionFinalizationStatus {
    FINALIZED,
    ALREADY_CLOSED,
    TARGET_MISSING,
    OPEN_SESSIONS_REMAIN
}

enum class SampleFlushStatus {
    FLUSHED,
    ALREADY_FLUSHED_IDEMPOTENT,
    SESSION_CLOSED_OR_MISSING,
    VERIFICATION_FAILED
}

@Dao
interface BatteryDao {

    @Insert
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargingSessionEntity)

    @Query("SELECT * FROM charging_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE endTime IS NULL ORDER BY startTime ASC")
    suspend fun getAllOpenSessions(): List<ChargingSessionEntity>

    @Query("SELECT * FROM battery_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSessionList(sessionId: Long): List<BatterySampleEntity>

    @Query("UPDATE charging_sessions SET endTime = :endTime, endLevel = :endLevel WHERE endTime IS NULL")
    suspend fun closeAllOpenSessions(endTime: Long, endLevel: Int)

    @Query("UPDATE charging_sessions SET endTime = :endTime, endLevel = :endLevel WHERE endTime IS NULL AND id != :keepSessionId")
    suspend fun closeOpenSessionsExceptQuery(keepSessionId: Long, endTime: Long, endLevel: Int): Int

    @Query("SELECT COUNT(*) FROM battery_samples WHERE sessionId = :sessionId AND timestamp = :timestamp")
    suspend fun countSamplesAtTimestamp(sessionId: Long, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM battery_health_logs WHERE sessionId = :sessionId")
    suspend fun countHealthLogsForSession(sessionId: Long): Int

    suspend fun isSamplePersisted(sessionId: Long, timestamp: Long): Boolean {
        return countSamplesAtTimestamp(sessionId, timestamp) > 0
    }

    /**
     * Menutup dan memfinalisasi SATU sesi terbuka dengan merekonsiliasi metrik dari baris sesi
     * dan sampel milik sesi tersebut di battery_samples, sehingga sesi tidak pernah ditutup
     * dengan metrik kosong/inkonsisten dan tidak memengaruhi sesi lain.
     */
    suspend fun closeSingleOpenSessionWithConsistentMetrics(
        session: ChargingSessionEntity,
        fallbackEndTime: Long,
        fallbackEndLevel: Int
    ): Boolean {
        val fresh = getSessionById(session.id) ?: return false
        if (fresh.endTime != null) return true

        val samples = getSamplesForSessionList(fresh.id)
        val lastSample = samples.lastOrNull()

        val consistentEndTime = maxOf(
            fallbackEndTime,
            fresh.startTime,
            lastSample?.timestamp ?: fresh.startTime
        )
        val consistentEndLevel = when {
            fallbackEndLevel in 0..100 -> fallbackEndLevel
            lastSample != null && lastSample.batteryLevel in 0..100 -> lastSample.batteryLevel
            else -> fresh.startLevel
        }
        val consistentAvgCurrentMa = when {
            fresh.avgCurrentMa != 0 -> fresh.avgCurrentMa
            samples.isNotEmpty() -> samples.map { it.currentMa.toLong() }.average().toInt()
            else -> 0
        }
        val consistentMaxTemp = maxOf(
            fresh.maxTempCelsius,
            samples.maxOfOrNull { it.tempCelsius } ?: 0f
        )
        val consistentTotalMah = if (fresh.totalMahDelivered > 0.0) {
            fresh.totalMahDelivered
        } else if (samples.size >= 2) {
            var integratedMah = 0.0
            for (i in 1 until samples.size) {
                val dtSec = ((samples[i].timestamp - samples[i - 1].timestamp) / 1000.0).coerceAtLeast(0.0)
                if (dtSec > 0.0) {
                    integratedMah += (samples[i].currentMa / 3600.0) * dtSec
                }
            }
            integratedMah.coerceAtLeast(0.0)
        } else {
            0.0
        }

        updateSession(
            fresh.copy(
                endTime = consistentEndTime,
                endLevel = consistentEndLevel,
                totalMahDelivered = consistentTotalMah,
                avgCurrentMa = consistentAvgCurrentMa,
                maxTempCelsius = consistentMaxTemp
            )
        )
        return getSessionById(fresh.id)?.endTime != null
    }

    @Transaction
    suspend fun closeStaleOpenSessionsExcept(keepSessionId: Long, endTime: Long, endLevel: Int): Int {
        val staleSessions = getAllOpenSessions().filter { it.id != keepSessionId }
        var closedCount = 0
        for (stale in staleSessions) {
            val closed = runCatching {
                closeSingleOpenSessionWithConsistentMetrics(stale, endTime, endLevel)
            }.getOrDefault(false)
            if (closed) {
                closedCount++
            }
        }
        return closedCount
    }

    @Transaction
    suspend fun closeAllOpenSessionsAndVerify(endTime: Long, endLevel: Int): Boolean {
        val openSessions = getAllOpenSessions()
        for (openSession in openSessions) {
            runCatching {
                closeSingleOpenSessionWithConsistentMetrics(
                    session = openSession,
                    fallbackEndTime = endTime,
                    fallbackEndLevel = endLevel
                )
            }
        }
        return getAllOpenSessions().isEmpty()
    }

    /**
     * Memfinalisasi SATU sesi terbuka yatim secara atomik & terisolasi menggunakan data dan sampel
     * milik sesi tersebut sendiri. Kegagalan pada sesi ini tidak memengaruhi sesi lain.
     */
    @Transaction
    suspend fun finalizeSingleSessionWithReconciledMetrics(
        sessionId: Long,
        endTime: Long,
        endLevel: Int,
        designCapacityMah: Int
    ): SessionFinalizationStatus {
        val existing = getSessionById(sessionId) ?: return SessionFinalizationStatus.TARGET_MISSING
        if (existing.endTime != null) {
            return SessionFinalizationStatus.ALREADY_CLOSED
        }

        val closed = closeSingleOpenSessionWithConsistentMetrics(
            session = existing,
            fallbackEndTime = endTime,
            fallbackEndLevel = endLevel
        )
        if (!closed) {
            return SessionFinalizationStatus.OPEN_SESSIONS_REMAIN
        }

        val finalizedRow = getSessionById(sessionId) ?: return SessionFinalizationStatus.TARGET_MISSING
        val resolvedEndLevel = finalizedRow.endLevel ?: finalizedRow.startLevel
        val deltaPercent = resolvedEndLevel - finalizedRow.startLevel
        if (deltaPercent >= 40 &&
            finalizedRow.totalMahDelivered > 300.0 &&
            designCapacityMah > 0 &&
            countHealthLogsForSession(sessionId) == 0
        ) {
            val estimatedFullMah = ((finalizedRow.totalMahDelivered / deltaPercent.toDouble()) * 100.0).toInt()
            val healthPct = ((estimatedFullMah.toFloat() / designCapacityMah.toFloat()) * 100f).coerceIn(10f, 110f)
            insertHealthLog(
                BatteryHealthEntity(
                    timestamp = finalizedRow.endTime ?: endTime,
                    sessionId = sessionId,
                    estimatedCapacityMah = estimatedFullMah,
                    designCapacityMah = designCapacityMah,
                    healthPercentage = healthPct
                )
            )
        }
        return SessionFinalizationStatus.FINALIZED
    }

    @Transaction
    suspend fun persistSampleAndUpdateSessionAtomically(
        sample: BatterySampleEntity,
        totalMahDelivered: Double,
        avgCurrentMa: Int,
        maxTempCelsius: Float,
        hasAnomaly: Boolean,
        anomalyReason: String?,
        deepSleepRatio: Float
    ): SampleFlushStatus {
        val currentSession = getSessionById(sample.sessionId)
        if (currentSession == null || currentSession.endTime != null) {
            return SampleFlushStatus.SESSION_CLOSED_OR_MISSING
        }
        val alreadyPersisted = countSamplesAtTimestamp(sample.sessionId, sample.timestamp) > 0
        if (!alreadyPersisted) {
            insertSample(sample)
        }
        val mergedReason = listOfNotNull(currentSession.anomalyReason, anomalyReason)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .ifEmpty { null }

        val targetTotalMah = maxOf(currentSession.totalMahDelivered, totalMahDelivered)
        val targetMaxTemp = maxOf(currentSession.maxTempCelsius, maxTempCelsius)

        updateSession(
            currentSession.copy(
                totalMahDelivered = targetTotalMah,
                avgCurrentMa = if (avgCurrentMa != 0) avgCurrentMa else currentSession.avgCurrentMa,
                maxTempCelsius = targetMaxTemp,
                isAnomaly = currentSession.isAnomaly || hasAnomaly,
                anomalyReason = mergedReason,
                deepSleepRatio = deepSleepRatio
            )
        )

        val sampleVerified = countSamplesAtTimestamp(sample.sessionId, sample.timestamp) > 0
        val updatedSession = getSessionById(sample.sessionId)
        val sessionVerified = updatedSession != null &&
            updatedSession.totalMahDelivered + 0.0001 >= targetTotalMah &&
            updatedSession.maxTempCelsius + 0.001f >= targetMaxTemp

        if (!sampleVerified || !sessionVerified) {
            return SampleFlushStatus.VERIFICATION_FAILED
        }

        return if (alreadyPersisted) {
            SampleFlushStatus.ALREADY_FLUSHED_IDEMPOTENT
        } else {
            SampleFlushStatus.FLUSHED
        }
    }

    /**
     * Merekonsiliasi data recovery ke baris sesi di Room (baik terbuka maupun sudah tertutup)
     * secara atomik dan idempotent tanpa menggandakan sampel maupun metrik kumulatif,
     * serta memverifikasi keadaan akhir database sebelum mengembalikan true.
     */
    @Transaction
    suspend fun reconcileRecoveredSessionAtomically(
        sessionId: Long,
        recoveredTotalMah: Double,
        recoveredAvgCurrentMa: Int,
        recoveredMaxTempCelsius: Float,
        recoveredHasAnomaly: Boolean,
        recoveredAnomalyReason: String?,
        recoveredSample: BatterySampleEntity?
    ): Boolean {
        val existing = getSessionById(sessionId) ?: return false
        if (recoveredSample != null && recoveredSample.sessionId == sessionId) {
            if (countSamplesAtTimestamp(sessionId, recoveredSample.timestamp) == 0) {
                insertSample(recoveredSample)
            }
            if (countSamplesAtTimestamp(sessionId, recoveredSample.timestamp) == 0) {
                return false
            }
        }

        val mergedAnomaly = existing.isAnomaly || recoveredHasAnomaly
        val mergedReason = listOfNotNull(existing.anomalyReason, recoveredAnomalyReason)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .ifEmpty { null }

        val targetTotalMah = maxOf(existing.totalMahDelivered, recoveredTotalMah)
        val targetMaxTemp = maxOf(existing.maxTempCelsius, recoveredMaxTempCelsius)

        updateSession(
            existing.copy(
                totalMahDelivered = targetTotalMah,
                avgCurrentMa = if (recoveredAvgCurrentMa != 0) recoveredAvgCurrentMa else existing.avgCurrentMa,
                maxTempCelsius = targetMaxTemp,
                isAnomaly = mergedAnomaly,
                anomalyReason = mergedReason
            )
        )

        val verified = getSessionById(sessionId) ?: return false
        return verified.totalMahDelivered + 0.0001 >= targetTotalMah &&
            verified.maxTempCelsius + 0.001f >= targetMaxTemp &&
            (!mergedAnomaly || verified.isAnomaly)
    }

    /**
     * Memfinalisasi HANYA sesi target [sessionId] secara atomik dan idempotent.
     * Tidak pernah menutup sesi lain secara massal, dan tidak menyentuh sesi lain
     * apabila [sessionId] null atau tidak ditemukan di database.
     */
    @Transaction
    suspend fun finalizeSessionAtomically(
        sessionId: Long?,
        endTime: Long,
        endLevel: Int,
        totalMahDelivered: Double,
        avgCurrentMa: Int,
        maxTempCelsius: Float,
        hasAnomaly: Boolean,
        anomalyReason: String?,
        deepSleepRatio: Float,
        finalSample: BatterySampleEntity?,
        healthLog: BatteryHealthEntity?
    ): SessionFinalizationStatus {
        if (sessionId == null) {
            return SessionFinalizationStatus.TARGET_MISSING
        }

        val targetSession = getSessionById(sessionId) ?: return SessionFinalizationStatus.TARGET_MISSING

        if (targetSession.endTime != null) {
            return SessionFinalizationStatus.ALREADY_CLOSED
        }

        if (finalSample != null && finalSample.sessionId == sessionId) {
            if (countSamplesAtTimestamp(sessionId, finalSample.timestamp) == 0) {
                insertSample(finalSample)
            }
            if (countSamplesAtTimestamp(sessionId, finalSample.timestamp) == 0) {
                return SessionFinalizationStatus.OPEN_SESSIONS_REMAIN
            }
        }

        val mergedReason = listOfNotNull(targetSession.anomalyReason, anomalyReason)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .ifEmpty { null }

        val resolvedEndTime = maxOf(endTime, targetSession.startTime)
        val resolvedEndLevel = if (endLevel in 0..100) endLevel else targetSession.startLevel
        val targetTotalMah = maxOf(targetSession.totalMahDelivered, totalMahDelivered)
        val targetMaxTemp = maxOf(targetSession.maxTempCelsius, maxTempCelsius)

        updateSession(
            targetSession.copy(
                endTime = resolvedEndTime,
                endLevel = resolvedEndLevel,
                totalMahDelivered = targetTotalMah,
                avgCurrentMa = if (avgCurrentMa != 0) avgCurrentMa else targetSession.avgCurrentMa,
                maxTempCelsius = targetMaxTemp,
                isAnomaly = targetSession.isAnomaly || hasAnomaly,
                anomalyReason = mergedReason,
                deepSleepRatio = deepSleepRatio
            )
        )

        if (healthLog != null &&
            healthLog.sessionId == sessionId &&
            countHealthLogsForSession(sessionId) == 0
        ) {
            insertHealthLog(healthLog)
        }

        val updatedTarget = getSessionById(sessionId)
        return if (updatedTarget != null &&
            updatedTarget.endTime != null &&
            updatedTarget.totalMahDelivered + 0.0001 >= targetTotalMah
        ) {
            SessionFinalizationStatus.FINALIZED
        } else {
            SessionFinalizationStatus.OPEN_SESSIONS_REMAIN
        }
    }

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<ChargingSessionEntity>>

    @Insert
    suspend fun insertSample(sample: BatterySampleEntity)

    @Query("SELECT * FROM battery_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSamplesForSessionFlow(sessionId: Long): Flow<List<BatterySampleEntity>>

    @Query("DELETE FROM battery_samples WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldSamples(cutoffTimestamp: Long)

    @Insert
    suspend fun insertHealthLog(health: BatteryHealthEntity)

    @Query("SELECT * FROM battery_health_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestHealthFlow(): Flow<BatteryHealthEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyRollup(rollup: DailyRollupEntity)

    @Query("SELECT * FROM daily_rollups ORDER BY dateKey DESC LIMIT 30")
    fun getRecentRollupsFlow(): Flow<List<DailyRollupEntity>>
}
