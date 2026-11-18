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

    val isAppForeground: Boolean
        get() = appForeground

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
            AutoStartDiagnosticLog.record(context, "COORDINATOR duplicate request trigger=$trigger")
            Log.d(TAG, "Duplicate Auto Start request ignored; trigger=$trigger")
            return AutoStartRequestResult.ALREADY_IN_FLIGHT
        }

        return try {
            val appContext = context.applicationContext
            val monitoringEnabled = BatteryMonitoringSettings.isEnabled(appContext)
            AutoStartDiagnosticLog.record(appContext, "R1 Battery Monitoring enabled=$monitoringEnabled trigger=$trigger")
            if (!monitoringEnabled) {
                Log.i(TAG, "Battery Monitoring OFF; Auto Start ignored; trigger=$trigger")
                return AutoStartRequestResult.MONITORING_DISABLED
            }

            val charging = isDeviceCurrentlyCharging(appContext)
            AutoStartDiagnosticLog.record(appContext, "R2 chargingState=$charging trigger=$trigger")
            if (!charging) {
                onChargingDisconnected()
                Log.d(TAG, "Charging State is not CHARGING; trigger=$trigger")
                return AutoStartRequestResult.NOT_CHARGING
            }

            val serviceActive = BatteryMonitorService.isStartingOrRunning
            AutoStartDiagnosticLog.record(appContext, "COORDINATOR serviceStartingOrRunning=$serviceActive")
            if (serviceActive) {
                Log.d(TAG, "Service already STARTING/RUNNING; trigger=$trigger")
                return AutoStartRequestResult.SERVICE_ALREADY_ACTIVE
            }

            val epoch = gate.recordChargingDetection(observedAtElapsedRealtime)
            if (epoch.isNew) {
                AutoStartDiagnosticLog.record(appContext, "T0 charging detected epoch=${epoch.epochId} trigger=$trigger")
                Log.i(
                    TAG,
                    "T0 charging detected; epoch=${epoch.epochId}; elapsedRealtime=${epoch.detectedAtElapsedRealtime}; trigger=$trigger"
                )
            }

            val batteryOptimizationIgnored = if (
                trigger == AutoStartTrigger.POWER_CONNECTED && !appForeground
            ) {
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
            } else {
                false
            }

            val allowed = AutoStartPolicy.isAllowed(trigger, appForeground, batteryOptimizationIgnored)
            AutoStartDiagnosticLog.record(appContext, "R3 foreground=$appForeground batteryOptimizationIgnored=$batteryOptimizationIgnored policyAllowed=$allowed trigger=$trigger")
            if (!allowed) {
                AutoStartDiagnosticLog.record(appContext, "R3 policy denied Auto Start trigger=$trigger")
                Log.w(TAG, "Auto Start deferred by Android background-start policy; trigger=$trigger")
                return AutoStartRequestResult.DEFERRED_BY_PLATFORM
            }

            val attemptId = gate.nextAttemptId()
            Log.i(TAG, "Auto Start request accepted; attempt=$attemptId; epoch=${epoch.epochId}; trigger=$trigger")
            AutoStartDiagnosticLog.record(appContext, "R4 startService request attempt=$attemptId epoch=${epoch.epochId} trigger=$trigger")
            BatteryMonitorService.requestStartService(
                context = appContext,
                manual = false,
                autoStartAttemptId = attemptId,
                autoStartEpochId = epoch.epochId,
                autoStartT0ElapsedRealtime = epoch.detectedAtElapsedRealtime
            )
            AutoStartDiagnosticLog.record(appContext, "R4 startService call returned attempt=$attemptId")
            AutoStartRequestResult.REQUESTED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val detail = error.message.orEmpty().replace('\n', ' ').replace('\r', ' ').take(180)
            AutoStartDiagnosticLog.record(
                context,
                "COORDINATOR exception trigger=$trigger type=${error.javaClass.simpleName} detail=$detail"
            )
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
