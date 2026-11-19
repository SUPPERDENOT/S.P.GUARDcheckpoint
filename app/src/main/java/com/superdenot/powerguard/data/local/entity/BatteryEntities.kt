package com.superdenot.powerguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startLevel: Int,
    val endLevel: Int? = null,
    val totalMahDelivered: Double = 0.0,
    val avgCurrentMa: Int = 0,
    val maxTempCelsius: Float = 0f,
    val chargerType: String,
    val isAnomaly: Boolean = false,
    val anomalyReason: String? = null,
    val deepSleepRatio: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val hasCurrentData: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasTemperatureData: Boolean = false
)

@Entity(
    tableName = "battery_samples",
    foreignKeys = [
        ForeignKey(
            entity = ChargingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["timestamp"])]
)
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val batteryLevel: Int,
    val currentMa: Int,
    val voltageMv: Int,
    val tempCelsius: Float,
    val powerWatt: Float,
    val isScreenOn: Boolean,
    @ColumnInfo(defaultValue = "0")
    val isLevelAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isCurrentAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isVoltageAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isTemperatureAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isPowerAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isChargingStatusAvailable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isCharging: Boolean = false
)

@Entity(tableName = "battery_health_logs")
data class BatteryHealthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long,
    val estimatedCapacityMah: Int,
    val designCapacityMah: Int,
    val healthPercentage: Float
)

@Entity(tableName = "daily_rollups")
data class DailyRollupEntity(
    @PrimaryKey
    val dateKey: String,
    val totalCycles: Float,
    val totalMahCharged: Double,
    val avgTempCelsius: Float,
    val maxTempCelsius: Float,
    val anomalyCount: Int
)
