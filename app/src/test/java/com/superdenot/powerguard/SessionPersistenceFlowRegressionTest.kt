package com.superdenot.powerguard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.dao.SampleFlushStatus
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class SessionPersistenceTestApplication : android.app.Application()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = SessionPersistenceTestApplication::class, manifest = Config.NONE)
class SessionPersistenceFlowRegressionTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun atomicSampleFlush_updatesSessionAndBothObservedFlows() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                startLevel = 49,
                chargerType = "AC Adapter"
            )
        )

        val initialSessions = dao.getAllSessionsFlow().first()
        assertEquals(1, initialSessions.size)
        assertEquals(0.0, initialSessions.single().totalMahDelivered, 0.0001)

        val sample = BatterySampleEntity(
            sessionId = sessionId,
            timestamp = 61_000L,
            batteryLevel = 50,
            currentMa = 2_000,
            voltageMv = 4_200,
            tempCelsius = 36.5f,
            powerWatt = 8.4f,
            isScreenOn = true,
            isLevelAvailable = true,
            isCurrentAvailable = true,
            isVoltageAvailable = true,
            isTemperatureAvailable = true,
            isPowerAvailable = true,
            isChargingStatusAvailable = true,
            isCharging = true
        )

        val status = dao.persistSampleAndUpdateSessionAtomically(
            sample = sample,
            totalMahDelivered = 34.0,
            avgCurrentMa = 2_000,
            maxTempCelsius = 36.5f,
            hasAnomaly = false,
            anomalyReason = null,
            deepSleepRatio = 0.1f
        )

        assertEquals(SampleFlushStatus.FLUSHED, status)

        val updatedSessions = dao.getAllSessionsFlow().first()
        assertEquals(1, updatedSessions.size)
        assertEquals(34.0, updatedSessions.single().totalMahDelivered, 0.0001)
        assertEquals(2_000, updatedSessions.single().avgCurrentMa)

        val persistedSamples = dao.getSamplesForSessionFlow(sessionId).first()
        assertEquals(1, persistedSamples.size)
        assertEquals(sample.timestamp, persistedSamples.single().timestamp)
        assertTrue(dao.isSamplePersisted(sessionId, sample.timestamp))
    }

    @Test
    fun legacyRecoveryIntegration_ignoresLongGapsAndNegativeCurrent() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                startLevel = 50,
                chargerType = "AC Adapter"
            )
        )

        val samples = listOf(
            BatterySampleEntity(
                sessionId = sessionId, timestamp = 1_000L, batteryLevel = 50,
                currentMa = 1_800, voltageMv = 4_200, tempCelsius = 36.0f,
                powerWatt = 7.5f, isScreenOn = true, isCurrentAvailable = true, isTemperatureAvailable = true
            ),
            BatterySampleEntity(
                sessionId = sessionId, timestamp = 2_000L, batteryLevel = 51,
                currentMa = 3_600, voltageMv = 4_200, tempCelsius = 36.0f,
                powerWatt = 15.0f, isScreenOn = true, isCurrentAvailable = true, isTemperatureAvailable = true
            ),
            BatterySampleEntity(
                sessionId = sessionId, timestamp = 10_000L, batteryLevel = 52,
                currentMa = 3_600, voltageMv = 4_200, tempCelsius = 36.0f,
                powerWatt = 15.0f, isScreenOn = true, isCurrentAvailable = true, isTemperatureAvailable = true
            ),
            BatterySampleEntity(
                sessionId = sessionId, timestamp = 11_000L, batteryLevel = 53,
                currentMa = -1_800, voltageMv = 4_200, tempCelsius = 36.0f,
                powerWatt = -7.5f, isScreenOn = true, isCurrentAvailable = true, isTemperatureAvailable = true
            )
        )
        samples.forEach { dao.insertSample(it) }

        assertTrue(
            dao.closeSingleOpenSessionWithConsistentMetrics(
                session = dao.getSessionById(sessionId)!!,
                fallbackEndTime = 12_000L,
                fallbackEndLevel = 54
            )
        )

        val finalized = dao.getSessionById(sessionId)!!
        // 0.75 mAh for the first 1s interval + 0.5 mAh for the last;
        // the 8s gap is excluded and negative current is treated as zero.
        assertEquals(1.25, finalized.totalMahDelivered, 0.0001)
    }

    @Test
    fun alreadyClosedFinalization_persistsEligibleHealthLogExactlyOnce() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                endTime = 3_000L,
                startLevel = 20,
                endLevel = 60,
                totalMahDelivered = 2_000.0,
                chargerType = "AC Adapter"
            )
        )
        val healthLog = BatteryHealthEntity(
            timestamp = 3_000L,
            sessionId = sessionId,
            estimatedCapacityMah = 5_000,
            designCapacityMah = 5_000,
            healthPercentage = 100f
        )

        repeat(2) {
            assertEquals(
                com.superdenot.powerguard.data.local.dao.SessionFinalizationStatus.ALREADY_CLOSED,
                dao.finalizeSessionAtomically(
                    sessionId = sessionId,
                    endTime = 4_000L,
                    endLevel = 60,
                    totalMahDelivered = 2_000.0,
                    avgCurrentMa = 1_800,
                    maxTempCelsius = 37f,
                    hasAnomaly = false,
                    anomalyReason = null,
                    deepSleepRatio = 0f,
                    finalSample = null,
                    healthLog = healthLog
                )
            )
        }

        assertEquals(1, dao.countHealthLogsForSession(sessionId))
    }

    @Test
    fun recoveryReconciliation_doesNotReplaceDurableAverageWithStaleSnapshot() = runBlocking {
        val dao = database.batteryDao()
        val durableAverageSessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                startLevel = 30,
                avgCurrentMa = 2_100,
                hasCurrentData = true,
                totalMahDelivered = 100.0,
                chargerType = "AC Adapter"
            )
        )
        assertTrue(
            dao.reconcileRecoveredSessionAtomically(
                sessionId = durableAverageSessionId,
                recoveredTotalMah = 150.0,
                recoveredAvgCurrentMa = 1_500,
                recoveredMaxTempCelsius = 37f,
                recoveredHasAnomaly = false,
                recoveredAnomalyReason = null,
                recoveredSample = null
            )
        )
        assertEquals(2_100, dao.getSessionById(durableAverageSessionId)!!.avgCurrentMa)

        val missingAverageSessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                startLevel = 30,
                avgCurrentMa = 0,
                chargerType = "AC Adapter"
            )
        )
        assertTrue(
            dao.reconcileRecoveredSessionAtomically(
                sessionId = missingAverageSessionId,
                recoveredTotalMah = 150.0,
                recoveredAvgCurrentMa = 1_800,
                recoveredMaxTempCelsius = 37f,
                recoveredHasAnomaly = false,
                recoveredAnomalyReason = null,
                recoveredSample = null,
                recoveredHasCurrentData = true
            )
        )
        assertEquals(1_800, dao.getSessionById(missingAverageSessionId)!!.avgCurrentMa)
    }

    @Test
    fun alreadyClosedRecovery_backfillsEligibleHealthLogExactlyOnce() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                endTime = 3_000L,
                startLevel = 20,
                endLevel = 60,
                totalMahDelivered = 2_000.0,
                chargerType = "AC Adapter"
            )
        )

        repeat(2) {
            assertEquals(
                com.superdenot.powerguard.data.local.dao.SessionFinalizationStatus.ALREADY_CLOSED,
                dao.finalizeSingleSessionWithReconciledMetrics(
                    sessionId = sessionId,
                    endTime = 4_000L,
                    endLevel = 60,
                    designCapacityMah = 5_000
                )
            )
        }

        assertEquals(1, dao.countHealthLogsForSession(sessionId))
    }

    @Test
    fun closedOrMissingSession_rejectsSampleFlushWithoutWritingSample() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                endTime = 2_000L,
                startLevel = 49,
                endLevel = 50,
                chargerType = "AC Adapter"
            )
        )

        val status = dao.persistSampleAndUpdateSessionAtomically(
            sample = BatterySampleEntity(
                sessionId = sessionId,
                timestamp = 3_000L,
                batteryLevel = 50,
                currentMa = 1_500,
                voltageMv = 4_100,
                tempCelsius = 37f,
                powerWatt = 6.1f,
                isScreenOn = true
            ),
            totalMahDelivered = 10.0,
            avgCurrentMa = 1_500,
            maxTempCelsius = 37f,
            hasAnomaly = false,
            anomalyReason = null,
            deepSleepRatio = 0f
        )

        assertEquals(SampleFlushStatus.SESSION_CLOSED_OR_MISSING, status)
        assertTrue(dao.getSamplesForSessionFlow(sessionId).first().isEmpty())
    }
    @Test
    fun unavailableMetricsAreExcludedButMeasuredZeroRemainsAvailable() = runBlocking {
        val dao = database.batteryDao()
        val sessionId = dao.insertSession(
            ChargingSessionEntity(
                startTime = 1_000L,
                startLevel = 50,
                chargerType = "AC Adapter"
            )
        )
        val unavailable = BatterySampleEntity(
            sessionId = sessionId,
            timestamp = 2_000L,
            batteryLevel = 0,
            currentMa = 1_500,
            voltageMv = 4_200,
            tempCelsius = 49f,
            powerWatt = 6.3f,
            isScreenOn = false
        )
        assertEquals(
            SampleFlushStatus.FLUSHED,
            dao.persistSampleAndUpdateSessionAtomically(
                sample = unavailable,
                totalMahDelivered = 0.0,
                avgCurrentMa = 1_500,
                maxTempCelsius = 49f,
                hasAnomaly = false,
                anomalyReason = null,
                deepSleepRatio = 0f
            )
        )
        val unavailableSession = dao.getSessionById(sessionId)!!
        assertEquals(0, unavailableSession.avgCurrentMa)
        assertEquals(0f, unavailableSession.maxTempCelsius, 0.001f)
        assertTrue(!unavailableSession.hasCurrentData)
        assertTrue(!unavailableSession.hasTemperatureData)

        val measuredZero = BatterySampleEntity(
            sessionId = sessionId,
            timestamp = 3_000L,
            batteryLevel = 0,
            currentMa = 0,
            voltageMv = 0,
            tempCelsius = 0f,
            powerWatt = 0f,
            isScreenOn = false,
            isLevelAvailable = true,
            isCurrentAvailable = true,
            isVoltageAvailable = true,
            isTemperatureAvailable = true,
            isPowerAvailable = true,
            isChargingStatusAvailable = true,
            isCharging = false
        )
        assertEquals(
            SampleFlushStatus.FLUSHED,
            dao.persistSampleAndUpdateSessionAtomically(
                sample = measuredZero,
                totalMahDelivered = 0.0,
                avgCurrentMa = 0,
                maxTempCelsius = 0f,
                hasAnomaly = false,
                anomalyReason = null,
                deepSleepRatio = 0f
            )
        )
        val zeroSession = dao.getSessionById(sessionId)!!
        assertTrue(zeroSession.hasCurrentData)
        assertTrue(zeroSession.hasTemperatureData)
        assertEquals(0, zeroSession.avgCurrentMa)
        assertEquals(0f, zeroSession.maxTempCelsius, 0.001f)
    }

}
