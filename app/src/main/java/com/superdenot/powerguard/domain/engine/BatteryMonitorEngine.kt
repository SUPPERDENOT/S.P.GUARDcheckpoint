package com.superdenot.powerguard.domain.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import com.superdenot.powerguard.domain.model.LiveBatteryState

class BatteryMonitorEngine(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
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
        val isLevelAvailable = level >= 0 && scale > 0
        val batteryPct = if (isLevelAvailable) (level * 100) / scale else 0

        val status = intentStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isChargingStatusAvailable = status in BatteryManager.BATTERY_STATUS_CHARGING..BatteryManager.BATTERY_STATUS_FULL
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intentStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isPowerConnected = plugged != 0
        val hasPluggedMetadata = intentStatus?.hasExtra(BatteryManager.EXTRA_PLUGGED) == true
        val protocolSource = ChargingProtocolDetector.resolveSource(
            plugged = plugged,
            hasPluggedMetadata = hasPluggedMetadata,
            isCharging = isCharging,
            acPluggedValue = BatteryManager.BATTERY_PLUGGED_AC,
            usbPluggedValue = BatteryManager.BATTERY_PLUGGED_USB,
            wirelessPluggedValue = BatteryManager.BATTERY_PLUGGED_WIRELESS
        )
        // Keep the existing user-facing coarse source classification unchanged.
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isPowerConnected || isCharging) "Unknown Source" else "Discharging"
        }
        val maxChargingCurrentMicroAmps = intentStatus
            ?.takeIf { it.hasExtra(BatteryManager.EXTRA_MAX_CHARGING_CURRENT) }
            ?.getIntExtra(BatteryManager.EXTRA_MAX_CHARGING_CURRENT, -1)
            ?.takeIf { it >= 0 }
            ?.toLong()
        val maxChargingVoltageMicroVolts = intentStatus
            ?.takeIf { it.hasExtra(BatteryManager.EXTRA_MAX_CHARGING_VOLTAGE) }
            ?.getIntExtra(BatteryManager.EXTRA_MAX_CHARGING_VOLTAGE, -1)
            ?.takeIf { it >= 0 }
            ?.toLong()
        val sysfsProtocol = if (isCharging && protocolSource != ChargingProtocolDetector.Source.NONE) {
            SysfsChargingProtocolReader.read()
        } else {
            SysfsChargingProtocolReader.Result(access = ChargingProtocolDetector.SysfsAccess.UNAVAILABLE)
        }
        val chargingProtocol = ChargingProtocolDetector.detect(
            ChargingProtocolDetector.Input(
                source = protocolSource,
                isCharging = isCharging,
                maxChargingCurrentMicroAmps = maxChargingCurrentMicroAmps,
                maxChargingVoltageMicroVolts = maxChargingVoltageMicroVolts,
                explicitSysfsProtocol = sysfsProtocol.protocol,
                sysfsAccess = sysfsProtocol.access
            )
        )

        val isVoltageAvailable = intentStatus?.hasExtra(BatteryManager.EXTRA_VOLTAGE) == true &&
            (intentStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) > 0)
        val voltageMv = if (isVoltageAvailable) {
            intentStatus!!.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        } else 0

        val isTemperatureAvailable = intentStatus?.hasExtra(BatteryManager.EXTRA_TEMPERATURE) == true
        val tempTenths = if (isTemperatureAvailable) {
            intentStatus!!.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        } else 0
        val tempCelsius = tempTenths / 10f

        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val isCurrentAvailable = rawCurrent != Int.MIN_VALUE
        val calibratedCurrentMa = BatteryCurrentNormalizer.normalize(rawCurrent, isPowerConnected)

        val isPowerAvailable = isVoltageAvailable && isCurrentAvailable
        val powerWatt = if (isPowerAvailable) {
            (voltageMv / 1000f) * (calibratedCurrentMa / 1000f)
        } else 0f

        return LiveBatteryState(
            level = batteryPct,
            currentMa = calibratedCurrentMa,
            voltageMv = voltageMv,
            powerWatt = powerWatt,
            tempCelsius = tempCelsius,
            isCharging = isCharging,
            isPowerConnected = isPowerConnected,
            chargeSource = chargeSource,
            chargingProtocol = chargingProtocol,
            sessionMahDelivered = sessionMah,
            sessionDurationSec = sessionDurationSec,
            deepSleepRatio = calculateDeepSleepRatio(),
            anomalyAlert = activeAnomaly,
            designCapacityMah = designCapacityMah,
            isLevelAvailable = isLevelAvailable,
            isCurrentAvailable = isCurrentAvailable,
            isVoltageAvailable = isVoltageAvailable,
            isTemperatureAvailable = isTemperatureAvailable,
            isPowerAvailable = isPowerAvailable,
            isChargingStatusAvailable = isChargingStatusAvailable
        )
    }

    @SuppressLint("PrivateApi")
    private fun readDesignCapacityFromSystem(): Int {
        return runCatching {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val getBatteryCapacity = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (getBatteryCapacity.invoke(powerProfile) as Double).toInt()
            if (capacity in 1000..15000) capacity else 0
        }.getOrDefault(0)
    }
}
