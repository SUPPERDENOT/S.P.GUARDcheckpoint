package com.superdenot.powerguard.domain.model

data class LiveBatteryState(
    val level: Int = 0,
    val currentMa: Int = 0,
    val voltageMv: Int = 0,
    val powerWatt: Float = 0f,
    val tempCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val chargeSource: String = "Discharging",
    val isFastCharging: Boolean = false,
    val sessionMahDelivered: Double = 0.0,
    val sessionDurationSec: Long = 0L,
    val deepSleepRatio: Float = 0f,
    val anomalyAlert: String? = null,
    val designCapacityMah: Int = 0,
    // Availability flags keep unsupported sensors distinct from valid zero readings.
    val isLevelAvailable: Boolean = true,
    val isCurrentAvailable: Boolean = true,
    val isVoltageAvailable: Boolean = true,
    val isTemperatureAvailable: Boolean = true,
    val isPowerAvailable: Boolean = true
)

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val foregroundTimeMinutes: Long,
    val lastTimeUsed: Long
)
