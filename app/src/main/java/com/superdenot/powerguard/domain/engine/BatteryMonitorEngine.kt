package com.superdenot.powerguard.domain.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import com.superdenot.powerguard.domain.model.LiveBatteryState
import kotlin.math.abs

class BatteryMonitorEngine(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var invertCurrentSign: Boolean? = null
    private var baselineElapsedRealtime: Long = SystemClock.elapsedRealtime()
    private var baselineUptimeMillis: Long = SystemClock.uptimeMillis()
    private val batteryIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    val designCapacityMah: Int by lazy { readDesignCapacityFromSystem() }

    fun resetDeepSleepBaseline() {
        baselineElapsedRealtime = SystemClock.elapsedRealtime()
        baselineUptimeMillis = SystemClock.uptimeMillis()
    }

    fun calculateDeepSleepRatio(): Float {
        val deltaElapsed = (SystemClock.elapsedRealtime() - baselineElapsedRealtime).coerceAtLeast(1L)
        val deltaUptime = (SystemClock.uptimeMillis() - baselineUptimeMillis).coerceAtLeast(0L)
        val deepSleepMillis = (deltaElapsed - deltaUptime).coerceAtLeast(0L)
        return (deepSleepMillis.toFloat() / deltaElapsed.toFloat()).coerceIn(0f, 1f)
    }

    fun readCurrentState(
        sessionMah: Double = 0.0,
        sessionDurationSec: Long = 0L,
        activeAnomaly: String? = null
    ): LiveBatteryState {
        val intentStatus = context.registerReceiver(null, batteryIntentFilter)

        val level = intentStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intentStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = intentStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intentStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Discharging"
        }

        val voltageMv = intentStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempTenths = intentStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10f

        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val calibratedCurrentMa = calibrateCurrent(rawCurrent, isCharging)

        val powerWatt = (voltageMv / 1000f) * (calibratedCurrentMa / 1000f)
        val isFastCharging = isCharging && (voltageMv > 5500 || calibratedCurrentMa >= 1800 || powerWatt >= 14f)

        return LiveBatteryState(
            level = batteryPct,
            currentMa = calibratedCurrentMa,
            voltageMv = voltageMv,
            powerWatt = powerWatt,
            tempCelsius = tempCelsius,
            isCharging = isCharging,
            chargeSource = chargeSource,
            isFastCharging = isFastCharging,
            sessionMahDelivered = sessionMah,
            sessionDurationSec = sessionDurationSec,
            deepSleepRatio = calculateDeepSleepRatio(),
            anomalyAlert = activeAnomaly,
            designCapacityMah = designCapacityMah
        )
    }

    private fun calibrateCurrent(raw: Int, isCharging: Boolean): Int {
        if (raw == 0 || raw == Int.MIN_VALUE) return 0

        val magnitudeMa = if (abs(raw) >= 10_000) raw / 1000 else raw

        if (!isCharging && invertCurrentSign == null && abs(magnitudeMa) > 50) {
            invertCurrentSign = magnitudeMa > 0
        }

        return if (invertCurrentSign == true) -magnitudeMa else magnitudeMa
    }

    @SuppressLint("PrivateApi")
    private fun readDesignCapacityFromSystem(): Int {
        return runCatching {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val getBatteryCapacity = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (getBatteryCapacity.invoke(powerProfile) as Double).toInt()
            if (capacity in 1000..15000) capacity else 5000
        }.getOrDefault(5000)
    }
}
