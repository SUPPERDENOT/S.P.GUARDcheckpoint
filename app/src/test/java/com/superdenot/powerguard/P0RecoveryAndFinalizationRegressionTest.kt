package com.superdenot.powerguard

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.SessionRecoveryStore
import com.superdenot.powerguard.data.local.dao.BatteryDao
import com.superdenot.powerguard.data.local.dao.SessionFinalizationStatus
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.receiver.PowerConnectionReceiver
import com.superdenot.powerguard.service.BatteryMonitorService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class P0RecoveryAndFinalizationRegressionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var faultDao: FaultInjectingBatteryDao
    private lateinit var serviceController: ServiceController<BatteryMonitorService>
    private lateinit var service: BatteryMonitorService

    private class FaultInjectingBatteryDao(
        private val delegate: BatteryDao
    ) : BatteryDao by delegate {

        var failReconcile: Boolean = false
        var failSampleFlush: Boolean = false
        var failFinalizeTarget: Boolean = false
        var failSingleSessionClose: Boolean = false
        var failingSessionIdOnSingleClose: Long? = null
        var failSessionLookup: Boolean = false
        var failOpenSessionsQuery: Boolean = false

        var onReconcileEntered: CompletableDeferred<Unit>? = null
        var allowReconcileProceed: CompletableDeferred<Unit>? = null

        override suspend fun getSessionById(sessionId: Long): ChargingSessionEntity? {
            if (failSessionLookup) {
                throw SQLiteException("Simulated Room database inaccessible on getSessionById")
            }
            return delegate.getSessionById(sessionId)
        }

        override suspend fun getAllOpenSessions(): List<ChargingSessionEntity> {
            if (failOpenSessionsQuery) {
                throw SQLiteException("Simulated Room database inaccessible on getAllOpenSessions")
            }
            return delegate.getAllOpenSessions()
        }

        override suspend fun reconcileRecoveredSessionAtomically(
            sessionId: Long,
            recoveredTotalMah: Double,
            recoveredAvgCurrentMa: Int,
            recoveredMaxTempCelsius: Float,
            recoveredHasAnomaly: Boolean,
            recoveredAnomalyReason: String?,
            recoveredSample: BatterySampleEntity?
        ): Boolean {
            onReconcileEntered?.complete(Unit)
            allowReconcileProceed?.await()
            if (failReconcile) {
                throw SQLiteException("Simulated Room transaction abort before commit")
            }
            return delegate.reconcileRecoveredSessionAtomically(
                sessionId = sessionId,
                recoveredTotalMah = recoveredTotalMah,
                recoveredAvgCurrentMa = recoveredAvgCurrentMa,
                recoveredMaxTempCelsius = recoveredMaxTempCelsius,
                recoveredHasAnomaly = recoveredHasAnomaly,
                recoveredAnomalyReason = recoveredAnomalyReason,
                recoveredSample = recoveredSample
            )
        }

        override suspend fun persistSampleAndUpdateSessionAtomically(
            sample: BatterySampleEntity,
            totalMahDelivered: Double,
            avgCurrentMa: Int,
            maxTempCelsius: Float,
            hasAnomaly: Boolean,
            anomalyReason: String?,
            deepSleepRatio: Float
        ) = if (failSampleFlush) {
            throw SQLiteException("Simulated SQLite write error during sample flush")
        } else {
            delegate.persistSampleAndUpdateSessionAtomically(
                sample = sample,
                totalMahDelivered = totalMahDelivered,
                avgCurrentMa = avgCurrentMa,
                maxTempCelsius = maxTempCelsius,
                hasAnomaly = hasAnomaly,
                anomalyReason = anomalyReason,
                deepSleepRatio = deepSleepRatio
            )
        }

        override suspend fun finalizeSessionAtomically(
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
            healthLog: com.superdenot.powerguard.data.local.entity.BatteryHealthEntity?
        ): SessionFinalizationStatus {
            if (failFinalizeTarget) {
                throw SQLiteException("Simulated SQLite lock during target finalization")
            }
            return delegate.finalizeSessionAtomically(
                sessionId = sessionId,
                endTime = endTime,
                endLevel = endLevel,
                totalMahDelivered = totalMahDelivered,
                avgCurrentMa = avgCurrentMa,
                maxTempCelsius = maxTempCelsius,
                hasAnomaly = hasAnomaly,
                anomalyReason = anomalyReason,
                deepSleepRatio = deepSleepRatio,
                finalSample = finalSample,
                healthLog = healthLog
            )
        }

        override suspend fun finalizeSingleSessionWithReconciledMetrics(
            sessionId: Long,
            endTime: Long,
            endLevel: Int,
            designCapacityMah: Int
        ): SessionFinalizationStatus {
            if (failSingleSessionClose || failingSessionIdOnSingleClose == sessionId) {
                throw SQLiteException("Simulated SQLite failure on single session id=$sessionId")
            }
            return delegate.finalizeSingleSessionWithReconciledMetrics(
                sessionId = sessionId,
                endTime = endTime,
                endLevel = endLevel,
                designCapacityMah = designCapacityMah
            )
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SessionRecoveryStore.resetPersistenceDelegate()
        SessionRecoveryStore.clearAllForTesting(context)

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        faultDao = FaultInjectingBatteryDao(db.batteryDao())

        serviceController = Robolectric.buildService(BatteryMonitorService::class.java).create()
        service = serviceController.get()
        service.daoOverride = faultDao
    }

    @After
    fun tearDown() {
        runCatching { serviceController.destroy() }
        SessionRecoveryStore.resetPersistenceDelegate()
        SessionRecoveryStore.clearAllForTesting(context)
        db.close()
    }

    private fun sampleLiveState(
        level: Int = 55,
        currentMa: Int = 1800,
        voltageMv: Int = 4200,
        tempCelsius: Float = 36.5f,
        isCharging: Boolean = true,
        sessionMah: Double = 450.0,
        sessionDurationSec: Long = 900L
    ): LiveBatteryState {
        return LiveBatteryState(
            level = level,
            currentMa = currentMa,
            voltageMv = voltageMv,
            powerWatt = (voltageMv / 1000f) * (currentMa / 1000f),
            tempCelsius = tempCelsius,
            isCharging = isCharging,
            chargeSource = if (isCharging) "AC Adapter" else "Discharging",
            isFastCharging = isCharging,
            sessionMahDelivered = sessionMah,
            sessionDurationSec = sessionDurationSec,
            deepSleepRatio = 0.4f,
            anomalyAlert = null,
            designCapacityMah = 5000
        )
    }

    private fun buildRecoveryCheckpoint(
        sessionId: Long,
        accumulatedMah: Double = 850.0,
        sessionSeconds: Long = 1800L,
        maxTemp: Float = 38.5f,
        currentSum: Long = 1_500_000L,
        currentCount: Long = 1000L,
        anomalyCodes: Set<String> = setOf("SLOW_CHARGING"),
        batchTimestampMs: Long = 1_700_000_050_000L,
        hasPendingSample: Boolean = true,
        sampleWeight: Int = 1,
        dbSessionsClosed: Boolean = false
    ): SessionRecoveryStore.PersistedSessionRecovery {
        return SessionRecoveryStore.PersistedSessionRecovery(
            sessionId = sessionId,
            accumulatedMah = accumulatedMah,
            sessionSeconds = sessionSeconds,
            maxSessionTemp = maxTemp,
            currentSumForAvg = currentSum,
            currentReadingsCount = currentCount,
            alertedAnomalyCodes = anomalyCodes,
            pendingBatchTimestampMs = batchTimestampMs,
            hasPendingSample = hasPendingSample,
            sampleBatteryLevel = 62,
            sampleCurrentMa = 1500,
            sampleVoltageMv = 4250,
            sampleTempCelsius = maxTemp,
            samplePowerWatt = 6.375f,
            sampleIsScreenOn = false,
            sampleWeight = sampleWeight,
            dbSessionsClosed = dbSessionsClosed,
            failureReason = "Test checkpoint"
        )
    }

    /**
     * Skenario 1: Recovery normal berhasil -> data direkonsiliasi ke Room dan checkpoint baru dihapus setelah commit.
     */
    @Test
    fun test01_normalRecoverySuccess_reconcilesRoomBeforeClearingCheckpoint() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 1_800_000L,
                startLevel = 25,
                totalMahDelivered = 500.0,
                avgCurrentMa = 1400,
                maxTempCelsius = 36.0f,
                chargerType = "FAST (AC Adapter)"
            )
        )
        val batchTs = 1_700_000_050_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 850.0,
                    sessionSeconds = 1800L,
                    maxTemp = 38.5f,
                    anomalyCodes = setOf("SLOW_CHARGING"),
                    batchTimestampMs = batchTs
                )
            )
        )

        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 63, tempCelsius = 38.0f))

        val updatedSession = faultDao.getSessionById(sessionId)
        assertNotNull(updatedSession)
        assertEquals(850.0, updatedSession!!.totalMahDelivered, 0.001)
        assertEquals(1500, updatedSession.avgCurrentMa)
        assertEquals(38.5f, updatedSession.maxTempCelsius, 0.01f)
        assertTrue(updatedSession.isAnomaly)
        assertEquals("SLOW_CHARGING", updatedSession.anomalyReason)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 2: Aplikasi berhenti sebelum transaksi Room selesai -> checkpoint tetap tersimpan dan bisa dilanjutkan.
     */
    @Test
    fun test02_appStopsBeforeRoomTransactionCompletes_retainsCheckpointAndResumesLater() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 1_200_000L,
                startLevel = 30,
                totalMahDelivered = 300.0,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_060_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 620.0,
                    batchTimestampMs = batchTs
                )
            )
        )

        faultDao.failReconcile = true
        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 52))

        val sessionAfterAbort = faultDao.getSessionById(sessionId)!!
        assertEquals(300.0, sessionAfterAbort.totalMahDelivered, 0.001)
        assertEquals(0, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        val retainedCheckpoint = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Checkpoint tidak boleh terhapus jika transaksi Room gagal", retainedCheckpoint)
        assertEquals(620.0, retainedCheckpoint!!.accumulatedMah, 0.001)

        faultDao.failReconcile = false
        service.seedActiveSessionStateForTesting(
            sessionId = null,
            accumulatedMah = 0.0,
            sessionSeconds = 0L,
            maxSessionTemp = 0f,
            currentSumForAvg = 0L,
            currentReadingsCount = 0L
        )
        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 53))

        val recoveredSession = faultDao.getSessionById(sessionId)!!
        assertEquals(620.0, recoveredSession.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 3: Transaksi Room berhasil tetapi checkpoint belum sempat dihapus -> recovery ulang idempotent tanpa duplikasi.
     */
    @Test
    fun test03_roomCommitsButCheckpointNotYetCleared_repeatRecoveryIsIdempotent() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 1_500_000L,
                startLevel = 20,
                totalMahDelivered = 400.0,
                anomalyReason = "THERMAL_THROTTLING",
                isAnomaly = true,
                chargerType = "FAST (AC Adapter)"
            )
        )
        val batchTs = 1_700_000_070_000L
        val checkpoint = buildRecoveryCheckpoint(
            sessionId = sessionId,
            accumulatedMah = 740.0,
            anomalyCodes = setOf("THERMAL_THROTTLING"),
            batchTimestampMs = batchTs
        )

        var blockClear = true
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            private var stored: SessionRecoveryStore.PersistedSessionRecovery? = checkpoint

            override fun saveDetailed(
                context: Context,
                recovery: SessionRecoveryStore.PersistedSessionRecovery
            ): SessionRecoveryStore.CheckpointSaveResult {
                stored = recovery
                return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.SAVED)
            }

            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                return stored?.let { SessionRecoveryStore.CheckpointLoadResult.Loaded(it) }
                    ?: SessionRecoveryStore.CheckpointLoadResult.Empty
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean {
                if (blockClear) return false
                if (stored?.sessionId == expectedSessionId) {
                    stored = null
                }
                return true
            }

            override fun clearAllForTesting(context: Context): Boolean {
                stored = null
                return true
            }
        })

        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 60))
        assertNotNull("Checkpoint masih ada karena clear() gagal", SessionRecoveryStore.loadOrNull(context))
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(740.0, faultDao.getSessionById(sessionId)!!.totalMahDelivered, 0.001)

        blockClear = false
        service.seedActiveSessionStateForTesting(
            sessionId = null,
            accumulatedMah = 0.0,
            sessionSeconds = 0L,
            maxSessionTemp = 0f,
            currentSumForAvg = 0L,
            currentReadingsCount = 0L
        )
        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 60))

        val finalRow = faultDao.getSessionById(sessionId)!!
        assertEquals("mAh tidak boleh tergandakan saat recovery ulang", 740.0, finalRow.totalMahDelivered, 0.001)
        assertEquals("Sampel tidak boleh terduplikasi saat recovery ulang", 1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals("Anomaly reason tidak boleh terduplikasi", "THERMAL_THROTTLING", finalRow.anomalyReason)
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 4: SessionRecoveryStore.save() gagal -> buffer dan state sesi tidak dihapus/direset dan tersedia retry aman.
     */
    @Test
    fun test04_sessionRecoveryStoreSaveFails_preservesBufferAndStateWithSafeRetry() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 300_000L,
                startLevel = 40,
                chargerType = "AC Adapter"
            )
        )
        val bufferedSamples = listOf(
            sampleLiveState(level = 41, currentMa = 1600, sessionMah = 100.0, sessionDurationSec = 60L),
            sampleLiveState(level = 42, currentMa = 1650, sessionMah = 200.0, sessionDurationSec = 120L),
            sampleLiveState(level = 43, currentMa = 1700, sessionMah = 300.0, sessionDurationSec = 180L)
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 410.0,
            sessionSeconds = 300L,
            maxSessionTemp = 37.5f,
            currentSumForAvg = 495_000L,
            currentReadingsCount = 300L,
            alertedAnomalyCodes = setOf("SLOW_CHARGING"),
            bufferedSamples = bufferedSamples,
            pendingBatchTimestampMs = 1_700_000_080_000L
        )

        faultDao.failFinalizeTarget = true
        var failSave = true
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            private var stored: SessionRecoveryStore.PersistedSessionRecovery? = null

            override fun saveDetailed(
                context: Context,
                recovery: SessionRecoveryStore.PersistedSessionRecovery
            ): SessionRecoveryStore.CheckpointSaveResult {
                if (failSave) {
                    return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.COMMIT_FAILED)
                }
                stored = recovery
                return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.SAVED)
            }

            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                return stored?.let { SessionRecoveryStore.CheckpointLoadResult.Loaded(it) }
                    ?: SessionRecoveryStore.CheckpointLoadResult.Empty
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean {
                if (stored?.sessionId == expectedSessionId) stored = null
                return true
            }

            override fun clearAllForTesting(context: Context): Boolean {
                stored = null
                return true
            }
        })

        val result = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.FAILED, result)

        val snapshotAfterFail = service.getInMemorySessionSnapshotForTesting()
        assertEquals(sessionId, snapshotAfterFail.activeSessionId)
        assertEquals(3, snapshotAfterFail.bufferedSamplesCount)
        assertEquals(410.0, snapshotAfterFail.accumulatedMah, 0.001)
        assertEquals(300L, snapshotAfterFail.sessionSeconds)
        assertTrue(snapshotAfterFail.checkpointSavePendingRetry)
        assertNull("Sesi di Room tidak boleh ditutup destruktif saat save checkpoint gagal", faultDao.getSessionById(sessionId)!!.endTime)

        failSave = false
        faultDao.failFinalizeTarget = false
        val retryOk = service.retryPendingCheckpointAndFinalizationForTest()
        assertTrue(retryOk)
        assertEquals(0, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, 1_700_000_080_000L))
    }

    /**
     * Skenario 5: Penyimpanan sampel ke Room gagal -> buffer tetap dipertahankan, backoff terjadwal, retry berhasil.
     */
    @Test
    fun test05_sampleFlushToRoomFails_retainsBufferAndRecoversOnRetry() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 60_000L,
                startLevel = 50,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_085_000L
        val samples = listOf(
            sampleLiveState(level = 51, currentMa = 1800, sessionMah = 160.0, sessionDurationSec = 30L),
            sampleLiveState(level = 52, currentMa = 1850, sessionMah = 320.0, sessionDurationSec = 60L)
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 320.0,
            sessionSeconds = 60L,
            maxSessionTemp = 36.8f,
            currentSumForAvg = 109_500L,
            currentReadingsCount = 60L,
            bufferedSamples = samples,
            pendingBatchTimestampMs = batchTs
        )

        faultDao.failSampleFlush = true
        service.flushSampleBufferForTesting(isScreenOn = false)

        val midSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals("Buffer sampel tidak boleh hilang saat flush ke Room gagal", 2, midSnapshot.bufferedSamplesCount)
        assertEquals(1, midSnapshot.consecutiveFlushFailures)
        assertTrue(midSnapshot.nextFlushRetryAtSec > 60L)
        assertNotNull("Checkpoint darurat harus tersimpan saat flush gagal", SessionRecoveryStore.loadOrNull(context))

        faultDao.failSampleFlush = false
        service.flushSampleBufferForTesting(isScreenOn = false)

        val postSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals(0, postSnapshot.bufferedSamplesCount)
        assertEquals(0, postSnapshot.consecutiveFlushFailures)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(320.0, faultDao.getSessionById(sessionId)!!.totalMahDelivered, 0.001)
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 6: Target sesi tidak ditemukan -> mengembalikan TARGET_MISSING_NEEDS_FOLLOW_UP
     * (tidak pernah dianggap persistensi bersih) dan tidak menutup sesi lain yang masih terbuka.
     */
    @Test
    fun test06_targetSessionNotFound_doesNotCloseOtherOpenSessions() = runTest {
        val openSessionA = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 600_000L,
                startLevel = 20,
                totalMahDelivered = 250.0,
                chargerType = "AC Adapter"
            )
        )
        val openSessionB = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 300_000L,
                startLevel = 45,
                totalMahDelivered = 120.0,
                chargerType = "USB Port"
            )
        )

        val daoStatus = faultDao.finalizeSessionAtomically(
            sessionId = 99_999L,
            endTime = System.currentTimeMillis(),
            endLevel = 80,
            totalMahDelivered = 0.0,
            avgCurrentMa = 0,
            maxTempCelsius = 0f,
            hasAnomaly = false,
            anomalyReason = null,
            deepSleepRatio = 0f,
            finalSample = null,
            healthLog = null
        )
        assertEquals(SessionFinalizationStatus.TARGET_MISSING, daoStatus)
        assertNull("Sesi A harus tetap terbuka", faultDao.getSessionById(openSessionA)!!.endTime)
        assertNull("Sesi B harus tetap terbuka", faultDao.getSessionById(openSessionB)!!.endTime)

        service.seedActiveSessionStateForTesting(
            sessionId = 88_888L,
            accumulatedMah = 50.0,
            sessionSeconds = 60L,
            maxSessionTemp = 35.0f,
            currentSumForAvg = 60_000L,
            currentReadingsCount = 60L
        )
        val serviceStatus = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP, serviceStatus)
        assertEquals(88_888L, service.getInMemorySessionSnapshotForTesting().activeSessionId)
        assertNotNull("Checkpoint follow-up harus tersimpan untuk target hilang", SessionRecoveryStore.loadOrNull(context))
        assertNull("Service tidak boleh menutup Sesi A secara massal", faultDao.getSessionById(openSessionA)!!.endTime)
        assertNull("Service tidak boleh menutup Sesi B secara massal", faultDao.getSessionById(openSessionB)!!.endTime)
    }

    /**
     * Skenario 7: Finalisasi dijalankan berulang kali -> idempotent tanpa duplikasi sampel maupun health log.
     */
    @Test
    fun test07_repeatedFinalization_isIdempotentWithoutDuplicates() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 3_600_000L,
                startLevel = 20,
                chargerType = "FAST (AC Adapter)"
            )
        )
        val finalSampleTs = 1_700_000_090_000L
        val buffered = listOf(
            sampleLiveState(level = 80, currentMa = 2500, sessionMah = 2500.0, sessionDurationSec = 3600L)
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 2500.0,
            sessionSeconds = 3600L,
            maxSessionTemp = 39.0f,
            currentSumForAvg = 9_000_000L,
            currentReadingsCount = 3600L,
            bufferedSamples = buffered,
            pendingBatchTimestampMs = finalSampleTs
        )

        val firstResult = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.COMPLETE_DATA_FINALIZED, firstResult)

        val firstRow = faultDao.getSessionById(sessionId)!!
        assertNotNull(firstRow.endTime)
        assertEquals(2500.0, firstRow.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, finalSampleTs))
        assertEquals(1, faultDao.countHealthLogsForSession(sessionId))

        val secondDaoStatus = faultDao.finalizeSessionAtomically(
            sessionId = sessionId,
            endTime = firstRow.endTime!! + 10_000L,
            endLevel = 81,
            totalMahDelivered = 2500.0,
            avgCurrentMa = 2500,
            maxTempCelsius = 39.0f,
            hasAnomaly = false,
            anomalyReason = null,
            deepSleepRatio = 0.5f,
            finalSample = BatterySampleEntity(
                sessionId = sessionId,
                timestamp = finalSampleTs,
                batteryLevel = 80,
                currentMa = 2500,
                voltageMv = 4200,
                tempCelsius = 39.0f,
                powerWatt = 10.5f,
                isScreenOn = false
            ),
            healthLog = null
        )
        val thirdServiceResult = service.ensureSessionFinalizedSafelyForTesting()

        assertEquals(SessionFinalizationStatus.ALREADY_CLOSED, secondDaoStatus)
        assertEquals(BatteryMonitorService.SessionFinalizationResult.NO_ACTIVE_SESSION_CLEAN, thirdServiceResult)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, finalSampleTs))
        assertEquals(1, faultDao.countHealthLogsForSession(sessionId))
        assertEquals(firstRow.endTime, faultDao.getSessionById(sessionId)!!.endTime)
    }

    /**
     * Skenario 8: Cleanup receiver dan service terjadi bersamaan -> tersinkronisasi aman tanpa race/korupsi data.
     */
    @Test
    fun test08_concurrentReceiverAndServiceCleanup_synchronizesCleanly() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 900_000L,
                startLevel = 30,
                totalMahDelivered = 400.0,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_095_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 900.0,
                    batchTimestampMs = batchTs
                )
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 900.0,
            sessionSeconds = 900L,
            maxSessionTemp = 38.5f,
            currentSumForAvg = 1_350_000L,
            currentReadingsCount = 900L,
            bufferedSamples = listOf(sampleLiveState(level = 65, sessionMah = 900.0, sessionDurationSec = 900L)),
            pendingBatchTimestampMs = batchTs
        )

        val receiver = PowerConnectionReceiver()
        val jobs = listOf(
            async { service.ensureSessionFinalizedSafelyForTesting() },
            async {
                receiver.executeGuardedBackgroundCleanupNow(
                    context = context,
                    daoOverride = faultDao,
                    isChargingOverride = false,
                    batteryLevelOverride = 65
                )
            }
        )
        jobs.awaitAll()

        val closedRow = faultDao.getSessionById(sessionId)!!
        assertNotNull(closedRow.endTime)
        assertEquals(900.0, closedRow.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertTrue(faultDao.getAllOpenSessions().isEmpty())
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 9: Aplikasi dibuka kembali ketika masih ada sesi yang belum selesai -> rekonsiliasi & lanjut/tutup aman.
     */
    @Test
    fun test09_appReopenedWithUnfinishedSession_reconcilesAndCompletesSafely() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 1_200_000L,
                startLevel = 40,
                totalMahDelivered = 450.0,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_100_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 520.0,
                    batchTimestampMs = batchTs
                )
            )
        )

        BatteryMonitorService.recoverOrReconcileOnAppLaunch(
            context = context,
            daoOverride = faultDao
        )

        val reconciledRow = faultDao.getSessionById(sessionId)!!
        assertNotNull("Sesi belum selesai harus difinalisasi saat aplikasi dibuka dalam kondisi unplugged", reconciledRow.endTime)
        assertEquals(520.0, reconciledRow.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 10: Buffer sampel tetap aman ketika proses cleanup gagal -> tidak ada sampel hilang & bisa di-retry.
     */
    @Test
    fun test10_sampleBufferPreservedWhenCleanupFails_allowsSafeRecovery() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 120_000L,
                startLevel = 55,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_110_000L
        val twelveSamples = (1..12).map { idx ->
            sampleLiveState(
                level = 55 + (idx / 4),
                currentMa = 1700 + idx * 10,
                sessionMah = 30.0 * idx,
                sessionDurationSec = 10L * idx
            )
        }
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 380.0,
            sessionSeconds = 120L,
            maxSessionTemp = 37.2f,
            currentSumForAvg = 211_200L,
            currentReadingsCount = 120L,
            bufferedSamples = twelveSamples,
            pendingBatchTimestampMs = batchTs
        )

        faultDao.failFinalizeTarget = true
        faultDao.failSampleFlush = true
        faultDao.failSingleSessionClose = true

        val cleanupResult = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.DATABASE_UNAVAILABLE, cleanupResult)

        val persistedCheckpoint = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Checkpoint harus menyimpan sampel tertunda saat cleanup DB gagal", persistedCheckpoint)
        assertTrue(persistedCheckpoint!!.hasPendingSample)
        assertEquals(batchTs, persistedCheckpoint.pendingBatchTimestampMs)
        assertTrue(service.getInMemorySessionSnapshotForTesting().hasIncompleteRecoverySnapshot)

        faultDao.failFinalizeTarget = false
        faultDao.failSampleFlush = false
        faultDao.failSingleSessionClose = false

        BatteryMonitorService.recoverOrReconcileOnAppLaunch(
            context = context,
            daoOverride = faultDao
        )

        val recoveredSession = faultDao.getSessionById(sessionId)!!
        assertNotNull(recoveredSession.endTime)
        assertEquals(380.0, recoveredSession.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 11: Target session hilang di Room -> checkpoint tetap tersimpan,
     * seluruh metadata recovery dipertahankan, ditandai membutuhkan penanganan lanjutan, dan
     * tidak membuat session/sample palsu di Room.
     */
    @Test
    fun test11_orphanedRecoveryCheckpoint_targetSessionMissing_retainsCheckpointAndMarksFollowUp() = runTest {
        val missingSessionId = 77_777L
        val batchTs = 1_700_000_200_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = missingSessionId,
                    accumulatedMah = 925.5,
                    sessionSeconds = 2100L,
                    maxTemp = 39.2f,
                    currentSum = 3_150_000L,
                    currentCount = 2100L,
                    anomalyCodes = setOf("SLOW_CHARGING", "THERMAL_THROTTLING"),
                    batchTimestampMs = batchTs,
                    hasPendingSample = true
                )
            )
        )

        val reconcileStatus = service.reconcilePersistedRecoveryForTesting()
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP,
            reconcileStatus
        )

        val persistedAfter = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Orphaned checkpoint wajib tetap tersimpan di SessionRecoveryStore", persistedAfter)
        assertEquals(missingSessionId, persistedAfter!!.sessionId)
        assertEquals(925.5, persistedAfter.accumulatedMah, 0.001)
        assertEquals(2100L, persistedAfter.sessionSeconds)
        assertEquals(39.2f, persistedAfter.maxSessionTemp, 0.01f)
        assertEquals(3_150_000L, persistedAfter.currentSumForAvg)
        assertEquals(2100L, persistedAfter.currentReadingsCount)
        assertEquals(setOf("SLOW_CHARGING", "THERMAL_THROTTLING"), persistedAfter.alertedAnomalyCodes)
        assertEquals(batchTs, persistedAfter.pendingBatchTimestampMs)
        assertTrue(persistedAfter.hasPendingSample)
        assertEquals(
            SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
            persistedAfter.recoveryStatus
        )
        assertTrue(persistedAfter.requiresFollowUp)

        val memSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertTrue(memSnapshot.hasIncompleteRecoverySnapshot)
        assertTrue(memSnapshot.recoveryRequiresFollowUp)
        assertEquals(missingSessionId, memSnapshot.incompleteRecoverySessionId)
        assertEquals(925.5, memSnapshot.incompleteRecoveryAccumulatedMah, 0.001)
        assertNull("Tidak boleh membuat row ChargingSessionEntity palsu", faultDao.getSessionById(missingSessionId))
        assertEquals("Tidak boleh membuat BatterySampleEntity palsu", 0, faultDao.countSamplesAtTimestamp(missingSessionId, batchTs))

        BatteryMonitorService.recoverOrReconcileOnAppLaunch(context = context, daoOverride = faultDao)
        assertNotNull("Startup recovery tidak boleh menghapus orphan checkpoint", SessionRecoveryStore.loadOrNull(context))

        PowerConnectionReceiver().executeGuardedBackgroundCleanupNow(
            context = context,
            daoOverride = faultDao,
            isChargingOverride = false,
            batteryLevelOverride = 60
        )
        val afterReceiver = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Receiver cleanup tidak boleh menghapus orphan checkpoint", afterReceiver)
        assertEquals(925.5, afterReceiver!!.accumulatedMah, 0.001)
    }

    /**
     * Skenario 12: Fallback executeFallbackSessionClosure() menerima TARGET_MISSING
     * -> tidak memanggil SessionRecoveryStore.clear() maupun resetInMemorySessionTracking().
     */
    @Test
    fun test12_fallbackTargetMissing_doesNotClearCheckpointOrResetInMemoryTracking() = runTest {
        val missingTargetId = 66_666L
        val batchTs = 1_700_000_210_000L
        val buffered = listOf(
            sampleLiveState(level = 58, currentMa = 1600, sessionMah = 160.0, sessionDurationSec = 360L),
            sampleLiveState(level = 59, currentMa = 1620, sessionMah = 320.0, sessionDurationSec = 720L),
            sampleLiveState(level = 60, currentMa = 1640, sessionMah = 480.0, sessionDurationSec = 1080L),
            sampleLiveState(level = 61, currentMa = 1660, sessionMah = 640.0, sessionDurationSec = 1440L)
        )
        service.seedActiveSessionStateForTesting(
            sessionId = missingTargetId,
            accumulatedMah = 640.0,
            sessionSeconds = 1440L,
            maxSessionTemp = 38.4f,
            currentSumForAvg = 2_304_000L,
            currentReadingsCount = 1440L,
            alertedAnomalyCodes = setOf("FAKE_FAST_CHARGING"),
            bufferedSamples = buffered,
            pendingBatchTimestampMs = batchTs
        )

        val fallbackStatus = service.executeFallbackSessionClosureForTesting()
        assertEquals(
            "Fallback TARGET_MISSING wajib mengembalikan status kegagalan eksplisit",
            BatteryMonitorService.FallbackClosureStatus.TARGET_MISSING_FAILED,
            fallbackStatus
        )

        val checkpoint = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Checkpoint tidak boleh dihapus saat fallback TARGET_MISSING", checkpoint)
        assertEquals(missingTargetId, checkpoint!!.sessionId)
        assertEquals(640.0, checkpoint.accumulatedMah, 0.001)
        assertEquals(
            SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
            checkpoint.recoveryStatus
        )

        val memSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals("activeSessionId tidak boleh di-reset ke null", missingTargetId, memSnapshot.activeSessionId)
        assertEquals("sampleBuffer tidak boleh dihapus", 4, memSnapshot.bufferedSamplesCount)
        assertEquals("accumulatedMah tidak boleh di-reset", 640.0, memSnapshot.accumulatedMah, 0.001)
        assertEquals("sessionSeconds tidak boleh di-reset", 1440L, memSnapshot.sessionSeconds)
        assertEquals("maxSessionTemp tidak boleh di-reset", 38.4f, memSnapshot.maxSessionTemp, 0.01f)
        assertEquals("currentSumForAvg tidak boleh di-reset", 2_304_000L, memSnapshot.currentSumForAvg)
        assertEquals("currentReadingsCount tidak boleh di-reset", 1440L, memSnapshot.currentReadingsCount)
        assertEquals(setOf("FAKE_FAST_CHARGING"), memSnapshot.alertedAnomalyCodes)
        assertTrue(memSnapshot.hasIncompleteRecoverySnapshot)
        assertTrue(memSnapshot.recoveryRequiresFollowUp)
    }

    /**
     * Skenario 13: Buffer sampel dan seluruh accumulator tetap utuh setelah kegagalan finalisasi utama + fallback TARGET_MISSING.
     */
    @Test
    fun test13_bufferAndAccumulatorsRemainIntactAfterFailure() = runTest {
        val missingSessionId = 65_432L
        val batchTs = 1_700_000_220_000L
        val sixSamples = (1..6).map { i ->
            sampleLiveState(
                level = 50 + i,
                currentMa = 1500 + (i * 25),
                sessionMah = 130.0 * i,
                sessionDurationSec = 300L * i
            )
        }
        service.seedActiveSessionStateForTesting(
            sessionId = missingSessionId,
            accumulatedMah = 780.25,
            sessionSeconds = 1800L,
            maxSessionTemp = 40.1f,
            currentSumForAvg = 2_808_900L,
            currentReadingsCount = 1800L,
            alertedAnomalyCodes = setOf("NET_DRAIN_CHARGING"),
            bufferedSamples = sixSamples,
            pendingBatchTimestampMs = batchTs
        )

        faultDao.failFinalizeTarget = true
        val finalizationResult = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP, finalizationResult)

        val snapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals(missingSessionId, snapshot.activeSessionId)
        assertEquals(6, snapshot.bufferedSamplesCount)
        assertEquals(batchTs, snapshot.pendingBatchTimestampMs)
        assertEquals(780.25, snapshot.accumulatedMah, 0.001)
        assertEquals(1800L, snapshot.sessionSeconds)
        assertEquals(40.1f, snapshot.maxSessionTemp, 0.01f)
        assertEquals(2_808_900L, snapshot.currentSumForAvg)
        assertEquals(1800L, snapshot.currentReadingsCount)
        assertEquals(setOf("NET_DRAIN_CHARGING"), snapshot.alertedAnomalyCodes)
        assertEquals(6, snapshot.incompleteRecoveryBufferedCount)

        val persisted = SessionRecoveryStore.loadOrNull(context)
        assertNotNull(persisted)
        assertEquals(780.25, persisted!!.accumulatedMah, 0.001)
        assertEquals(1800L, persisted.sessionSeconds)
        assertTrue(persisted.hasPendingSample)
    }

    /**
     * Skenario 14: Kondisi database tidak dapat diakses dibedakan secara eksplisit dari target session hilang.
     */
    @Test
    fun test14_databaseInaccessible_distinguishedFromTargetSessionMissing() = runTest {
        val existingSessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 900_000L,
                startLevel = 35,
                totalMahDelivered = 250.0,
                chargerType = "AC Adapter"
            )
        )
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = existingSessionId,
                    accumulatedMah = 490.0
                )
            )
        )

        faultDao.failSessionLookup = true
        val dbDownStatus = service.reconcilePersistedRecoveryForTesting()
        assertEquals(
            "Database error harus terdeteksi sebagai DATABASE_INACCESSIBLE, bukan target missing",
            BatteryMonitorService.PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE,
            dbDownStatus
        )
        val checkpointDuringDbDown = SessionRecoveryStore.loadOrNull(context)
        assertNotNull(checkpointDuringDbDown)
        assertEquals(
            SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
            checkpointDuringDbDown!!.recoveryStatus
        )

        faultDao.failSessionLookup = false
        assertTrue(SessionRecoveryStore.clear(context, expectedSessionId = existingSessionId))
        val trulyMissingId = 44_444L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = trulyMissingId,
                    accumulatedMah = 510.0
                )
            )
        )
        val missingStatus = service.reconcilePersistedRecoveryForTesting()
        assertEquals(
            "Row hilang pada DB sehat harus terdeteksi sebagai TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP",
            BatteryMonitorService.PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP,
            missingStatus
        )
        val checkpointMissing = SessionRecoveryStore.loadOrNull(context)
        assertNotNull(checkpointMissing)
        assertEquals(
            SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
            checkpointMissing!!.recoveryStatus
        )
    }

    /**
     * Skenario 15: Retry berulang saat target hilang maupun setelah target tersedia kembali tidak menyebabkan kehilangan/duplikasi data.
     */
    @Test
    fun test15_repeatedRetries_doNotLoseOrDuplicateData() = runTest {
        val targetSessionId = 33_333L
        val batchTs = 1_700_000_230_000L
        val buffered = listOf(
            sampleLiveState(level = 45, currentMa = 1500, sessionMah = 170.0, sessionDurationSec = 400L),
            sampleLiveState(level = 46, currentMa = 1500, sessionMah = 340.0, sessionDurationSec = 800L),
            sampleLiveState(level = 47, currentMa = 1500, sessionMah = 515.0, sessionDurationSec = 1200L)
        )
        service.seedActiveSessionStateForTesting(
            sessionId = targetSessionId,
            accumulatedMah = 515.0,
            sessionSeconds = 1200L,
            maxSessionTemp = 37.8f,
            currentSumForAvg = 1_800_000L,
            currentReadingsCount = 1200L,
            alertedAnomalyCodes = setOf("SLOW_CHARGING"),
            bufferedSamples = buffered,
            pendingBatchTimestampMs = batchTs
        )

        repeat(3) {
            val status = service.executeFallbackSessionClosureForTesting()
            assertEquals(BatteryMonitorService.FallbackClosureStatus.TARGET_MISSING_FAILED, status)
            val retryResult = service.retryPendingCheckpointAndFinalizationForTest()
            assertFalse(retryResult)
        }

        val midSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals(3, midSnapshot.bufferedSamplesCount)
        assertEquals(515.0, midSnapshot.accumulatedMah, 0.001)
        val midCheckpoint = SessionRecoveryStore.loadOrNull(context)
        assertNotNull(midCheckpoint)
        assertEquals(515.0, midCheckpoint!!.accumulatedMah, 0.001)

        faultDao.insertSession(
            ChargingSessionEntity(
                id = targetSessionId,
                startTime = System.currentTimeMillis() - 1_200_000L,
                startLevel = 30,
                totalMahDelivered = 200.0,
                chargerType = "AC Adapter"
            )
        )

        assertTrue(service.retryPendingCheckpointAndFinalizationForTest())
        assertTrue(service.retryPendingCheckpointAndFinalizationForTest())

        val recoveredRow = faultDao.getSessionById(targetSessionId)!!
        assertEquals("mAh tidak boleh terduplikasi pada retry berulang", 515.0, recoveredRow.totalMahDelivered, 0.001)
        assertEquals("Sampel tidak boleh terduplikasi pada retry berulang", 1, faultDao.countSamplesAtTimestamp(targetSessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 16: Session lain yang tidak terkait tetap tidak berubah saat menangani orphan checkpoint maupun fallback TARGET_MISSING.
     */
    @Test
    fun test16_unrelatedSessions_remainCompletelyUnchanged() = runTest {
        val sampleTsUnrelated = 1_700_000_240_000L
        val unrelatedOpenId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 600_000L,
                startLevel = 28,
                totalMahDelivered = 340.0,
                avgCurrentMa = 1650,
                maxTempCelsius = 37.0f,
                chargerType = "FAST (AC Adapter)"
            )
        )
        faultDao.insertSample(
            BatterySampleEntity(
                sessionId = unrelatedOpenId,
                timestamp = sampleTsUnrelated,
                batteryLevel = 35,
                currentMa = 1650,
                voltageMv = 4200,
                tempCelsius = 37.0f,
                powerWatt = 6.93f,
                isScreenOn = false
            )
        )

        val closedEndTs = 1_700_000_245_000L
        val unrelatedClosedId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = closedEndTs - 1_800_000L,
                endTime = closedEndTs,
                startLevel = 20,
                endLevel = 85,
                totalMahDelivered = 1920.0,
                avgCurrentMa = 2100,
                maxTempCelsius = 38.8f,
                chargerType = "AC Adapter",
                anomalyReason = "THERMAL_THROTTLING",
                isAnomaly = true
            )
        )

        val orphanSessionId = 99_123L
        service.seedActiveSessionStateForTesting(
            sessionId = orphanSessionId,
            accumulatedMah = 410.0,
            sessionSeconds = 900L,
            maxSessionTemp = 38.0f,
            currentSumForAvg = 1_350_000L,
            currentReadingsCount = 900L,
            bufferedSamples = listOf(sampleLiveState(level = 50, sessionMah = 410.0, sessionDurationSec = 900L)),
            pendingBatchTimestampMs = 1_700_000_250_000L
        )

        assertEquals(
            BatteryMonitorService.FallbackClosureStatus.TARGET_MISSING_FAILED,
            service.executeFallbackSessionClosureForTesting()
        )
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP,
            service.reconcilePersistedRecoveryForTesting()
        )

        val openAfter = faultDao.getSessionById(unrelatedOpenId)!!
        assertNull("Sesi terbuka lain tidak boleh ikut ditutup", openAfter.endTime)
        assertEquals(340.0, openAfter.totalMahDelivered, 0.001)
        assertEquals(1650, openAfter.avgCurrentMa)
        assertEquals(37.0f, openAfter.maxTempCelsius, 0.01f)
        assertEquals(1, faultDao.countSamplesAtTimestamp(unrelatedOpenId, sampleTsUnrelated))

        val closedAfter = faultDao.getSessionById(unrelatedClosedId)!!
        assertEquals(closedEndTs, closedAfter.endTime)
        assertEquals(85, closedAfter.endLevel)
        assertEquals(1920.0, closedAfter.totalMahDelivered, 0.001)
        assertEquals(2100, closedAfter.avgCurrentMa)
        assertEquals(38.8f, closedAfter.maxTempCelsius, 0.01f)
        assertEquals("THERMAL_THROTTLING", closedAfter.anomalyReason)

        assertNotNull(SessionRecoveryStore.loadOrNull(context))
    }

    /**
     * Skenario 17 (v5 P0): Multi-session keyed-by-sessionId isolasi kepemilikan & pencegahan overwrite/clear lintas sesi.
     * Checkpoint milik sessionA tidak pernah ditimpa atau dihapus oleh sessionB, dan sesi baru dapat dimulai
     * secara terisolasi tanpa merusak checkpoint follow-up milik sessionA.
     */
    @Test
    fun test17_unresolvedCheckpointOwnership_isolatesSessionsAndBlocksCrossSessionOverwriteOrClear() = runTest {
        val ownerSessionId = 101L
        val otherSessionId = 202L
        val ownerCheckpoint = buildRecoveryCheckpoint(
            sessionId = ownerSessionId,
            accumulatedMah = 710.0,
            sessionSeconds = 1500L
        )
        assertTrue(SessionRecoveryStore.save(context, ownerCheckpoint))

        // Penulisan lintas sesi dengan expectedOwnerSessionId berbeda wajib ditolak
        val crossOwnerWrite = SessionRecoveryStore.saveForSession(
            context = context,
            expectedOwnerSessionId = ownerSessionId,
            recovery = buildRecoveryCheckpoint(
                sessionId = otherSessionId,
                accumulatedMah = 120.0,
                sessionSeconds = 200L
            )
        )
        assertEquals(SessionRecoveryStore.SaveStatus.REJECTED_OWNERSHIP_CONFLICT, crossOwnerWrite.status)
        assertEquals(ownerSessionId, crossOwnerWrite.existingOwnerSessionId)

        // Menyimpan checkpoint untuk sessionId=202 tersimpan di slot terisolasi miliknya sendiri tanpa menimpa 101
        val secondSessionSave = SessionRecoveryStore.saveForSession(
            context = context,
            expectedOwnerSessionId = otherSessionId,
            recovery = buildRecoveryCheckpoint(
                sessionId = otherSessionId,
                accumulatedMah = 240.0,
                sessionSeconds = 450L
            )
        )
        assertEquals(SessionRecoveryStore.SaveStatus.SAVED, secondSessionSave.status)

        // Verifikasi kedua checkpoint tersimpan secara terpisah dan utuh 100%
        val preservedOwner = SessionRecoveryStore.loadForSessionOrNull(context, ownerSessionId)
        val preservedOther = SessionRecoveryStore.loadForSessionOrNull(context, otherSessionId)
        assertNotNull(preservedOwner)
        assertNotNull(preservedOther)
        assertEquals(710.0, preservedOwner!!.accumulatedMah, 0.001)
        assertEquals(240.0, preservedOther!!.accumulatedMah, 0.001)

        // Menghapus sessionId=202 hanya menghapus 202 dan mempertahankan 101 sepenuhnya
        assertTrue(SessionRecoveryStore.clear(context, expectedSessionId = otherSessionId))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.loadForSession(context, otherSessionId))
        assertEquals(710.0, SessionRecoveryStore.loadForSessionOrNull(context, ownerSessionId)!!.accumulatedMah, 0.001)

        // recoverOrStartChargingSession merekonsiliasi 101 (menandai NEEDS_FOLLOW_UP_TARGET_MISSING) lalu memulai sesi baru
        // secara terisolasi tanpa menimpa atau menghapus checkpoint 101
        service.recoverOrStartChargingSessionForTesting(sampleLiveState(level = 55))
        val newActiveId = service.getInMemorySessionSnapshotForTesting().activeSessionId
        assertNotNull("Sesi baru wajib dimulai secara terisolasi", newActiveId)
        assertEquals(ownerSessionId, SessionRecoveryStore.loadForSessionOrNull(context, ownerSessionId)?.sessionId)
        assertEquals(
            SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
            SessionRecoveryStore.loadForSessionOrNull(context, ownerSessionId)?.recoveryStatus
        )
    }

    /**
     * Skenario 18 (v4 P0): Kegagalan penyimpanan checkpoint mencegah shutdown service (RECOVERY_HOLD)
     * ketika data relevan hanya ada di RAM, dan layanan baru berhenti setelah persistensi terkonfirmasi.
     */
    @Test
    fun test18_failedCheckpointCapture_preventsServiceShutdownWhenDataExistsOnlyInRam() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 300_000L,
                startLevel = 40,
                chargerType = "AC Adapter"
            )
        )
        service.setActorRunningForTesting(charging = true, generation = 1L)
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 350.0,
            sessionSeconds = 300L,
            maxSessionTemp = 37.0f,
            currentSumForAvg = 450_000L,
            currentReadingsCount = 300L,
            bufferedSamples = listOf(sampleLiveState(level = 45, sessionMah = 350.0, sessionDurationSec = 300L)),
            pendingBatchTimestampMs = 1_700_000_300_000L
        )

        // Simulasikan Room gagal DAN penyimpanan checkpoint gagal saat stop command diterima
        faultDao.failFinalizeTarget = true
        var blockCheckpointSave = true
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            private var stored: SessionRecoveryStore.PersistedSessionRecovery? = null

            override fun saveDetailed(
                context: Context,
                recovery: SessionRecoveryStore.PersistedSessionRecovery
            ): SessionRecoveryStore.CheckpointSaveResult {
                if (blockCheckpointSave) {
                    return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.COMMIT_FAILED)
                }
                stored = recovery
                return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.SAVED)
            }

            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                return stored?.let { SessionRecoveryStore.CheckpointLoadResult.Loaded(it) }
                    ?: SessionRecoveryStore.CheckpointLoadResult.Empty
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean {
                if (stored?.sessionId == expectedSessionId) stored = null
                return true
            }

            override fun clearAllForTesting(context: Context): Boolean {
                stored = null
                return true
            }
        })

        service.handleStopCommandForTesting(stopStartId = 1, stopGeneration = 1L)

        // Service wajib tetap berjalan dalam RECOVERY_HOLD dan data RAM tetap utuh
        assertTrue("Service tidak boleh shutdown ketika data hanya ada di RAM", BatteryMonitorService.isRunning)
        val holdSnapshot = service.getInMemorySessionSnapshotForTesting()
        assertTrue(holdSnapshot.isInRecoveryHold)
        assertEquals(sessionId, holdSnapshot.activeSessionId)
        assertEquals(1, holdSnapshot.bufferedSamplesCount)
        assertEquals(350.0, holdSnapshot.accumulatedMah, 0.001)

        // Setelah penyimpanan DB & checkpoint pulih, stop berikutnya menyelesaikan finalisasi dan berhenti bersih
        blockCheckpointSave = false
        faultDao.failFinalizeTarget = false
        service.handleStopCommandForTesting(stopStartId = 2, stopGeneration = 1L)

        assertFalse("Service berhenti bersih setelah data terkonfirmasi persisten", BatteryMonitorService.isRunning)
        assertNotNull(faultDao.getSessionById(sessionId)!!.endTime)
    }

    /**
     * Skenario 19 (v4 P0): Database unavailable tidak menghapus checkpoint dan mengembalikan DATABASE_UNAVAILABLE.
     */
    @Test
    fun test19_databaseUnavailable_doesNotClearCheckpoint() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 600_000L,
                startLevel = 30,
                totalMahDelivered = 200.0,
                chargerType = "AC Adapter"
            )
        )
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(sessionId = sessionId, accumulatedMah = 480.0)
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 480.0,
            sessionSeconds = 600L,
            maxSessionTemp = 37.5f,
            currentSumForAvg = 900_000L,
            currentReadingsCount = 600L
        )

        faultDao.failSessionLookup = true
        val status = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.DATABASE_UNAVAILABLE, status)
        val retained = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Checkpoint wajib tetap tersimpan saat database tidak tersedia", retained)
        assertEquals(SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE, retained!!.recoveryStatus)
    }

    /**
     * Skenario 20 (v4 P0): Target genuinely missing tidak menghapus checkpoint dan mengembalikan TARGET_MISSING_NEEDS_FOLLOW_UP.
     */
    @Test
    fun test20_targetGenuinelyMissing_doesNotClearCheckpoint() = runTest {
        val missingId = 55_111L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(sessionId = missingId, accumulatedMah = 390.0)
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = missingId,
            accumulatedMah = 390.0,
            sessionSeconds = 500L,
            maxSessionTemp = 36.5f,
            currentSumForAvg = 750_000L,
            currentReadingsCount = 500L
        )

        val status = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP, status)
        val retained = SessionRecoveryStore.loadOrNull(context)
        assertNotNull("Checkpoint wajib dipertahankan saat target hilang di Room", retained)
        assertEquals(missingId, retained!!.sessionId)
        assertEquals(SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING, retained.recoveryStatus)
    }

    /**
     * Skenario 21 (v4 P0): Sesi yang sudah tertutup di Room (endTime != null) namun masih memiliki
     * sampel/checkpoint tertunda tetap direkonsiliasi; kegagalan rekonsiliasi mencegah penghapusan
     * checkpoint maupun reset state memori.
     */
    @Test
    fun test21_alreadyClosedRoomSession_reconcilesPendingSamplesAndBlocksClearOnReconcileFailure() = runTest {
        val closedTs = System.currentTimeMillis() - 60_000L
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = closedTs - 600_000L,
                endTime = closedTs,
                startLevel = 30,
                endLevel = 55,
                totalMahDelivered = 400.0,
                avgCurrentMa = 1500,
                maxTempCelsius = 37.0f,
                chargerType = "AC Adapter"
            )
        )
        val pendingBatchTs = 1_700_000_310_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 610.0,
                    batchTimestampMs = pendingBatchTs,
                    dbSessionsClosed = true
                )
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 610.0,
            sessionSeconds = 600L,
            maxSessionTemp = 38.2f,
            currentSumForAvg = 900_000L,
            currentReadingsCount = 600L,
            bufferedSamples = listOf(sampleLiveState(level = 55, sessionMah = 610.0, sessionDurationSec = 600L)),
            pendingBatchTimestampMs = pendingBatchTs
        )

        // Saat rekonsiliasi gagal, checkpoint dan buffer in-memory TIDAK boleh dihapus!
        faultDao.failReconcile = true
        faultDao.failFinalizeTarget = true
        faultDao.failSingleSessionClose = true
        val failedAttempt = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.FAILED, failedAttempt)
        assertNotNull("Checkpoint tidak boleh terhapus saat rekonsiliasi sesi tertutup gagal", SessionRecoveryStore.loadOrNull(context))
        assertEquals(1, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)

        // Setelah DB sehat, finalisasi sesi tertutup merekonsiliasi sampel & metrik tertunda sebelum membersihkan checkpoint
        faultDao.failReconcile = false
        faultDao.failFinalizeTarget = false
        faultDao.failSingleSessionClose = false
        val succeededAttempt = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.ALREADY_CLOSED_IDEMPOTENT, succeededAttempt)

        val updated = faultDao.getSessionById(sessionId)!!
        assertEquals(610.0, updated.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, pendingBatchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
        assertEquals(0, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)
    }

    /**
     * Skenario 22 (v4 P0): Penghapusan checkpoint hanya membersihkan checkpoint milik sessionId yang sesuai
     * dan menolak penghapusan checkpoint milik sesi lain.
     */
    @Test
    fun test22_clearCheckpoint_clearsOnlyMatchingSessionId() = runTest {
        val ownerId = 501L
        val wrongId = 502L
        assertTrue(SessionRecoveryStore.save(context, buildRecoveryCheckpoint(sessionId = ownerId, accumulatedMah = 420.0)))

        // Mencoba menghapus dengan sessionId berbeda wajib gagal dan mempertahankan checkpoint pemilik
        assertFalse(SessionRecoveryStore.clear(context, expectedSessionId = wrongId))
        assertEquals(ownerId, SessionRecoveryStore.loadOrNull(context)?.sessionId)

        // Menghapus dengan sessionId pemilik wajib berhasil dan mengembalikan Empty
        assertTrue(SessionRecoveryStore.clear(context, expectedSessionId = ownerId))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 23 (v4 P0): Retry yang berhasil hanya menghapus sampel yang terbukti tersimpan di Room
     * dan mempertahankan sampel baru yang masuk setelahnya.
     */
    @Test
    fun test23_successfulRetry_removesOnlyProvenPersistedSamples() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 120_000L,
                startLevel = 40,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_320_000L
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 250.0,
            sessionSeconds = 120L,
            maxSessionTemp = 36.5f,
            currentSumForAvg = 180_000L,
            currentReadingsCount = 120L,
            bufferedSamples = listOf(
                sampleLiveState(level = 41, sessionMah = 125.0, sessionDurationSec = 60L),
                sampleLiveState(level = 42, sessionMah = 250.0, sessionDurationSec = 120L)
            ),
            pendingBatchTimestampMs = batchTs,
            samplePersistenceHalted = true
        )

        assertTrue(service.retryPendingCheckpointAndFinalizationForTest())
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(0, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)

        // Tambahkan sampel baru setelah batch pertama dipersistensikan -> sampel baru tetap aman di buffer
        service.appendSampleToBufferForTesting(sampleLiveState(level = 43, sessionMah = 260.0, sessionDurationSec = 121L))
        assertEquals(1, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)
    }

    /**
     * Skenario 24 (v4 P0): Retry yang berhasil memulihkan samplePersistenceHalted = false saat DB sehat
     * dan memastikan layanan tidak tertinggal macet.
     */
    @Test
    fun test24_successfulRetry_restoresSamplePersistenceHaltedAndResumesSafely() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 90_000L,
                startLevel = 50,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_330_000L
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 180.0,
            sessionSeconds = 90L,
            maxSessionTemp = 36.0f,
            currentSumForAvg = 135_000L,
            currentReadingsCount = 90L,
            bufferedSamples = listOf(sampleLiveState(level = 51, sessionMah = 180.0, sessionDurationSec = 90L)),
            pendingBatchTimestampMs = batchTs,
            samplePersistenceHalted = true
        )

        assertTrue(service.retryPendingCheckpointAndFinalizationForTest())
        val snapshot = service.getInMemorySessionSnapshotForTesting()
        assertFalse("samplePersistenceHalted wajib dipulihkan ke false saat DB sehat", snapshot.samplePersistenceHalted)
        assertEquals(0, snapshot.consecutiveFlushFailures)
        assertEquals(0, snapshot.bufferedSamplesCount)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
    }

    /**
     * Regression: read-back checkpoint harus mempertahankan seluruh payload, termasuk field
     * yang sebelumnya tidak ikut diverifikasi setelah SharedPreferences.commit().
     */
    @Test
    fun test_checkpointReadBack_preservesEveryPersistedField() {
        val sessionId = 987654321L
        val expected = buildRecoveryCheckpoint(
            sessionId = sessionId,
            accumulatedMah = 731.25,
            sessionSeconds = 2345L,
            maxTemp = 41.25f,
            currentSum = -987654L,
            currentCount = 321L,
            anomalyCodes = setOf("SLOW_CHARGING", "HIGH_TEMPERATURE"),
            batchTimestampMs = 1_700_000_345_678L,
            sampleWeight = 7,
            dbSessionsClosed = true
        ).copy(
            sampleBatteryLevel = 63,
            sampleCurrentMa = -1725,
            sampleVoltageMv = 4187,
            sampleTempCelsius = 39.75f,
            samplePowerWatt = -7.224f,
            sampleIsScreenOn = true,
            sampleSpanDurationMs = 4567L,
            failureReason = "full payload verification",
            recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE
        )

        assertTrue(SessionRecoveryStore.saveForSession(context, sessionId, expected).isSuccess)
        val loaded = SessionRecoveryStore.loadForSession(context, sessionId)
            as SessionRecoveryStore.CheckpointLoadResult.Loaded

        assertEquals(expected, loaded.recovery)
    }

    /**
     * Skenario 25 (v4 P0): Rekonsiliasi berulang tidak menduplikasi sampel maupun menggandakan metrik kumulatif.
     */
    @Test
    fun test25_repeatedReconciliation_doesNotDuplicateSamplesOrDoubleCountMetrics() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 600_000L,
                startLevel = 25,
                totalMahDelivered = 300.0,
                avgCurrentMa = 1400,
                maxTempCelsius = 36.0f,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_340_000L
        val checkpoint = buildRecoveryCheckpoint(
            sessionId = sessionId,
            accumulatedMah = 650.0,
            sessionSeconds = 600L,
            maxTemp = 38.0f,
            currentSum = 960_000L,
            currentCount = 600L,
            anomalyCodes = setOf("SLOW_CHARGING"),
            batchTimestampMs = batchTs
        )

        repeat(4) {
            assertTrue(SessionRecoveryStore.save(context, checkpoint))
            assertEquals(
                BatteryMonitorService.PersistedRecoveryReconcileStatus.RECONCILED_AND_CLEARED,
                service.reconcilePersistedRecoveryForTesting()
            )
        }

        val row = faultDao.getSessionById(sessionId)!!
        assertEquals(650.0, row.totalMahDelivered, 0.001)
        assertEquals(1600, row.avgCurrentMa)
        assertEquals(38.0f, row.maxTempCelsius, 0.01f)
        assertEquals("SLOW_CHARGING", row.anomalyReason)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
    }

    /**
     * Skenario 26 (v4 P0): Restart proses di tengah recovery mempertahankan data dan menyelesaikan rekonsiliasi idempotent.
     */
    @Test
    fun test26_processRestartDuringRecovery_preservesDataAndCompletesIdempotently() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 1_000_000L,
                startLevel = 30,
                totalMahDelivered = 250.0,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_350_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 580.0,
                    batchTimestampMs = batchTs
                )
            )
        )

        // Simulasikan transaksi Room terhenti di tengah jalan akibat restart proses
        faultDao.failReconcile = true
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE,
            service.reconcilePersistedRecoveryForTesting()
        )
        assertNotNull(SessionRecoveryStore.loadOrNull(context))

        // Simulasikan instance Service baru setelah restart proses dengan DB kembali normal
        faultDao.failReconcile = false
        val restartedController = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val restartedService = restartedController.get()
        restartedService.daoOverride = faultDao
        try {
            assertEquals(
                BatteryMonitorService.PersistedRecoveryReconcileStatus.RECONCILED_AND_CLEARED,
                restartedService.reconcilePersistedRecoveryForTesting()
            )
            val row = faultDao.getSessionById(sessionId)!!
            assertEquals(580.0, row.totalMahDelivered, 0.001)
            assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
            assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
        } finally {
            runCatching { restartedController.destroy() }
        }
    }

    /**
     * Skenario 27 (v4 P0): Sinkronisasi deterministik menggunakan barrier eksplisit antara
     * cleanup PowerConnectionReceiver dan finalisasi BatteryMonitorService.
     */
    @Test
    fun test27_receiverAndServiceCleanup_synchronizedDeterministicallyWithBarriers() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 800_000L,
                startLevel = 35,
                totalMahDelivered = 400.0,
                chargerType = "AC Adapter"
            )
        )
        val batchTs = 1_700_000_360_000L
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = sessionId,
                    accumulatedMah = 820.0,
                    batchTimestampMs = batchTs
                )
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 820.0,
            sessionSeconds = 800L,
            maxSessionTemp = 38.0f,
            currentSumForAvg = 1_200_000L,
            currentReadingsCount = 800L,
            bufferedSamples = listOf(sampleLiveState(level = 60, sessionMah = 820.0, sessionDurationSec = 800L)),
            pendingBatchTimestampMs = batchTs
        )

        val reconcileEntered = CompletableDeferred<Unit>()
        val allowProceed = CompletableDeferred<Unit>()
        faultDao.onReconcileEntered = reconcileEntered
        faultDao.allowReconcileProceed = allowProceed

        val serviceJob = async { service.ensureSessionFinalizedSafelyForTesting() }
        reconcileEntered.await()

        val receiver = PowerConnectionReceiver()
        val receiverJob = async {
            receiver.executeGuardedBackgroundCleanupNow(
                context = context,
                daoOverride = faultDao,
                isChargingOverride = false,
                batteryLevelOverride = 60
            )
        }

        // Lepaskan barrier secara deterministik setelah kedua jalur aktif
        allowProceed.complete(Unit)
        listOf(serviceJob, receiverJob).awaitAll()

        val finalRow = faultDao.getSessionById(sessionId)!!
        assertNotNull(finalRow.endTime)
        assertEquals(820.0, finalRow.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, batchTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.load(context))
    }

    /**
     * Skenario 28 (v4 P1): Pemadatan buffer terbatas (>300 sampel) mempertahankan bobot sampel
     * dan menghasilkan rata-rata tertimbang yang akurat secara matematis.
     */
    @Test
    fun test28_boundedBufferCompaction_preservesSampleWeightsAndWeightedAverages() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 400_000L,
                startLevel = 20,
                chargerType = "FAST (AC Adapter)"
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 500.0,
            sessionSeconds = 301L,
            maxSessionTemp = 38.0f,
            currentSumForAvg = 452_000L,
            currentReadingsCount = 301L
        )

        // 150 sampel pertama pada 1000 mA, 151 sampel berikutnya pada 2000 mA -> memicu pemadatan 150 sampel pertama
        repeat(150) { idx ->
            service.appendSampleToBufferForTesting(
                sampleLiveState(level = 25, currentMa = 1000, voltageMv = 4000, tempCelsius = 34.0f, sessionDurationSec = idx.toLong() + 1)
            )
        }
        repeat(151) { idx ->
            service.appendSampleToBufferForTesting(
                sampleLiveState(level = 35, currentMa = 2000, voltageMv = 4200, tempCelsius = 38.0f, sessionDurationSec = 151L + idx)
            )
        }

        val snapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals("Total bobot sampel wajib tetap 301 pasca pemadatan", 301, snapshot.bufferedTotalSampleWeight)
        assertEquals("Ukuran entri buffer wajib terkompaksi menjadi 152", 152, snapshot.bufferedSamplesCount)

        service.flushSampleBufferForTesting(isScreenOn = false)
        val persistedSamples = faultDao.getSamplesForSessionList(sessionId)
        assertEquals(1, persistedSamples.size)

        // Rata-rata tertimbang eksak: (150 * 1000 + 151 * 2000) / 301 = 452000 / 301 = 1501 mA
        // (Tanpa bobot, rata-rata naif akan menghasilkan 1993 mA)
        val expectedWeightedMa = (150L * 1000L + 151L * 2000L) / 301L
        assertEquals(expectedWeightedMa.toInt(), persistedSamples.first().currentMa)
    }

    /**
     * Skenario 29 (v5 P0): Multi-session keyed-by-sessionId menyimpan banyak sesi tanpa saling menimpa,
     * menolak operasi clear/save lintas sesi, dan mengisolasi penghapusan per sessionId.
     */
    @Test
    fun test29_multipleSessions_isolatedBySessionIdWithoutCrossSessionOverwriteOrClear() = runTest {
        val sessionA = 1001L
        val sessionB = 1002L
        val sessionC = 1003L

        assertEquals(
            SessionRecoveryStore.SaveStatus.SAVED,
            SessionRecoveryStore.saveDetailed(context, buildRecoveryCheckpoint(sessionId = sessionA, accumulatedMah = 410.0)).status
        )
        assertEquals(
            SessionRecoveryStore.SaveStatus.SAVED,
            SessionRecoveryStore.saveDetailed(context, buildRecoveryCheckpoint(sessionId = sessionB, accumulatedMah = 220.0)).status
        )
        assertEquals(
            SessionRecoveryStore.SaveStatus.SAVED,
            SessionRecoveryStore.saveDetailed(context, buildRecoveryCheckpoint(sessionId = sessionC, accumulatedMah = 330.0)).status
        )

        // Penolakan write lintas sesi
        assertEquals(
            SessionRecoveryStore.SaveStatus.REJECTED_OWNERSHIP_CONFLICT,
            SessionRecoveryStore.saveForSession(
                context = context,
                expectedOwnerSessionId = sessionA,
                recovery = buildRecoveryCheckpoint(sessionId = sessionB, accumulatedMah = 999.0)
            ).status
        )

        // Penolakan clear dengan sessionId yang tidak terdaftar tidak memengaruhi A, B, maupun C
        assertTrue(SessionRecoveryStore.clear(context, expectedSessionId = 9999L))
        assertEquals(3, SessionRecoveryStore.loadAllLoaded(context).size)
        assertEquals(410.0, SessionRecoveryStore.loadForSessionOrNull(context, sessionA)!!.accumulatedMah, 0.001)
        assertEquals(220.0, SessionRecoveryStore.loadForSessionOrNull(context, sessionB)!!.accumulatedMah, 0.001)
        assertEquals(330.0, SessionRecoveryStore.loadForSessionOrNull(context, sessionC)!!.accumulatedMah, 0.001)

        // Hapus hanya sessionB -> sessionA dan sessionC tetap utuh
        assertTrue(SessionRecoveryStore.clear(context, expectedSessionId = sessionB))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.loadForSession(context, sessionB))
        assertEquals(410.0, SessionRecoveryStore.loadForSessionOrNull(context, sessionA)!!.accumulatedMah, 0.001)
        assertEquals(330.0, SessionRecoveryStore.loadForSessionOrNull(context, sessionC)!!.accumulatedMah, 0.001)
    }

    /**
     * Skenario 30 (v5 P1): Klasifikasi kegagalan penyimpanan membedakan EMPTY, LOADED, CORRUPT, dan UNAVAILABLE,
     * serta tidak memperlakukan data korup atau exception penyimpanan sebagai EMPTY.
     */
    @Test
    fun test30_storageFailureClassification_distinguishesEmptyLoadedCorruptAndUnavailable() = runTest {
        // 1. EMPTY
        assertEquals(SessionRecoveryStore.LoadStatus.EMPTY, SessionRecoveryStore.load(context).status)

        // 2. LOADED
        assertTrue(SessionRecoveryStore.save(context, buildRecoveryCheckpoint(sessionId = 901L, accumulatedMah = 300.0)))
        val loadedResult = SessionRecoveryStore.loadForSession(context, 901L)
        assertEquals(SessionRecoveryStore.LoadStatus.LOADED, loadedResult.status)
        assertEquals(901L, loadedResult.getOrNull()?.sessionId)

        // 3. CORRUPT pada bucket sessionId=902 (s_902_has_record=true tetapi s_902_session_id tidak valid)
        val rawPrefs = context.getSharedPreferences("powerguard_session_recovery", Context.MODE_PRIVATE)
        assertTrue(
            rawPrefs.edit()
                .putBoolean("s_902_has_record", true)
                .putLong("s_902_session_id", -5L)
                .commit()
        )
        val corruptResult = SessionRecoveryStore.loadForSession(context, 902L)
        assertEquals(SessionRecoveryStore.LoadStatus.CORRUPT, corruptResult.status)
        assertTrue(corruptResult is SessionRecoveryStore.CheckpointLoadResult.Corrupt)

        // Save baru tidak boleh menimpa checkpoint CORRUPT sessionId=902 secara diam-diam
        val saveOverCorrupt = SessionRecoveryStore.saveDetailed(context, buildRecoveryCheckpoint(sessionId = 902L, accumulatedMah = 150.0))
        assertEquals(SessionRecoveryStore.SaveStatus.REJECTED_EXISTING_CORRUPT, saveOverCorrupt.status)

        // Sementara sessionId=901 tetap LOADED dan tidak terblokir oleh korupsi pada 902
        assertEquals(SessionRecoveryStore.LoadStatus.LOADED, SessionRecoveryStore.loadForSession(context, 901L).status)

        // 4. UNAVAILABLE (exception pada penyimpanan tidak boleh dianggap EMPTY)
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                throw IllegalStateException("Simulated encrypted/device-protected storage unavailable")
            }

            override fun loadAllDetailed(context: Context): List<SessionRecoveryStore.CheckpointLoadResult> {
                throw IllegalStateException("Simulated encrypted/device-protected storage unavailable")
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean = false
        })
        val unavailableResult = SessionRecoveryStore.load(context)
        assertEquals(SessionRecoveryStore.LoadStatus.UNAVAILABLE, unavailableResult.status)
        assertTrue(unavailableResult is SessionRecoveryStore.CheckpointLoadResult.Unavailable)
    }

    /**
     * Skenario 31 (v5 P0): Rekonsiliasi multi-sesi mengisolasi sesi yang targetnya hilang dari sesi yang valid.
     * Sesi valid direkonsiliasi & dibersihkan, sementara sesi target hilang dipertahankan dengan status follow-up.
     */
    @Test
    fun test31_multiSessionRecovery_isolatesMissingTargetFromValidSessionReconciliation() = runTest {
        val missingSessionId = 81_001L
        val validSessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 900_000L,
                startLevel = 30,
                totalMahDelivered = 250.0,
                chargerType = "AC Adapter"
            )
        )
        val missingTs = 1_700_000_410_000L
        val validTs = 1_700_000_420_000L

        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = missingSessionId,
                    accumulatedMah = 640.0,
                    batchTimestampMs = missingTs
                )
            )
        )
        assertTrue(
            SessionRecoveryStore.save(
                context,
                buildRecoveryCheckpoint(
                    sessionId = validSessionId,
                    accumulatedMah = 780.0,
                    batchTimestampMs = validTs
                )
            )
        )

        val status = service.reconcilePersistedRecoveryForTesting()
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP,
            status
        )

        // Sesi valid wajib sudah direkonsiliasi ke Room dan checkpoint-nya dibersihkan
        val validRow = faultDao.getSessionById(validSessionId)!!
        assertEquals(780.0, validRow.totalMahDelivered, 0.001)
        assertEquals(1, faultDao.countSamplesAtTimestamp(validSessionId, validTs))
        assertEquals(SessionRecoveryStore.CheckpointLoadResult.Empty, SessionRecoveryStore.loadForSession(context, validSessionId))

        // Sesi target hilang wajib tetap tersimpan di SessionRecoveryStore dengan status follow-up
        val retainedMissing = SessionRecoveryStore.loadForSessionOrNull(context, missingSessionId)
        assertNotNull(retainedMissing)
        assertEquals(640.0, retainedMissing!!.accumulatedMah, 0.001)
        assertEquals(SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING, retainedMissing.recoveryStatus)
    }

    /**
     * Regression P0: snapshot incoming dengan count dan bobot sampel yang sama tidak boleh
     * mengganti statistik/payload existing hanya karena proses save dipanggil ulang.
     */
    @Test
    fun test_mergeSameSession_equalCountAndWeight_preservesExistingSnapshot() {
        val sessionId = 40_404L
        val timestamp = 1_700_000_777_000L
        val existing = buildRecoveryCheckpoint(
            sessionId = sessionId,
            accumulatedMah = 250.0,
            sessionSeconds = 120L,
            currentSum = 125_000L,
            currentCount = 50L,
            batchTimestampMs = timestamp,
            sampleWeight = 5
        ).copy(sampleBatteryLevel = 48, sampleCurrentMa = 1250)

        val staleIncoming = existing.copy(
            accumulatedMah = 240.0,
            sessionSeconds = 115L,
            currentSumForAvg = 99_000L,
            sampleBatteryLevel = 47,
            sampleCurrentMa = 990,
            failureReason = "stale retry"
        )

        assertTrue(SessionRecoveryStore.save(context, existing))
        assertTrue(SessionRecoveryStore.save(context, staleIncoming))

        val merged = SessionRecoveryStore.loadForSessionOrNull(context, sessionId)!!
        assertEquals(125_000L, merged.currentSumForAvg)
        assertEquals(50L, merged.currentReadingsCount)
        assertEquals(48, merged.sampleBatteryLevel)
        assertEquals(1250, merged.sampleCurrentMa)
        assertEquals(5, merged.sampleWeight)
        assertEquals(timestamp, merged.pendingBatchTimestampMs)
    }

    /**
     * Skenario 32 (v5 P1): Penggabungan dua batch sampel berbeda pada sessionId yang sama mempertahankan
     * total bobot tertimbang dan metrik gabungan tanpa kehilangan sampel batch sebelumnya.
     */
    @Test
    fun test32_multiBatchSameSessionMerge_preservesCombinedSampleWeightsAndTelemetry() = runTest {
        val sessionId = 404L
        val batch1 = SessionRecoveryStore.PersistedSessionRecovery(
            sessionId = sessionId,
            accumulatedMah = 200.0,
            sessionSeconds = 100L,
            maxSessionTemp = 36.0f,
            currentSumForAvg = 100_000L,
            currentReadingsCount = 100L,
            alertedAnomalyCodes = setOf("SLOW_CHARGING"),
            pendingBatchTimestampMs = 1_700_000_500_000L,
            hasPendingSample = true,
            sampleBatteryLevel = 45,
            sampleCurrentMa = 1000,
            sampleVoltageMv = 4000,
            sampleTempCelsius = 35.0f,
            samplePowerWatt = 4.0f,
            sampleIsScreenOn = false,
            sampleWeight = 100,
            sampleSpanDurationMs = 99_000L,
            dbSessionsClosed = false,
            failureReason = "Batch 1"
        )
        val batch2 = SessionRecoveryStore.PersistedSessionRecovery(
            sessionId = sessionId,
            accumulatedMah = 600.0,
            sessionSeconds = 200L,
            maxSessionTemp = 38.5f,
            currentSumForAvg = 300_000L,
            currentReadingsCount = 200L,
            alertedAnomalyCodes = setOf("THERMAL_THROTTLING"),
            pendingBatchTimestampMs = 1_700_000_600_000L,
            hasPendingSample = true,
            sampleBatteryLevel = 55,
            sampleCurrentMa = 2000,
            sampleVoltageMv = 4200,
            sampleTempCelsius = 38.0f,
            samplePowerWatt = 8.4f,
            sampleIsScreenOn = true,
            sampleWeight = 100,
            sampleSpanDurationMs = 99_000L,
            dbSessionsClosed = false,
            failureReason = "Batch 2"
        )

        assertTrue(SessionRecoveryStore.save(context, batch1))
        assertTrue(SessionRecoveryStore.save(context, batch2))

        val merged = SessionRecoveryStore.loadForSessionOrNull(context, sessionId)!!
        assertEquals(200, merged.sampleWeight)
        assertEquals(1_700_000_500_000L, merged.pendingBatchTimestampMs)
        assertEquals(199_000L, merged.sampleSpanDurationMs)
        assertEquals(1500, merged.sampleCurrentMa)
        assertEquals(4100, merged.sampleVoltageMv)
        assertEquals(36.5f, merged.sampleTempCelsius, 0.01f)
        assertEquals(55, merged.sampleBatteryLevel)
        assertEquals(setOf("SLOW_CHARGING", "THERMAL_THROTTLING"), merged.alertedAnomalyCodes)
    }

    /**
     * Skenario 33 (v5 P0): Sesi yang sudah tertutup di Room tetap merekonsiliasi kode anomali baru
     * dan sampel tertunda, serta menahan penghapusan state jika rekonsiliasi gagal.
     */
    @Test
    fun test33_alreadyClosedSession_reconcilesNewlyDetectedAnomalyCodesAndPendingSamples() = runTest {
        val closedTs = System.currentTimeMillis() - 30_000L
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = closedTs - 600_000L,
                endTime = closedTs,
                startLevel = 35,
                endLevel = 60,
                totalMahDelivered = 500.0,
                avgCurrentMa = 1500,
                maxTempCelsius = 37.0f,
                chargerType = "AC Adapter",
                isAnomaly = true,
                anomalyReason = "SLOW_CHARGING"
            )
        )
        val pendingTs = 1_700_000_700_000L
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 500.0,
            sessionSeconds = 600L,
            maxSessionTemp = 37.0f,
            currentSumForAvg = 900_000L,
            currentReadingsCount = 600L,
            alertedAnomalyCodes = setOf("SLOW_CHARGING", "THERMAL_THROTTLING"),
            bufferedSamples = listOf(sampleLiveState(level = 60, sessionMah = 500.0, sessionDurationSec = 600L)),
            pendingBatchTimestampMs = pendingTs
        )

        val result = service.ensureSessionFinalizedSafelyForTesting()
        assertEquals(BatteryMonitorService.SessionFinalizationResult.ALREADY_CLOSED_IDEMPOTENT, result)

        val updated = faultDao.getSessionById(sessionId)!!
        assertEquals("SLOW_CHARGING, THERMAL_THROTTLING", updated.anomalyReason)
        assertEquals(1, faultDao.countSamplesAtTimestamp(sessionId, pendingTs))
        assertEquals(0, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)
    }

    /**
     * Skenario 34 (v5 P0): Klasifikasi kegagalan rekonsiliasi membedakan CHECKPOINT_CORRUPT,
     * CHECKPOINT_UNAVAILABLE, DATABASE_INACCESSIBLE, dan TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP.
     */
    @Test
    fun test34_separateClassification_distinguishesCheckpointCorruptFromCheckpointUnavailable() = runTest {
        // 1. CHECKPOINT_CORRUPT
        val rawPrefs = context.getSharedPreferences("powerguard_session_recovery", Context.MODE_PRIVATE)
        assertTrue(
            rawPrefs.edit()
                .putBoolean("s_701_has_record", true)
                .putLong("s_701_session_id", 701L)
                // sengaja menghilangkan field wajib lainnya agar terdeteksi korup
                .commit()
        )
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.CHECKPOINT_CORRUPT,
            service.reconcilePersistedRecoveryForTesting()
        )
        assertEquals(SessionRecoveryStore.LoadStatus.CORRUPT, SessionRecoveryStore.loadForSession(context, 701L).status)

        // 2. CHECKPOINT_UNAVAILABLE
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                throw IllegalStateException("Storage locked")
            }

            override fun loadAllDetailed(context: Context): List<SessionRecoveryStore.CheckpointLoadResult> {
                throw IllegalStateException("Storage locked")
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean = false
        })
        assertEquals(
            BatteryMonitorService.PersistedRecoveryReconcileStatus.CHECKPOINT_UNAVAILABLE,
            service.reconcilePersistedRecoveryForTesting()
        )
    }

    /**
     * Skenario 35 (v5 P1): Multi-round bounded buffer compaction (>600 sampel) mempertahankan
     * total bobot kumulatif, suhu puncak, dan rata-rata tertimbang secara presisi.
     */
    @Test
    fun test35_multiRoundBoundedBufferCompaction_preservesExactWeightsPeakTempAndDuration() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 700_000L,
                startLevel = 15,
                chargerType = "FAST (AC Adapter)"
            )
        )
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 950.0,
            sessionSeconds = 620L,
            maxSessionTemp = 35.0f,
            currentSumForAvg = 930_000L,
            currentReadingsCount = 620L
        )

        // 200 sampel @ 1000 mA (suhu 35.0C), 200 sampel @ 1500 mA (suhu puncak 41.5C), 220 sampel @ 2000 mA (suhu 37.0C)
        repeat(200) { idx ->
            service.appendSampleToBufferForTesting(
                sampleLiveState(level = 20, currentMa = 1000, voltageMv = 4000, tempCelsius = 35.0f, sessionDurationSec = idx.toLong() + 1)
            )
        }
        repeat(200) { idx ->
            service.appendSampleToBufferForTesting(
                sampleLiveState(level = 30, currentMa = 1500, voltageMv = 4100, tempCelsius = if (idx == 50) 41.5f else 36.0f, sessionDurationSec = 201L + idx)
            )
        }
        repeat(220) { idx ->
            service.appendSampleToBufferForTesting(
                sampleLiveState(level = 45, currentMa = 2000, voltageMv = 4200, tempCelsius = 37.0f, sessionDurationSec = 401L + idx)
            )
        }

        val snapshot = service.getInMemorySessionSnapshotForTesting()
        assertEquals("Total bobot 620 sampel wajib utuh setelah 2 siklus pemadatan", 620, snapshot.bufferedTotalSampleWeight)
        assertTrue("Ukuran entri buffer wajib tetap <= 300", snapshot.bufferedSamplesCount <= 300)

        service.flushSampleBufferForTesting(isScreenOn = false)
        val persistedSamples = faultDao.getSamplesForSessionList(sessionId)
        assertEquals(1, persistedSamples.size)

        val expectedWeightedMa = ((200L * 1000L) + (200L * 1500L) + (220L * 2000L)) / 620L
        assertEquals(expectedWeightedMa.toInt(), persistedSamples.first().currentMa)
        assertEquals(41.5f, faultDao.getSessionById(sessionId)!!.maxTempCelsius, 0.01f)
    }

    /**
     * Regression P0: checkpoint lama dengan sessionId benar tetapi payload tertinggal tidak
     * boleh dianggap sebagai bukti bahwa snapshot RAM sudah durable.
     */
    @Test
    fun test_durabilityGate_rejectsStaleCheckpointForSameSessionId() {
        val sessionId = 40_405L
        val stale = buildRecoveryCheckpoint(
            sessionId = sessionId,
            accumulatedMah = 100.0,
            sessionSeconds = 60L,
            maxTemp = 35.0f,
            currentSum = 60_000L,
            currentCount = 60L,
            anomalyCodes = emptySet(),
            batchTimestampMs = 1_700_000_800_000L,
            hasPendingSample = false
        )
        assertTrue(SessionRecoveryStore.save(context, stale))

        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 280.0,
            sessionSeconds = 240L,
            maxSessionTemp = 38.0f,
            currentSumForAvg = 360_000L,
            currentReadingsCount = 240L,
            alertedAnomalyCodes = setOf("SLOW_CHARGING"),
            bufferedSamples = listOf(sampleLiveState(level = 49, sessionMah = 280.0, sessionDurationSec = 240L)),
            pendingBatchTimestampMs = 1_700_000_800_000L
        )

        assertTrue(
            "Checkpoint stale tidak boleh melewati durability gate hanya karena sessionId cocok",
            service.hasUnpersistedRamOnlyDataForTesting(
                BatteryMonitorService.SessionFinalizationResult.COMPLETE_DATA_FINALIZED
            )
        )
    }

    /**
     * Skenario 36 (v5 P0): DATABASE_UNAVAILABLE disertai kegagalan penyimpanan checkpoint
     * tetap menahan shutdown layanan (RECOVERY_HOLD) sampai persistensi terkonfirmasi.
     */
    @Test
    fun test36_databaseUnavailableWithFailedCheckpointCapture_entersRecoveryHoldWithoutShutdown() = runTest {
        val sessionId = faultDao.insertSession(
            ChargingSessionEntity(
                startTime = System.currentTimeMillis() - 240_000L,
                startLevel = 45,
                chargerType = "AC Adapter"
            )
        )
        service.setActorRunningForTesting(charging = true, generation = 1L)
        service.seedActiveSessionStateForTesting(
            sessionId = sessionId,
            accumulatedMah = 280.0,
            sessionSeconds = 240L,
            maxSessionTemp = 37.2f,
            currentSumForAvg = 360_000L,
            currentReadingsCount = 240L,
            bufferedSamples = listOf(sampleLiveState(level = 49, sessionMah = 280.0, sessionDurationSec = 240L)),
            pendingBatchTimestampMs = 1_700_000_800_000L
        )

        faultDao.failSessionLookup = true
        var blockSave = true
        SessionRecoveryStore.setPersistenceDelegateForTesting(object : SessionRecoveryStore.PersistenceDelegate {
            private var stored: SessionRecoveryStore.PersistedSessionRecovery? = null

            override fun saveDetailed(
                context: Context,
                recovery: SessionRecoveryStore.PersistedSessionRecovery
            ): SessionRecoveryStore.CheckpointSaveResult {
                if (blockSave) {
                    return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.COMMIT_FAILED)
                }
                stored = recovery
                return SessionRecoveryStore.CheckpointSaveResult(SessionRecoveryStore.SaveStatus.SAVED)
            }

            override fun loadDetailed(context: Context): SessionRecoveryStore.CheckpointLoadResult {
                return stored?.let { SessionRecoveryStore.CheckpointLoadResult.Loaded(it) }
                    ?: SessionRecoveryStore.CheckpointLoadResult.Empty
            }

            override fun clear(context: Context, expectedSessionId: Long): Boolean {
                if (stored?.sessionId == expectedSessionId) stored = null
                return true
            }

            override fun clearAllForTesting(context: Context): Boolean {
                stored = null
                return true
            }
        })

        service.handleStopCommandForTesting(stopStartId = 1, stopGeneration = 1L)
        assertTrue("Service wajib masuk RECOVERY_HOLD saat DB unavailable dan checkpoint save gagal", BatteryMonitorService.isRunning)
        assertTrue(service.getInMemorySessionSnapshotForTesting().isInRecoveryHold)
        assertEquals(1, service.getInMemorySessionSnapshotForTesting().bufferedSamplesCount)

        blockSave = false
        faultDao.failSessionLookup = false
        service.handleStopCommandForTesting(stopStartId = 2, stopGeneration = 1L)
        assertFalse("Service berhenti bersih setelah DB pulih dan data terverifikasi", BatteryMonitorService.isRunning)
        assertNotNull(faultDao.getSessionById(sessionId)!!.endTime)
    }
}
