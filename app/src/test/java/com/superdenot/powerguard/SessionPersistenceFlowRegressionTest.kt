package com.superdenot.powerguard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.dao.SampleFlushStatus
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
            isScreenOn = true
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
}
