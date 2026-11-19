package com.superdenot.powerguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Entity(tableName = "charging_sessions")
data class V1ChargingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val totalMahDelivered: Double,
    val avgCurrentMa: Int,
    val maxTempCelsius: Float,
    val chargerType: String,
    val isAnomaly: Boolean,
    val anomalyReason: String?,
    val deepSleepRatio: Float
)

@Entity(tableName = "battery_samples")
data class V1BatterySample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val batteryLevel: Int,
    val currentMa: Int,
    val voltageMv: Int,
    val tempCelsius: Float,
    val powerWatt: Float,
    val isScreenOn: Boolean
)

@Entity(tableName = "battery_health_logs")
data class V1BatteryHealth(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long,
    val estimatedCapacityMah: Int,
    val designCapacityMah: Int,
    val healthPercentage: Float
)

@Entity(tableName = "daily_rollups")
data class V1DailyRollup(
    @PrimaryKey val dateKey: String,
    val totalCycles: Float,
    val totalMahCharged: Double,
    val avgTempCelsius: Float,
    val maxTempCelsius: Float,
    val anomalyCount: Int
)

@Database(entities = [V1ChargingSession::class, V1BatterySample::class, V1BatteryHealth::class, V1DailyRollup::class], version = 1, exportSchema = false)
abstract class AppDatabaseV1 : RoomDatabase()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AppDatabaseMigrationTest {
    private lateinit var context: Context
    private lateinit var legacy: AppDatabaseV1
    private lateinit var migrated: AppDatabase

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        legacy = Room.databaseBuilder(context, AppDatabaseV1::class.java, DB_NAME).allowMainThreadQueries().build()
        legacy.openHelper.writableDatabase.execSQL("""INSERT INTO charging_sessions (id,startTime,endTime,startLevel,endLevel,totalMahDelivered,avgCurrentMa,maxTempCelsius,chargerType,isAnomaly,anomalyReason,deepSleepRatio) VALUES (41,1000,NULL,55,NULL,12.5,900,35.5,'AC Adapter',0,NULL,0.0)""")
        legacy.openHelper.writableDatabase.execSQL("""INSERT INTO battery_samples (id,sessionId,timestamp,batteryLevel,currentMa,voltageMv,tempCelsius,powerWatt,isScreenOn) VALUES (73,41,2000,56,900,4200,35.5,3.78,0)""")
        legacy.close()
        migrated = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() {
        if (::migrated.isInitialized) migrated.close()
        if (::legacy.isInitialized && legacy.isOpen) legacy.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test fun migrationPreservesRowsAndDefaultsNewAvailabilityToFalse() {
        val session = kotlinx.coroutines.runBlocking { migrated.batteryDao().getSessionById(41L) }
        val samples = kotlinx.coroutines.runBlocking { migrated.batteryDao().getSamplesForSessionList(41L) }
        assertNotNull(session)
        assertEquals(12.5, session!!.totalMahDelivered, 0.0001)
        assertFalse(session.hasCurrentData)
        assertFalse(session.hasTemperatureData)
        assertEquals(1, samples.size)
        assertEquals(73L, samples.single().id)
        assertFalse(samples.single().isCurrentAvailable)
        assertFalse(samples.single().isTemperatureAvailable)
        assertFalse(samples.single().isChargingStatusAvailable)
    }

    companion object { private const val DB_NAME = "spg-room-v1-v2-migration-test.db" }
}
