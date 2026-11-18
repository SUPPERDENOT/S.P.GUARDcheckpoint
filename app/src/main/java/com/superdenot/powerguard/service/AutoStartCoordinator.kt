package com.superdenot.powerguard.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.superdenot.powerguard.data.repository.BatteryMonitoringSettings
import kotlinx.coroutines.CancellationException

internal enum class AutoStartTrigger {
    POWER_CONNECTED,
    BOOT_COMPLETED,
    APP_FOREGROUND_CHARGING,
    MONITORING_ENABLED
}

internal enum class AutoStartRequestResult {
    REQUESTED,
    ALREADY_IN_FLIGHT,
    MONITORING_DISABLED,
    NOT_CHARGING,
    SERVICE_ALREADY_ACTIVE,
    DEFERRED_BY_PLATFORM,
    FAILED
}

internal data class ChargingDetectionEpoch(
    val epochId: Long,
    val detectedAtElapsedRealtime: Long,
    val isNew: Boolean
)

/**
 * Process-local gate shared by receiver and ViewModel triggers.
 * It deduplicates concurrent requests while preserving one T0 per charging epoch.
 */
internal class AutoStartAttemptGate {
    private val lock = Any()
    private var chargingEpoch: ChargingDetectionEpoch? = null
    private var requestInFlight = false
    private var nextEpochId = 0L
    private var nextAttemptId = 0L

    fun tryAcquire(): Boolean = synchronized(lock) {
        if (requestInFlight) return@synchronized false
        requestInFlight = true
        true
    }

    fun release() = synchronized(lock) {
        requestInFlight = false
    }

    fun recordChargingDetection(nowElapsedRealtime: Long): ChargingDetectionEpoch =
        synchronized(lock) {
            chargingEpoch?.let { return@synchronized it.copy(isNew = false) }
            val epoch = ChargingDetectionEpoch(
                epochId = ++nextEpochId,
                detectedAtElapsedRealtime = nowElapsedRealtime,
                isNew = true
            )
            chargingEpoch = epoch
            epoch
        }

    fun nextAttemptId(): Long = synchronized(lock) { ++nextAttemptId }

    fun resetChargingEpoch(): Boolean = synchronized(lock) {
        val hadEpoch = chargingEpoch != null
        chargingEpoch = null
        hadEpoch
    }
}

internal object AutoStartPolicy {
    fun isAllowed(
        trigger: AutoStartTrigger,
        appForeground: Boolean,
        batteryOptimizationIgnored: Boolean
    ): Boolean = when (trigger) {
        AutoStartTrigger.BOOT_COMPLETED -> true
        AutoStartTrigger.POWER_CONNECTED -> appForeground || BatteryMonitoringSettings.canStartFromPowerConnected(true, batteryOptimizationIgnored)
        AutoStartTrigger.APP_FOREGROUND_CHARGING,
        AutoStartTrigger.MONITORING_ENABLED -> appForeground
    }
}

internal object AutoStartCoordinator {
    private const val TAG = "SPG_AUTO_START"
    private val gate = AutoStartAttemptGate()

    @Volatile
    private var appForeground = false

    fun setAppForeground(isForeground: Boolean) {
        appForeground = isForeground
        Log.d(TAG, "App foreground=$isForeground")
    }

    fun onChargingDisconnected() {
        if (gate.resetChargingEpoch()) {
            Log.d(TAG, "Charging epoch reset after disconnect")
        }
    }

    suspend fun requestStart(
        context: Context,
        trigger: AutoStartTrigger,
        observedAtElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): AutoStartRequestResult {
        if (!gate.tryAcquire()) {
            Log.d(TAG, "Duplicate Auto Start request ignored; trigger=$trigger")
            return AutoStartRequestResult.ALREADY_IN_FLIGHT
        }

        try {
            val appContext = context.applicationContext
            if (!BatteryMonitoringSettings.isEnabled(appContext)) {
                Log.i(TAG, "Battery Monitoring OFF; Auto Start ignored; trigger=$trigger")
                return AutoStartRequestResult.MONITORING_DISABLED
            }

            if (!isDeviceCurrentlyCharging(appContext)) {
                onChargingDisconnected()
                Log.d(TAG, "Charging State is not CHARGING; trigger=$trigger")
                return AutoStartRequestResult.NOT_CHARGING
            }

            if (BatteryMonitorService.isStartingOrRunning) {
                Log.d(TAG, "Service already STARTING/RUNNING; trigger=$trigger")
                return AutoStartRequestResult.SERVICE_ALREADY_ACTIVE
            }

            val batteryOptimizationIgnored = if (
                trigger == AutoStartTrigger.POWER_CONNECTED && !appForeground
            ) {
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
            } else {
                false
            }

            if (!AutoStartPolicy.isAllowed(trigger, appForeground, batteryOptimizationIgnored)) {
                Log.w(TAG, "Auto Start deferred by Android background-start policy; trigger=$trigger")
                return AutoStartRequestResult.DEFERRED_BY_PLATFORM
            }

            val epoch = gate.recordChargingDetection(observedAtElapsedRealtime)
            if (epoch.isNew) {
                Log.i(
                    TAG,
                    "T0 charging detected; epoch=${epoch.epochId}; elapsedRealtime=${epoch.detectedAtElapsedRealtime}; trigger=$trigger"
                )
            }

            val attemptId = gate.nextAttemptId()
            Log.i(TAG, "Auto Start request accepted; attempt=$attemptId; epoch=${epoch.epochId}; trigger=$trigger")
            BatteryMonitorService.requestStartService(
                context = appContext,
                manual = false,
                autoStartAttemptId = attemptId,
                autoStartEpochId = epoch.epochId,
                autoStartT0ElapsedRealtime = epoch.detectedAtElapsedRealtime
            )
            AutoStartRequestResult.REQUESTED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Auto Start request failed; trigger=$trigger; ${error.message}", error)
            AutoStartRequestResult.FAILED
        } finally {
            gate.release()
        }
    }

    private fun isDeviceCurrentlyCharging(context: Context): Boolean {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
