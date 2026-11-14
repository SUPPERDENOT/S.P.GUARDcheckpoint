package com.superdenot.powerguard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.domain.model.LiveBatteryState

/**
 * Penyimpanan checkpoint recovery persisten multi-sesi (keyed by sessionId) lintas restart
 * service maupun restart proses.
 *
 * Jaminan keamanan P0/P1:
 * 1. Isolasi ketat per sessionId (`s_<sessionId>_*`): setiap sesi memiliki namespace kunci
 *    tersendiri sehingga satu sesi yang belum terselesaikan atau berstatus follow-up tidak
 *    pernah menimpa, menghapus, atau memblokir checkpoint milik sesi lain.
 * 2. Kepemilikan eksplisit (`saveForSession` & `clear(context, expectedSessionId)`): operasi
 *    simpan/ubah/hapus wajib cocok dengan sessionId pemilik dan diverifikasi melalui read-back
 *    setelah `commit()` sinkron.
 * 3. Klasifikasi status penyimpanan eksplisit: EMPTY, LOADED, UNAVAILABLE, CORRUPT.
 *    Exception penyimpanan atau data korup tidak pernah dianggap sebagai EMPTY.
 * 4. Penggabungan monolitik (`mergeSameSessionRecovery`) mempertahankan bobot sampel tertimbang
 *    (`sampleWeight`), rentang durasi batch, serta metrik kumulatif tanpa kehilangan data.
 */
object SessionRecoveryStore {

    enum class RecoveryStatus {
        PENDING_RECONCILIATION,
        NEEDS_FOLLOW_UP_TARGET_MISSING,
        NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
        NEEDS_FOLLOW_UP_FALLBACK_FAILED
    }

    enum class LoadStatus {
        EMPTY,
        LOADED,
        UNAVAILABLE,
        CORRUPT
    }

    sealed class CheckpointLoadResult {
        abstract val status: LoadStatus

        data object Empty : CheckpointLoadResult() {
            override val status: LoadStatus = LoadStatus.EMPTY
        }

        data class Loaded(val recovery: PersistedSessionRecovery) : CheckpointLoadResult() {
            override val status: LoadStatus = LoadStatus.LOADED
        }

        data class Unavailable(
            val reason: String,
            val cause: Throwable? = null
        ) : CheckpointLoadResult() {
            override val status: LoadStatus = LoadStatus.UNAVAILABLE
        }

        data class Corrupt(
            val reason: String,
            val rawSessionId: Long? = null
        ) : CheckpointLoadResult() {
            override val status: LoadStatus = LoadStatus.CORRUPT
        }

        fun getOrNull(): PersistedSessionRecovery? = (this as? Loaded)?.recovery
    }

    enum class SaveStatus {
        SAVED,
        REJECTED_OWNERSHIP_CONFLICT,
        REJECTED_EXISTING_CORRUPT,
        REJECTED_STORAGE_UNAVAILABLE,
        INVALID_RECOVERY_PAYLOAD,
        COMMIT_FAILED,
        VERIFICATION_FAILED
    }

    data class CheckpointSaveResult(
        val status: SaveStatus,
        val existingOwnerSessionId: Long? = null,
        val message: String = ""
    ) {
        val isSuccess: Boolean
            get() = status == SaveStatus.SAVED
    }

    private const val PREFS_NAME = "powerguard_session_recovery"
    private const val KEY_INDEX_SESSION_IDS = "tracked_session_ids"

    // Prefix kunci per sesi: s_<sessionId>_<field>
    private const val SESSION_KEY_PREFIX = "s_"
    private const val FIELD_HAS_RECORD = "has_record"
    private const val FIELD_SESSION_ID = "session_id"
    private const val FIELD_ACCUMULATED_MAH = "accumulated_mah"
    private const val FIELD_SESSION_SECONDS = "session_seconds"
    private const val FIELD_MAX_TEMP = "max_temp"
    private const val FIELD_CURRENT_SUM = "current_sum"
    private const val FIELD_CURRENT_COUNT = "current_count"
    private const val FIELD_ANOMALY_CODES = "anomaly_codes"
    private const val FIELD_PENDING_BATCH_TS = "pending_batch_ts"
    private const val FIELD_HAS_PENDING_SAMPLE = "has_pending_sample"
    private const val FIELD_SAMPLE_LEVEL = "sample_level"
    private const val FIELD_SAMPLE_CURRENT_MA = "sample_current_ma"
    private const val FIELD_SAMPLE_VOLTAGE_MV = "sample_voltage_mv"
    private const val FIELD_SAMPLE_TEMP = "sample_temp"
    private const val FIELD_SAMPLE_WATT = "sample_watt"
    private const val FIELD_SAMPLE_SCREEN_ON = "sample_screen_on"
    private const val FIELD_SAMPLE_WEIGHT = "sample_weight"
    private const val FIELD_SAMPLE_SPAN_MS = "sample_span_ms"
    private const val FIELD_DB_SESSIONS_CLOSED = "db_sessions_closed"
    private const val FIELD_FAILURE_REASON = "failure_reason"
    private const val FIELD_PERSISTED_AT_MS = "persisted_at_ms"
    private const val FIELD_RECOVERY_STATUS = "recovery_status"

    // Kunci legacy unscoped (tetap dideteksi untuk migrasi aman / deteksi korup)
    private const val LEGACY_KEY_HAS_RECORD = "has_record"
    private const val LEGACY_KEY_SESSION_ID = "session_id"

    private val storeLock = Any()

    private fun sessionPrefix(sessionId: Long): String = "$SESSION_KEY_PREFIX${sessionId}_"
    private fun sessionKey(sessionId: Long, field: String): String = "${sessionPrefix(sessionId)}$field"

    interface PersistenceDelegate {
        fun saveDetailed(context: Context, recovery: PersistedSessionRecovery): CheckpointSaveResult {
            return if (save(context, recovery)) {
                CheckpointSaveResult(SaveStatus.SAVED, message = "Saved via delegate")
            } else {
                CheckpointSaveResult(SaveStatus.COMMIT_FAILED, message = "Delegate save returned false")
            }
        }

        fun save(context: Context, recovery: PersistedSessionRecovery): Boolean =
            saveDetailed(context, recovery).isSuccess

        fun loadDetailed(context: Context): CheckpointLoadResult {
            val legacy = load(context)
            return if (legacy != null) {
                CheckpointLoadResult.Loaded(legacy)
            } else {
                CheckpointLoadResult.Empty
            }
        }

        fun loadForSessionDetailed(context: Context, sessionId: Long): CheckpointLoadResult {
            if (sessionId <= 0L) {
                return CheckpointLoadResult.Corrupt("sessionId tidak valid ($sessionId)", sessionId)
            }
            return when (val single = loadDetailed(context)) {
                is CheckpointLoadResult.Loaded -> {
                    if (single.recovery.sessionId == sessionId) single else CheckpointLoadResult.Empty
                }
                is CheckpointLoadResult.Empty -> CheckpointLoadResult.Empty
                is CheckpointLoadResult.Unavailable -> single
                is CheckpointLoadResult.Corrupt -> {
                    if (single.rawSessionId == null || single.rawSessionId == sessionId) single
                    else CheckpointLoadResult.Empty
                }
            }
        }

        fun loadAllDetailed(context: Context): List<CheckpointLoadResult> {
            return when (val single = loadDetailed(context)) {
                is CheckpointLoadResult.Empty -> emptyList()
                else -> listOf(single)
            }
        }

        fun load(context: Context): PersistedSessionRecovery? = loadDetailed(context).getOrNull()

        fun clear(context: Context, expectedSessionId: Long): Boolean

        fun clearAllForTesting(context: Context): Boolean = false
    }

    private fun validatePayload(recovery: PersistedSessionRecovery): String? {
        if (recovery.sessionId <= 0L) {
            return "sessionId tidak valid (${recovery.sessionId})"
        }
        if (!recovery.accumulatedMah.isFinite() || recovery.accumulatedMah < 0.0) {
            return "accumulatedMah tidak valid (${recovery.accumulatedMah})"
        }
        if (recovery.sessionSeconds < 0L) {
            return "sessionSeconds negatif (${recovery.sessionSeconds})"
        }
        if (!recovery.maxSessionTemp.isFinite() || recovery.maxSessionTemp < 0f) {
            return "maxSessionTemp tidak valid (${recovery.maxSessionTemp})"
        }
        if (recovery.currentReadingsCount < 0L) {
            return "currentReadingsCount negatif (${recovery.currentReadingsCount})"
        }
        if (recovery.currentReadingsCount == 0L && recovery.currentSumForAvg != 0L) {
            return "currentSumForAvg (${recovery.currentSumForAvg}) tidak konsisten dengan currentReadingsCount=0"
        }
        if (recovery.sampleWeight < 1) {
            return "sampleWeight tidak valid (${recovery.sampleWeight})"
        }
        if (recovery.sampleSpanDurationMs < 0L) {
            return "sampleSpanDurationMs negatif (${recovery.sampleSpanDurationMs})"
        }
        if (recovery.hasPendingSample) {
            if (recovery.pendingBatchTimestampMs <= 0L) {
                return "pendingBatchTimestampMs wajib > 0 saat hasPendingSample=true"
            }
            if (recovery.sampleBatteryLevel !in 0..100) {
                return "sampleBatteryLevel di luar rentang 0..100 (${recovery.sampleBatteryLevel})"
            }
            if (!recovery.sampleTempCelsius.isFinite() || !recovery.samplePowerWatt.isFinite()) {
                return "sampleTempCelsius/samplePowerWatt tidak finite"
            }
        }
        return null
    }

    private fun mergeSameSessionRecovery(
        existing: PersistedSessionRecovery,
        incoming: PersistedSessionRecovery
    ): PersistedSessionRecovery {
        val mergedCount = maxOf(existing.currentReadingsCount, incoming.currentReadingsCount)
        // Jumlah pembacaan yang sama tidak membuktikan snapshot incoming lebih baru.
        // Pertahankan sum tersimpan pada count yang sama agar retry/stale snapshot tidak
        // mengganti statistik dengan nilai yang tidak konsisten.
        val incomingCurrentSnapshotIsNewer =
            incoming.persistedAtMs > existing.persistedAtMs
        val mergedSum = when {
            incoming.currentReadingsCount > existing.currentReadingsCount && incoming.currentReadingsCount > 0L ->
                incoming.currentSumForAvg
            incoming.currentReadingsCount == existing.currentReadingsCount &&
                incoming.currentReadingsCount > 0L && incomingCurrentSnapshotIsNewer ->
                incoming.currentSumForAvg
            existing.currentReadingsCount > 0L ->
                existing.currentSumForAvg
            else -> 0L
        }

        val mergedPendingTs: Long
        val mergedHasSample: Boolean
        val mergedLevel: Int
        val mergedCurrentMa: Int
        val mergedVoltageMv: Int
        val mergedTemp: Float
        val mergedPower: Float
        val mergedScreenOn: Boolean
        val mergedWeight: Int
        val mergedSpanMs: Long

        when {
            !incoming.hasPendingSample && existing.hasPendingSample -> {
                mergedPendingTs = existing.pendingBatchTimestampMs
                mergedHasSample = true
                mergedLevel = existing.sampleBatteryLevel
                mergedCurrentMa = existing.sampleCurrentMa
                mergedVoltageMv = existing.sampleVoltageMv
                mergedTemp = existing.sampleTempCelsius
                mergedPower = existing.samplePowerWatt
                mergedScreenOn = existing.sampleIsScreenOn
                mergedWeight = existing.sampleWeight.coerceAtLeast(1)
                mergedSpanMs = existing.sampleSpanDurationMs.coerceAtLeast(0L)
            }
            incoming.hasPendingSample && !existing.hasPendingSample -> {
                mergedPendingTs = incoming.pendingBatchTimestampMs
                mergedHasSample = true
                mergedLevel = incoming.sampleBatteryLevel
                mergedCurrentMa = incoming.sampleCurrentMa
                mergedVoltageMv = incoming.sampleVoltageMv
                mergedTemp = incoming.sampleTempCelsius
                mergedPower = incoming.samplePowerWatt
                mergedScreenOn = incoming.sampleIsScreenOn
                mergedWeight = incoming.sampleWeight.coerceAtLeast(1)
                mergedSpanMs = incoming.sampleSpanDurationMs.coerceAtLeast(0L)
            }
            incoming.hasPendingSample && existing.hasPendingSample -> {
                if (existing.pendingBatchTimestampMs == incoming.pendingBatchTimestampMs) {
                    // Bobot sama bukan bukti payload incoming lebih baru; pertahankan checkpoint existing.
                    val primary = if (
                        incoming.sampleWeight > existing.sampleWeight ||
                        (incoming.sampleWeight == existing.sampleWeight && incomingCurrentSnapshotIsNewer)
                    ) incoming else existing
                    mergedPendingTs = primary.pendingBatchTimestampMs
                    mergedHasSample = true
                    mergedLevel = primary.sampleBatteryLevel
                    mergedCurrentMa = primary.sampleCurrentMa
                    mergedVoltageMv = primary.sampleVoltageMv
                    mergedTemp = primary.sampleTempCelsius
                    mergedPower = primary.samplePowerWatt
                    mergedScreenOn = primary.sampleIsScreenOn
                    mergedWeight = maxOf(existing.sampleWeight, incoming.sampleWeight).coerceAtLeast(1)
                    mergedSpanMs = maxOf(existing.sampleSpanDurationMs, incoming.sampleSpanDurationMs).coerceAtLeast(0L)
                } else {
                    // Gabungkan dua batch tertunda berbeda milik sesi yang sama secara tertimbang agar tidak ada sampel hilang
                    val w1 = existing.sampleWeight.coerceAtLeast(1).toLong()
                    val w2 = incoming.sampleWeight.coerceAtLeast(1).toLong()
                    val totalW = w1 + w2
                    val later = if (incoming.pendingBatchTimestampMs >= existing.pendingBatchTimestampMs) incoming else existing
                    val earliestStart = minOf(existing.pendingBatchTimestampMs, incoming.pendingBatchTimestampMs)
                    val latestEnd = maxOf(
                        existing.pendingBatchTimestampMs + existing.sampleSpanDurationMs.coerceAtLeast(0L),
                        incoming.pendingBatchTimestampMs + incoming.sampleSpanDurationMs.coerceAtLeast(0L)
                    )

                    mergedPendingTs = earliestStart
                    mergedHasSample = true
                    mergedLevel = later.sampleBatteryLevel
                    mergedCurrentMa = ((existing.sampleCurrentMa.toLong() * w1 + incoming.sampleCurrentMa.toLong() * w2) / totalW).toInt()
                    mergedVoltageMv = ((existing.sampleVoltageMv.toLong() * w1 + incoming.sampleVoltageMv.toLong() * w2) / totalW).toInt()
                    mergedTemp = ((existing.sampleTempCelsius.toDouble() * w1 + incoming.sampleTempCelsius.toDouble() * w2) / totalW).toFloat()
                    mergedPower = ((existing.samplePowerWatt.toDouble() * w1 + incoming.samplePowerWatt.toDouble() * w2) / totalW).toFloat()
                    mergedScreenOn = later.sampleIsScreenOn
                    mergedWeight = totalW.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    mergedSpanMs = (latestEnd - earliestStart).coerceAtLeast(0L)
                }
            }
            else -> {
                mergedPendingTs = incoming.pendingBatchTimestampMs
                mergedHasSample = false
                mergedLevel = incoming.sampleBatteryLevel
                mergedCurrentMa = incoming.sampleCurrentMa
                mergedVoltageMv = incoming.sampleVoltageMv
                mergedTemp = incoming.sampleTempCelsius
                mergedPower = incoming.samplePowerWatt
                mergedScreenOn = incoming.sampleIsScreenOn
                mergedWeight = 1
                mergedSpanMs = 0L
            }
        }

        return incoming.copy(
            accumulatedMah = maxOf(existing.accumulatedMah, incoming.accumulatedMah),
            sessionSeconds = maxOf(existing.sessionSeconds, incoming.sessionSeconds),
            maxSessionTemp = maxOf(existing.maxSessionTemp, incoming.maxSessionTemp),
            currentSumForAvg = mergedSum,
            currentReadingsCount = mergedCount,
            alertedAnomalyCodes = existing.alertedAnomalyCodes + incoming.alertedAnomalyCodes,
            pendingBatchTimestampMs = mergedPendingTs,
            hasPendingSample = mergedHasSample,
            sampleBatteryLevel = mergedLevel,
            sampleCurrentMa = mergedCurrentMa,
            sampleVoltageMv = mergedVoltageMv,
            sampleTempCelsius = mergedTemp,
            samplePowerWatt = mergedPower,
            sampleIsScreenOn = mergedScreenOn,
            sampleWeight = mergedWeight,
            sampleSpanDurationMs = mergedSpanMs,
            dbSessionsClosed = existing.dbSessionsClosed || incoming.dbSessionsClosed
        )
    }

    private fun discoverTrackedAndScopedSessionIds(allEntries: Map<String, *>): Pair<Set<Long>, List<CheckpointLoadResult.Corrupt>> {
        val validIds = linkedSetOf<Long>()
        val corruptList = mutableListOf<CheckpointLoadResult.Corrupt>()

        val rawIndex = allEntries[KEY_INDEX_SESSION_IDS]
        if (rawIndex != null) {
            if (rawIndex !is Set<*>) {
                corruptList.add(
                    CheckpointLoadResult.Corrupt(
                        reason = "Indeks tracked_session_ids korup (${rawIndex::class.java.simpleName})",
                        rawSessionId = null
                    )
                )
            } else {
                for (item in rawIndex) {
                    val str = item as? String
                    val parsedId = str?.toLongOrNull()
                    if (parsedId == null || parsedId <= 0L) {
                        corruptList.add(
                            CheckpointLoadResult.Corrupt(
                                reason = "Entri sessionId pada indeks tidak valid ($item)",
                                rawSessionId = parsedId
                            )
                        )
                    } else {
                        validIds.add(parsedId)
                    }
                }
            }
        }

        for (key in allEntries.keys) {
            if (key.startsWith(SESSION_KEY_PREFIX)) {
                val remainder = key.removePrefix(SESSION_KEY_PREFIX)
                val idToken = remainder.substringBefore('_', missingDelimiterValue = "")
                val parsedId = idToken.toLongOrNull()
                if (parsedId == null || parsedId <= 0L) {
                    corruptList.add(
                        CheckpointLoadResult.Corrupt(
                            reason = "Prefix kunci sesi korup ($key)",
                            rawSessionId = parsedId
                        )
                    )
                } else {
                    validIds.add(parsedId)
                }
            }
        }

        return validIds to corruptList
    }

    private fun readLegacyUnscopedBucket(allEntries: Map<String, *>): CheckpointLoadResult? {
        val legacyKeys = allEntries.keys.filter { key ->
            key != KEY_INDEX_SESSION_IDS && !key.startsWith(SESSION_KEY_PREFIX)
        }
        if (legacyKeys.isEmpty()) return null

        val hasRecordRaw = allEntries[LEGACY_KEY_HAS_RECORD]
        if (hasRecordRaw == null) {
            val danglingId = (allEntries[LEGACY_KEY_SESSION_ID] as? Number)?.toLong()
            return CheckpointLoadResult.Corrupt(
                reason = "Ditemukan kunci checkpoint legacy parsial tanpa has_record: $legacyKeys",
                rawSessionId = danglingId
            )
        }
        if (hasRecordRaw !is Boolean) {
            return CheckpointLoadResult.Corrupt(
                reason = "Tipe has_record legacy korup (${hasRecordRaw::class.java.simpleName})",
                rawSessionId = (allEntries[LEGACY_KEY_SESSION_ID] as? Number)?.toLong()
            )
        }
        if (!hasRecordRaw) {
            val residual = legacyKeys.filter { it != LEGACY_KEY_HAS_RECORD }
            return if (residual.isEmpty()) {
                null
            } else {
                CheckpointLoadResult.Corrupt(
                    reason = "has_record=false tetapi masih ada sisa kunci legacy: $residual",
                    rawSessionId = (allEntries[LEGACY_KEY_SESSION_ID] as? Number)?.toLong()
                )
            }
        }

        val sessionId = (allEntries[LEGACY_KEY_SESSION_ID] as? Long)
            ?: return CheckpointLoadResult.Corrupt("session_id legacy hilang atau bukan Long")
        if (sessionId <= 0L) {
            return CheckpointLoadResult.Corrupt(
                reason = "session_id legacy tidak valid ($sessionId)",
                rawSessionId = sessionId
            )
        }
        return parseBucketFields(allEntries, prefix = "", expectedSessionId = sessionId)
    }

    private fun readSessionBucketFromEntries(
        allEntries: Map<String, *>,
        sessionId: Long
    ): CheckpointLoadResult {
        if (sessionId <= 0L) {
            return CheckpointLoadResult.Corrupt("sessionId tidak valid ($sessionId)", sessionId)
        }
        val prefix = sessionPrefix(sessionId)
        val scopedKeys = allEntries.keys.filter { it.startsWith(prefix) }
        val indexed = (allEntries[KEY_INDEX_SESSION_IDS] as? Set<*>)?.contains(sessionId.toString()) == true

        if (scopedKeys.isEmpty() && !indexed) {
            return CheckpointLoadResult.Empty
        }

        val hasRecordRaw = allEntries[sessionKey(sessionId, FIELD_HAS_RECORD)]
        if (hasRecordRaw == null) {
            return CheckpointLoadResult.Corrupt(
                reason = "Bucket sessionId=$sessionId tidak memiliki has_record (indexed=$indexed, keys=$scopedKeys)",
                rawSessionId = sessionId
            )
        }
        if (hasRecordRaw !is Boolean) {
            return CheckpointLoadResult.Corrupt(
                reason = "Tipe has_record sessionId=$sessionId korup (${hasRecordRaw::class.java.simpleName})",
                rawSessionId = sessionId
            )
        }
        if (!hasRecordRaw) {
            val residual = scopedKeys.filter { it != sessionKey(sessionId, FIELD_HAS_RECORD) }
            return if (residual.isEmpty() && !indexed) {
                CheckpointLoadResult.Empty
            } else {
                CheckpointLoadResult.Corrupt(
                    reason = "has_record=false pada sessionId=$sessionId tetapi masih ada sisa metadata: $residual",
                    rawSessionId = sessionId
                )
            }
        }

        val storedSessionId = (allEntries[sessionKey(sessionId, FIELD_SESSION_ID)] as? Long)
            ?: return CheckpointLoadResult.Corrupt(
                reason = "session_id hilang atau bukan Long pada bucket sessionId=$sessionId",
                rawSessionId = sessionId
            )
        if (storedSessionId != sessionId) {
            return CheckpointLoadResult.Corrupt(
                reason = "Konflik kepemilikan internal bucket: key sessionId=$sessionId tetapi stored sessionId=$storedSessionId",
                rawSessionId = storedSessionId
            )
        }

        return parseBucketFields(allEntries, prefix = prefix, expectedSessionId = sessionId)
    }

    private fun parseBucketFields(
        allEntries: Map<String, *>,
        prefix: String,
        expectedSessionId: Long
    ): CheckpointLoadResult {
        fun k(field: String) = "$prefix$field"

        val mahBits = (allEntries[k(FIELD_ACCUMULATED_MAH)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("accumulated_mah hilang atau korup", expectedSessionId)
        val accumulatedMah = java.lang.Double.longBitsToDouble(mahBits)
        if (!accumulatedMah.isFinite() || accumulatedMah < 0.0) {
            return CheckpointLoadResult.Corrupt("accumulated_mah tidak finite atau negatif ($accumulatedMah)", expectedSessionId)
        }

        val sessionSeconds = (allEntries[k(FIELD_SESSION_SECONDS)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("session_seconds hilang atau korup", expectedSessionId)
        if (sessionSeconds < 0L) {
            return CheckpointLoadResult.Corrupt("session_seconds negatif ($sessionSeconds)", expectedSessionId)
        }

        val maxSessionTemp = (allEntries[k(FIELD_MAX_TEMP)] as? Float)
            ?: return CheckpointLoadResult.Corrupt("max_temp hilang atau korup", expectedSessionId)
        if (!maxSessionTemp.isFinite() || maxSessionTemp < 0f) {
            return CheckpointLoadResult.Corrupt("max_temp tidak valid ($maxSessionTemp)", expectedSessionId)
        }

        val currentSumForAvg = (allEntries[k(FIELD_CURRENT_SUM)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("current_sum hilang atau korup", expectedSessionId)
        val currentReadingsCount = (allEntries[k(FIELD_CURRENT_COUNT)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("current_count hilang atau korup", expectedSessionId)
        if (currentReadingsCount < 0L || (currentReadingsCount == 0L && currentSumForAvg != 0L)) {
            return CheckpointLoadResult.Corrupt(
                "Statistik arus tidak konsisten (sum=$currentSumForAvg, count=$currentReadingsCount)",
                expectedSessionId
            )
        }

        val rawAnomalySet = allEntries[k(FIELD_ANOMALY_CODES)]
        val anomalyCodes = when (rawAnomalySet) {
            null -> emptySet()
            is Set<*> -> rawAnomalySet.filterIsInstance<String>().toSet()
            else -> return CheckpointLoadResult.Corrupt("anomaly_codes bukan Set<String>", expectedSessionId)
        }

        val hasPendingSample = (allEntries[k(FIELD_HAS_PENDING_SAMPLE)] as? Boolean)
            ?: return CheckpointLoadResult.Corrupt("has_pending_sample hilang atau korup", expectedSessionId)
        val pendingBatchTs = (allEntries[k(FIELD_PENDING_BATCH_TS)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("pending_batch_ts hilang atau korup", expectedSessionId)
        val sampleLevel = (allEntries[k(FIELD_SAMPLE_LEVEL)] as? Int)
            ?: return CheckpointLoadResult.Corrupt("sample_level hilang atau korup", expectedSessionId)
        val sampleCurrentMa = (allEntries[k(FIELD_SAMPLE_CURRENT_MA)] as? Int)
            ?: return CheckpointLoadResult.Corrupt("sample_current_ma hilang atau korup", expectedSessionId)
        val sampleVoltageMv = (allEntries[k(FIELD_SAMPLE_VOLTAGE_MV)] as? Int)
            ?: return CheckpointLoadResult.Corrupt("sample_voltage_mv hilang atau korup", expectedSessionId)
        val sampleTemp = (allEntries[k(FIELD_SAMPLE_TEMP)] as? Float)
            ?: return CheckpointLoadResult.Corrupt("sample_temp hilang atau korup", expectedSessionId)
        val sampleWatt = (allEntries[k(FIELD_SAMPLE_WATT)] as? Float)
            ?: return CheckpointLoadResult.Corrupt("sample_watt hilang atau korup", expectedSessionId)
        val sampleScreenOn = (allEntries[k(FIELD_SAMPLE_SCREEN_ON)] as? Boolean)
            ?: return CheckpointLoadResult.Corrupt("sample_screen_on hilang atau korup", expectedSessionId)

        val rawSampleWeight = allEntries[k(FIELD_SAMPLE_WEIGHT)]
        val sampleWeight = when (rawSampleWeight) {
            null -> 1
            is Int -> rawSampleWeight
            else -> return CheckpointLoadResult.Corrupt("sample_weight bukan Int", expectedSessionId)
        }
        if (sampleWeight < 1) {
            return CheckpointLoadResult.Corrupt("sample_weight tidak valid ($sampleWeight)", expectedSessionId)
        }

        val rawSampleSpanMs = allEntries[k(FIELD_SAMPLE_SPAN_MS)]
        val sampleSpanMs = when (rawSampleSpanMs) {
            null -> 0L
            is Long -> rawSampleSpanMs
            else -> return CheckpointLoadResult.Corrupt("sample_span_ms bukan Long", expectedSessionId)
        }
        if (sampleSpanMs < 0L) {
            return CheckpointLoadResult.Corrupt("sample_span_ms negatif ($sampleSpanMs)", expectedSessionId)
        }

        if (hasPendingSample) {
            if (pendingBatchTs <= 0L || sampleLevel !in 0..100 || !sampleTemp.isFinite() || !sampleWatt.isFinite()) {
                return CheckpointLoadResult.Corrupt(
                    "Metadata sampel tertunda korup (ts=$pendingBatchTs, level=$sampleLevel, temp=$sampleTemp, watt=$sampleWatt)",
                    expectedSessionId
                )
            }
        }

        val dbSessionsClosed = (allEntries[k(FIELD_DB_SESSIONS_CLOSED)] as? Boolean)
            ?: return CheckpointLoadResult.Corrupt("db_sessions_closed hilang atau korup", expectedSessionId)
        val failureReason = (allEntries[k(FIELD_FAILURE_REASON)] as? String)
            ?: return CheckpointLoadResult.Corrupt("failure_reason hilang atau korup", expectedSessionId)
        val persistedAtMs = (allEntries[k(FIELD_PERSISTED_AT_MS)] as? Long)
            ?: return CheckpointLoadResult.Corrupt("persisted_at_ms hilang atau korup", expectedSessionId)
        if (persistedAtMs <= 0L) {
            return CheckpointLoadResult.Corrupt("persisted_at_ms tidak valid ($persistedAtMs)", expectedSessionId)
        }

        val rawStatus = (allEntries[k(FIELD_RECOVERY_STATUS)] as? String)
            ?: return CheckpointLoadResult.Corrupt("recovery_status hilang atau korup", expectedSessionId)
        val parsedStatus = RecoveryStatus.entries.firstOrNull { it.name == rawStatus }
            ?: return CheckpointLoadResult.Corrupt("recovery_status tidak dikenal ($rawStatus)", expectedSessionId)

        return CheckpointLoadResult.Loaded(
            PersistedSessionRecovery(
                sessionId = expectedSessionId,
                accumulatedMah = accumulatedMah,
                sessionSeconds = sessionSeconds,
                maxSessionTemp = maxSessionTemp,
                currentSumForAvg = currentSumForAvg,
                currentReadingsCount = currentReadingsCount,
                alertedAnomalyCodes = anomalyCodes,
                pendingBatchTimestampMs = pendingBatchTs,
                hasPendingSample = hasPendingSample,
                sampleBatteryLevel = sampleLevel,
                sampleCurrentMa = sampleCurrentMa,
                sampleVoltageMv = sampleVoltageMv,
                sampleTempCelsius = sampleTemp,
                samplePowerWatt = sampleWatt,
                sampleIsScreenOn = sampleScreenOn,
                sampleWeight = sampleWeight,
                sampleSpanDurationMs = sampleSpanMs,
                dbSessionsClosed = dbSessionsClosed,
                failureReason = failureReason,
                persistedAtMs = persistedAtMs,
                recoveryStatus = parsedStatus
            )
        )
    }

    private fun readAllFromPrefs(prefs: SharedPreferences): List<CheckpointLoadResult> {
        return try {
            val allEntries = prefs.all
            if (allEntries.isEmpty()) {
                return emptyList()
            }
            val results = mutableListOf<CheckpointLoadResult>()
            val (sessionIds, indexCorrupts) = discoverTrackedAndScopedSessionIds(allEntries)
            results.addAll(indexCorrupts)

            for (sessionId in sessionIds) {
                when (val bucketResult = readSessionBucketFromEntries(allEntries, sessionId)) {
                    is CheckpointLoadResult.Empty -> Unit
                    else -> results.add(bucketResult)
                }
            }

            readLegacyUnscopedBucket(allEntries)?.let { legacyResult ->
                results.add(legacyResult)
            }

            results
        } catch (t: Throwable) {
            listOf(
                CheckpointLoadResult.Unavailable(
                    reason = "Gagal membaca SharedPreferences recovery: ${t.message}",
                    cause = t
                )
            )
        }
    }

    private fun writeSessionRecordToPrefs(
        prefs: SharedPreferences,
        allExistingEntries: Map<String, *>,
        record: PersistedSessionRecovery
    ): Boolean {
        val persistedAt = if (record.persistedAtMs > 0L) record.persistedAtMs else System.currentTimeMillis()
        val existingIndex = (allExistingEntries[KEY_INDEX_SESSION_IDS] as? Set<*>)
            ?.filterIsInstance<String>()
            ?.toMutableSet()
            ?: mutableSetOf()
        existingIndex.add(record.sessionId.toString())

        val sid = record.sessionId
        return prefs.edit()
            .putStringSet(KEY_INDEX_SESSION_IDS, existingIndex)
            .putBoolean(sessionKey(sid, FIELD_HAS_RECORD), true)
            .putLong(sessionKey(sid, FIELD_SESSION_ID), sid)
            .putLong(sessionKey(sid, FIELD_ACCUMULATED_MAH), java.lang.Double.doubleToRawLongBits(record.accumulatedMah))
            .putLong(sessionKey(sid, FIELD_SESSION_SECONDS), record.sessionSeconds)
            .putFloat(sessionKey(sid, FIELD_MAX_TEMP), record.maxSessionTemp)
            .putLong(sessionKey(sid, FIELD_CURRENT_SUM), record.currentSumForAvg)
            .putLong(sessionKey(sid, FIELD_CURRENT_COUNT), record.currentReadingsCount)
            .putStringSet(sessionKey(sid, FIELD_ANOMALY_CODES), record.alertedAnomalyCodes.toSet())
            .putLong(sessionKey(sid, FIELD_PENDING_BATCH_TS), record.pendingBatchTimestampMs)
            .putBoolean(sessionKey(sid, FIELD_HAS_PENDING_SAMPLE), record.hasPendingSample)
            .putInt(sessionKey(sid, FIELD_SAMPLE_LEVEL), record.sampleBatteryLevel)
            .putInt(sessionKey(sid, FIELD_SAMPLE_CURRENT_MA), record.sampleCurrentMa)
            .putInt(sessionKey(sid, FIELD_SAMPLE_VOLTAGE_MV), record.sampleVoltageMv)
            .putFloat(sessionKey(sid, FIELD_SAMPLE_TEMP), record.sampleTempCelsius)
            .putFloat(sessionKey(sid, FIELD_SAMPLE_WATT), record.samplePowerWatt)
            .putBoolean(sessionKey(sid, FIELD_SAMPLE_SCREEN_ON), record.sampleIsScreenOn)
            .putInt(sessionKey(sid, FIELD_SAMPLE_WEIGHT), record.sampleWeight.coerceAtLeast(1))
            .putLong(sessionKey(sid, FIELD_SAMPLE_SPAN_MS), record.sampleSpanDurationMs.coerceAtLeast(0L))
            .putBoolean(sessionKey(sid, FIELD_DB_SESSIONS_CLOSED), record.dbSessionsClosed)
            .putString(sessionKey(sid, FIELD_FAILURE_REASON), record.failureReason)
            .putLong(sessionKey(sid, FIELD_PERSISTED_AT_MS), persistedAt)
            .putString(sessionKey(sid, FIELD_RECOVERY_STATUS), record.recoveryStatus.name)
            .commit()
    }

    private fun removeSessionKeysFromPrefs(
        prefs: SharedPreferences,
        allExistingEntries: Map<String, *>,
        sessionId: Long
    ): Boolean {
        val prefix = sessionPrefix(sessionId)
        val keysToRemove = allExistingEntries.keys.filter { it.startsWith(prefix) }
        val updatedIndex = (allExistingEntries[KEY_INDEX_SESSION_IDS] as? Set<*>)
            ?.filterIsInstance<String>()
            ?.filter { it != sessionId.toString() }
            ?.toSet()
            ?: emptySet()

        val editor = prefs.edit()
        if (updatedIndex.isEmpty()) {
            editor.remove(KEY_INDEX_SESSION_IDS)
        } else {
            editor.putStringSet(KEY_INDEX_SESSION_IDS, updatedIndex)
        }
        for (key in keysToRemove) {
            editor.remove(key)
        }
        return editor.commit()
    }

    private fun selectPrimaryResult(results: List<CheckpointLoadResult>): CheckpointLoadResult {
        if (results.isEmpty()) return CheckpointLoadResult.Empty
        results.firstOrNull { it is CheckpointLoadResult.Unavailable }?.let { return it }

        val loadedList = results.filterIsInstance<CheckpointLoadResult.Loaded>().map { it.recovery }
        if (loadedList.isNotEmpty()) {
            val prioritized = loadedList
                .sortedWith(
                    compareBy<PersistedSessionRecovery>(
                        {
                            when (it.recoveryStatus) {
                                RecoveryStatus.PENDING_RECONCILIATION -> 0
                                RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE -> 1
                                RecoveryStatus.NEEDS_FOLLOW_UP_FALLBACK_FAILED -> 2
                                RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING -> 3
                            }
                        },
                        { it.persistedAtMs },
                        { it.sessionId }
                    )
                )
                .first()
            return CheckpointLoadResult.Loaded(prioritized)
        }

        return results.first()
    }

    private val defaultDelegate = object : PersistenceDelegate {
        override fun saveDetailed(context: Context, recovery: PersistedSessionRecovery): CheckpointSaveResult {
            val payloadError = validatePayload(recovery)
            if (payloadError != null) {
                return CheckpointSaveResult(
                    status = SaveStatus.INVALID_RECOVERY_PAYLOAD,
                    message = payloadError
                )
            }

            synchronized(storeLock) {
                val prefs = try {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } catch (t: Throwable) {
                    return CheckpointSaveResult(
                        status = SaveStatus.REJECTED_STORAGE_UNAVAILABLE,
                        message = "SharedPreferences tidak tersedia: ${t.message}"
                    )
                }

                val allBefore = try {
                    prefs.all.toMap()
                } catch (t: Throwable) {
                    return CheckpointSaveResult(
                        status = SaveStatus.REJECTED_STORAGE_UNAVAILABLE,
                        message = "Gagal membaca SharedPreferences: ${t.message}"
                    )
                }

                val existingForSession = readSessionBucketFromEntries(allBefore, recovery.sessionId)
                val recordToWrite = when (existingForSession) {
                    is CheckpointLoadResult.Unavailable -> {
                        return CheckpointSaveResult(
                            status = SaveStatus.REJECTED_STORAGE_UNAVAILABLE,
                            message = existingForSession.reason
                        )
                    }
                    is CheckpointLoadResult.Corrupt -> {
                        return CheckpointSaveResult(
                            status = SaveStatus.REJECTED_EXISTING_CORRUPT,
                            existingOwnerSessionId = existingForSession.rawSessionId,
                            message = "Menolak overwrite karena checkpoint sessionId=${recovery.sessionId} korup: ${existingForSession.reason}"
                        )
                    }
                    is CheckpointLoadResult.Loaded -> {
                        val existing = existingForSession.recovery
                        if (existing.sessionId != recovery.sessionId) {
                            return CheckpointSaveResult(
                                status = SaveStatus.REJECTED_OWNERSHIP_CONFLICT,
                                existingOwnerSessionId = existing.sessionId,
                                message = "Konflik kepemilikan checkpoint: pemilik=${existing.sessionId}, target=${recovery.sessionId}"
                            )
                        }
                        mergeSameSessionRecovery(existing, recovery)
                    }
                    is CheckpointLoadResult.Empty -> recovery
                }

                // Snapshot seluruh sesi lain untuk memverifikasi isolasi penuh pasca commit
                val otherSessionsBefore = readAllFromPrefs(prefs)
                    .filterIsInstance<CheckpointLoadResult.Loaded>()
                    .map { it.recovery }
                    .filter { it.sessionId != recovery.sessionId }
                    .associateBy { it.sessionId }

                val committed = try {
                    writeSessionRecordToPrefs(prefs, allBefore, recordToWrite)
                } catch (t: Throwable) {
                    return CheckpointSaveResult(
                        status = SaveStatus.COMMIT_FAILED,
                        message = "Exception saat commit checkpoint: ${t.message}"
                    )
                }

                if (!committed) {
                    return CheckpointSaveResult(
                        status = SaveStatus.COMMIT_FAILED,
                        message = "SharedPreferences.Editor.commit() mengembalikan false"
                    )
                }

                val readBackAfter = try {
                    prefs.all.toMap()
                } catch (t: Throwable) {
                    return CheckpointSaveResult(
                        status = SaveStatus.VERIFICATION_FAILED,
                        message = "Gagal membaca ulang pasca commit: ${t.message}"
                    )
                }

                val verified = (readSessionBucketFromEntries(readBackAfter, recordToWrite.sessionId) as? CheckpointLoadResult.Loaded)?.recovery
                // Verifikasi seluruh payload, bukan hanya metrik utama. Checkpoint dianggap
                // durable hanya bila setiap field yang ditulis dapat dibaca kembali identik.
                val isVerified = verified != null &&
                    verified.sessionId == recordToWrite.sessionId &&
                    verified.accumulatedMah == recordToWrite.accumulatedMah &&
                    verified.sessionSeconds == recordToWrite.sessionSeconds &&
                    verified.maxSessionTemp == recordToWrite.maxSessionTemp &&
                    verified.currentSumForAvg == recordToWrite.currentSumForAvg &&
                    verified.currentReadingsCount == recordToWrite.currentReadingsCount &&
                    verified.alertedAnomalyCodes == recordToWrite.alertedAnomalyCodes &&
                    verified.pendingBatchTimestampMs == recordToWrite.pendingBatchTimestampMs &&
                    verified.hasPendingSample == recordToWrite.hasPendingSample &&
                    verified.sampleBatteryLevel == recordToWrite.sampleBatteryLevel &&
                    verified.sampleCurrentMa == recordToWrite.sampleCurrentMa &&
                    verified.sampleVoltageMv == recordToWrite.sampleVoltageMv &&
                    verified.sampleTempCelsius == recordToWrite.sampleTempCelsius &&
                    verified.samplePowerWatt == recordToWrite.samplePowerWatt &&
                    verified.sampleIsScreenOn == recordToWrite.sampleIsScreenOn &&
                    verified.sampleWeight == recordToWrite.sampleWeight.coerceAtLeast(1) &&
                    verified.sampleSpanDurationMs == recordToWrite.sampleSpanDurationMs.coerceAtLeast(0L) &&
                    verified.dbSessionsClosed == recordToWrite.dbSessionsClosed &&
                    verified.failureReason == recordToWrite.failureReason &&
                    verified.persistedAtMs == (if (recordToWrite.persistedAtMs > 0L) recordToWrite.persistedAtMs else verified.persistedAtMs) &&
                    verified.recoveryStatus == recordToWrite.recoveryStatus

                val otherSessionsIntact = otherSessionsBefore.all { (otherId, expectedOther) ->
                    val afterOther = (readSessionBucketFromEntries(readBackAfter, otherId) as? CheckpointLoadResult.Loaded)?.recovery
                    afterOther == expectedOther
                }

                if (!isVerified || !otherSessionsIntact) {
                    if (existingForSession is CheckpointLoadResult.Loaded) {
                        runCatching { writeSessionRecordToPrefs(prefs, readBackAfter, existingForSession.recovery) }
                    }
                    return CheckpointSaveResult(
                        status = SaveStatus.VERIFICATION_FAILED,
                        message = "Verifikasi read-back pasca commit gagal untuk sessionId=${recordToWrite.sessionId} (isVerified=$isVerified, otherIntact=$otherSessionsIntact)"
                    )
                }

                return CheckpointSaveResult(
                    status = SaveStatus.SAVED,
                    message = "Checkpoint terverifikasi untuk sessionId=${recordToWrite.sessionId}"
                )
            }
        }

        override fun loadDetailed(context: Context): CheckpointLoadResult {
            synchronized(storeLock) {
                val all = loadAllDetailed(context)
                return selectPrimaryResult(all)
            }
        }

        override fun loadForSessionDetailed(context: Context, sessionId: Long): CheckpointLoadResult {
            if (sessionId <= 0L) {
                return CheckpointLoadResult.Corrupt("sessionId tidak valid ($sessionId)", sessionId)
            }
            synchronized(storeLock) {
                val prefs = try {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } catch (t: Throwable) {
                    return CheckpointLoadResult.Unavailable(
                        reason = "SharedPreferences tidak dapat dibuka: ${t.message}",
                        cause = t
                    )
                }
                return try {
                    val allEntries = prefs.all
                    val scoped = readSessionBucketFromEntries(allEntries, sessionId)
                    if (scoped !is CheckpointLoadResult.Empty) {
                        return scoped
                    }
                    val legacy = readLegacyUnscopedBucket(allEntries)
                    when (legacy) {
                        is CheckpointLoadResult.Loaded -> if (legacy.recovery.sessionId == sessionId) legacy else CheckpointLoadResult.Empty
                        is CheckpointLoadResult.Corrupt -> if (legacy.rawSessionId == sessionId) legacy else CheckpointLoadResult.Empty
                        else -> CheckpointLoadResult.Empty
                    }
                } catch (t: Throwable) {
                    CheckpointLoadResult.Unavailable(
                        reason = "Gagal membaca checkpoint sessionId=$sessionId: ${t.message}",
                        cause = t
                    )
                }
            }
        }

        override fun loadAllDetailed(context: Context): List<CheckpointLoadResult> {
            synchronized(storeLock) {
                val prefs = try {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } catch (t: Throwable) {
                    return listOf(
                        CheckpointLoadResult.Unavailable(
                            reason = "SharedPreferences tidak dapat dibuka: ${t.message}",
                            cause = t
                        )
                    )
                }
                return readAllFromPrefs(prefs)
            }
        }

        override fun clear(context: Context, expectedSessionId: Long): Boolean {
            if (expectedSessionId <= 0L) return false
            synchronized(storeLock) {
                val prefs = try {
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } catch (_: Throwable) {
                    return false
                }

                val allBefore = try {
                    prefs.all.toMap()
                } catch (_: Throwable) {
                    return false
                }

                return when (val current = readSessionBucketFromEntries(allBefore, expectedSessionId)) {
                    is CheckpointLoadResult.Empty -> true
                    is CheckpointLoadResult.Unavailable -> false
                    is CheckpointLoadResult.Corrupt -> false
                    is CheckpointLoadResult.Loaded -> {
                        if (current.recovery.sessionId != expectedSessionId) {
                            false
                        } else {
                            val otherSessionsBefore = readAllFromPrefs(prefs)
                                .filterIsInstance<CheckpointLoadResult.Loaded>()
                                .map { it.recovery }
                                .filter { it.sessionId != expectedSessionId }
                                .associateBy { it.sessionId }

                            val cleared = runCatching {
                                removeSessionKeysFromPrefs(prefs, allBefore, expectedSessionId)
                            }.getOrDefault(false)
                            if (!cleared) return false

                            val allAfter = runCatching { prefs.all.toMap() }.getOrNull() ?: return false
                            val targetNowEmpty = readSessionBucketFromEntries(allAfter, expectedSessionId) is CheckpointLoadResult.Empty
                            val othersIntact = otherSessionsBefore.all { (otherId, expectedOther) ->
                                (readSessionBucketFromEntries(allAfter, otherId) as? CheckpointLoadResult.Loaded)?.recovery == expectedOther
                            }
                            targetNowEmpty && othersIntact
                        }
                    }
                }
            }
        }

        override fun clearAllForTesting(context: Context): Boolean {
            synchronized(storeLock) {
                return runCatching {
                    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val cleared = prefs.edit().clear().commit()
                    cleared && readAllFromPrefs(prefs).isEmpty()
                }.getOrDefault(false)
            }
        }
    }

    @Volatile
    private var activeDelegate: PersistenceDelegate = defaultDelegate

    data class PersistedSessionRecovery(
        val sessionId: Long,
        val accumulatedMah: Double,
        val sessionSeconds: Long,
        val maxSessionTemp: Float,
        val currentSumForAvg: Long,
        val currentReadingsCount: Long,
        val alertedAnomalyCodes: Set<String>,
        val pendingBatchTimestampMs: Long,
        val hasPendingSample: Boolean,
        val sampleBatteryLevel: Int,
        val sampleCurrentMa: Int,
        val sampleVoltageMv: Int,
        val sampleTempCelsius: Float,
        val samplePowerWatt: Float,
        val sampleIsScreenOn: Boolean,
        val sampleWeight: Int = 1,
        val sampleSpanDurationMs: Long = 0L,
        val dbSessionsClosed: Boolean,
        val failureReason: String,
        val persistedAtMs: Long = System.currentTimeMillis(),
        val recoveryStatus: RecoveryStatus = RecoveryStatus.PENDING_RECONCILIATION
    ) {
        val requiresFollowUp: Boolean
            get() = recoveryStatus != RecoveryStatus.PENDING_RECONCILIATION

        fun toSampleEntityOrNull(): BatterySampleEntity? {
            if (!hasPendingSample || pendingBatchTimestampMs <= 0L) return null
            return BatterySampleEntity(
                sessionId = sessionId,
                timestamp = pendingBatchTimestampMs,
                batteryLevel = sampleBatteryLevel,
                currentMa = sampleCurrentMa,
                voltageMv = sampleVoltageMv,
                tempCelsius = sampleTempCelsius,
                powerWatt = samplePowerWatt,
                isScreenOn = sampleIsScreenOn
            )
        }

        fun toCompactedLiveBatteryState(): LiveBatteryState? {
            if (!hasPendingSample) return null
            return LiveBatteryState(
                level = sampleBatteryLevel,
                currentMa = sampleCurrentMa,
                voltageMv = sampleVoltageMv,
                powerWatt = samplePowerWatt,
                tempCelsius = sampleTempCelsius,
                isCharging = true,
                sessionMahDelivered = accumulatedMah,
                sessionDurationSec = sessionSeconds
            )
        }
    }

    /**
     * Menyimpan checkpoint recovery dengan validasi eksplisit terhadap [expectedOwnerSessionId].
     * Mencegah penulisan lintas sesi apabila payload tidak cocok dengan pemilik yang diharapkan.
     */
    fun saveForSession(
        context: Context,
        expectedOwnerSessionId: Long,
        recovery: PersistedSessionRecovery
    ): CheckpointSaveResult {
        if (expectedOwnerSessionId <= 0L || recovery.sessionId != expectedOwnerSessionId) {
            return CheckpointSaveResult(
                status = SaveStatus.REJECTED_OWNERSHIP_CONFLICT,
                existingOwnerSessionId = expectedOwnerSessionId,
                message = "Penolakan penulisan lintas sesi: expectedOwnerSessionId=$expectedOwnerSessionId tetapi recovery.sessionId=${recovery.sessionId}"
            )
        }
        return saveDetailed(context, recovery)
    }

    fun saveDetailed(context: Context, recovery: PersistedSessionRecovery): CheckpointSaveResult {
        val payloadError = validatePayload(recovery)
        if (payloadError != null) {
            return CheckpointSaveResult(
                status = SaveStatus.INVALID_RECOVERY_PAYLOAD,
                message = payloadError
            )
        }
        return try {
            activeDelegate.saveDetailed(context, recovery)
        } catch (t: Throwable) {
            CheckpointSaveResult(
                status = SaveStatus.COMMIT_FAILED,
                message = "Delegate saveDetailed exception: ${t.message}"
            )
        }
    }

    fun save(context: Context, recovery: PersistedSessionRecovery): Boolean {
        return saveDetailed(context, recovery).isSuccess
    }

    fun load(context: Context): CheckpointLoadResult {
        return try {
            activeDelegate.loadDetailed(context)
        } catch (t: Throwable) {
            CheckpointLoadResult.Unavailable(
                reason = "Delegate loadDetailed exception: ${t.message}",
                cause = t
            )
        }
    }

    fun loadForSession(context: Context, sessionId: Long): CheckpointLoadResult {
        if (sessionId <= 0L) {
            return CheckpointLoadResult.Corrupt("sessionId tidak valid ($sessionId)", sessionId)
        }
        return try {
            activeDelegate.loadForSessionDetailed(context, sessionId)
        } catch (t: Throwable) {
            CheckpointLoadResult.Unavailable(
                reason = "Delegate loadForSessionDetailed exception: ${t.message}",
                cause = t
            )
        }
    }

    fun loadAll(context: Context): List<CheckpointLoadResult> {
        return try {
            activeDelegate.loadAllDetailed(context)
        } catch (t: Throwable) {
            listOf(
                CheckpointLoadResult.Unavailable(
                    reason = "Delegate loadAllDetailed exception: ${t.message}",
                    cause = t
                )
            )
        }
    }

    fun loadAllLoaded(context: Context): List<PersistedSessionRecovery> {
        return loadAll(context).filterIsInstance<CheckpointLoadResult.Loaded>().map { it.recovery }
    }

    fun loadOrNull(context: Context): PersistedSessionRecovery? {
        return load(context).getOrNull()
    }

    fun loadForSessionOrNull(context: Context, sessionId: Long): PersistedSessionRecovery? {
        return loadForSession(context, sessionId).getOrNull()
    }

    fun markDbSessionsClosed(context: Context, sessionId: Long): Boolean {
        if (sessionId <= 0L) return false
        return try {
            when (val current = loadForSession(context, sessionId)) {
                is CheckpointLoadResult.Empty -> true
                is CheckpointLoadResult.Unavailable,
                is CheckpointLoadResult.Corrupt -> false
                is CheckpointLoadResult.Loaded -> {
                    val existing = current.recovery
                    if (existing.sessionId != sessionId) return false
                    val saveResult = saveForSession(context, sessionId, existing.copy(dbSessionsClosed = true))
                    if (!saveResult.isSuccess) return false
                    val verified = (loadForSession(context, sessionId) as? CheckpointLoadResult.Loaded)?.recovery
                    verified != null && verified.sessionId == sessionId && verified.dbSessionsClosed
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Memperbarui status tindak lanjut pada checkpoint milik [expectedSessionId] secara atomik
     * tanpa menyentuh checkpoint sesi lain, dan memverifikasi hasil simpan + baca-ulang sebelum
     * mengembalikan true.
     */
    fun markRequiresFollowUp(
        context: Context,
        expectedSessionId: Long,
        status: RecoveryStatus,
        reason: String
    ): Boolean {
        if (expectedSessionId <= 0L) return false
        return try {
            val current = (loadForSession(context, expectedSessionId) as? CheckpointLoadResult.Loaded)?.recovery
                ?: return false
            if (current.sessionId != expectedSessionId) return false
            val saveResult = saveForSession(
                context,
                expectedSessionId,
                current.copy(
                    recoveryStatus = status,
                    failureReason = reason
                )
            )
            if (!saveResult.isSuccess) return false
            val verified = (loadForSession(context, expectedSessionId) as? CheckpointLoadResult.Loaded)?.recovery
            verified != null &&
                verified.sessionId == expectedSessionId &&
                verified.recoveryStatus == status &&
                verified.failureReason == reason
        } catch (_: Throwable) {
            false
        }
    }

    fun clear(context: Context, expectedSessionId: Long): Boolean {
        if (expectedSessionId <= 0L) return false
        return try {
            activeDelegate.clear(context, expectedSessionId)
        } catch (_: Throwable) {
            false
        }
    }

    @VisibleForTesting
    fun clearAllForTesting(context: Context): Boolean {
        return try {
            activeDelegate.clearAllForTesting(context)
        } catch (_: Throwable) {
            false
        }
    }

    @VisibleForTesting
    fun setPersistenceDelegateForTesting(delegate: PersistenceDelegate?) {
        activeDelegate = delegate ?: defaultDelegate
    }

    @VisibleForTesting
    fun resetPersistenceDelegate() {
        activeDelegate = defaultDelegate
    }
}
