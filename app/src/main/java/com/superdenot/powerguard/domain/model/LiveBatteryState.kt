package com.superdenot.powerguard.domain.model

data class LiveBatteryState(
    val level: Int = 0,
    val currentMa: Int = 0,
    val voltageMv: Int = 0,
    val powerWatt: Float = 0f,
    val tempCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val isPowerConnected: Boolean = false,
    val chargeSource: String = "Discharging",
    // Explicit sysfs/HAL protocol when available; otherwise a conservative source-capability label.
    val chargingProtocol: String = PROTOCOL_NOT_CONNECTED,
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
    val isPowerAvailable: Boolean = true,
    val isChargingStatusAvailable: Boolean = true,
    /** Estimated minutes to full; null means insufficient or unavailable live samples. */
    val chargingEtaMinutes: Long? = null,
    /** True only while the Battery Monitoring session is collecting ETA samples. */
    val isChargingEtaTracking: Boolean = false,
    /** True only when ACTION_BATTERY_CHANGED exposed EXTRA_PLUGGED metadata. */
    val isChargeSourceAvailable: Boolean = false
) {
    companion object {
        const val PROTOCOL_NOT_CONNECTED = "Tidak Terhubung"
        const val PROTOCOL_NOT_EXPOSED = "Tidak Diekspos Perangkat"
    }
}

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val foregroundTimeMinutes: Long,
    val lastTimeUsed: Long
)
