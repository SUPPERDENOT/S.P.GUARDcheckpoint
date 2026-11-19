package com.superdenot.powerguard.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.superdenot.powerguard.data.repository.BatteryMonitoringSettings
import com.superdenot.powerguard.data.repository.StatusBarIndicatorSettings
import kotlinx.coroutines.flow.first
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

object ChargingAutoStartScheduler {
    // Keep this name stable so existing periodic work is reused, not duplicated.
    private const val UNIQUE_WORK_NAME = "powerguard_charging_auto_start"
    private const val ONE_TIME_WORK_NAME = "powerguard_charging_auto_start_once"
    private const val CHECK_INTERVAL_MINUTES = 15L
    private const val TAG = "SPG_CHARGING_SCHEDULER"

    /**
     * Called after either service feature toggle has been confirmed ON.
     * Keeps the established periodic fallback and arms a persistent one-time
     * charging-constrained request only when the device is confirmed unplugged.
     */
    fun schedule(context: Context, replaceExisting: Boolean = false) {
        val appContext = context.applicationContext
        val request = PeriodicWorkRequestBuilder<ChargingAutoStartWorker>(
            CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresCharging(true)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            if (replaceExisting) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        enqueueOneTimeIfConfirmedUnplugged(appContext)
    }

    /**
     * Application and boot entry point. A failed preference read is propagated
     * to the caller; callers log it and do not assume either service feature is enabled.
     */
    suspend fun ensureScheduledIfEnabled(context: Context) {
        val appContext = context.applicationContext
        if (hasAnyServiceFeatureEnabled(appContext)) {
            schedule(appContext)
        } else {
            cancel(appContext)
        }
    }

    /**
     * Disconnect receiver entry point. Re-checks the master toggle before
     * persisting a one-time request, and fails closed for unknown plug state.
     */
    suspend fun armOneTimeIfUnplugged(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!hasAnyServiceFeatureEnabled(appContext)) {
            cancelOneTime(appContext)
            return false
        }
        return enqueueOneTimeIfConfirmedUnplugged(appContext)
    }

    private suspend fun hasAnyServiceFeatureEnabled(context: Context): Boolean =
        BatteryMonitoringSettings.isEnabled(context) || StatusBarIndicatorSettings.observe(context).first().enabled

    private fun enqueueOneTimeIfConfirmedUnplugged(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val pluggedValue = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (!shouldArmChargingOneTimeWork(pluggedValue)) {
            Log.d(TAG, "One-time charging work not armed; plug state is not confirmed unplugged")
            return false
        }

        val request = OneTimeWorkRequestBuilder<ChargingAutoStartWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresCharging(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        Log.i(TAG, "One-time charging Auto Start work armed with KEEP policy")
        return true
    }

    /**
     * Cancel only work that is still waiting. A RUNNING worker is allowed to
     * finish; cancelling it during the fast receiver path could interrupt its
     * coordinator request. The coordinator still deduplicates overlapping starts.
     */
    fun cancelPendingOneTime(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val workInfos = workManager.getWorkInfosForUniqueWork(ONE_TIME_WORK_NAME)
        workInfos.addListener(
            Runnable {
                try {
                    val hasPendingWork = workInfos.get().any { info ->
                        info.state == WorkInfo.State.ENQUEUED ||
                            info.state == WorkInfo.State.BLOCKED
                    }
                    if (hasPendingWork) {
                        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
                        Log.d(TAG, "Pending one-time charging Auto Start work cancelled")
                    }
                } catch (error: Exception) {
                    Log.w(TAG, "Could not inspect pending one-time charging work", error)
                }
            },
            Executor { command -> command.run() }
        )
    }

    private fun cancelOneTime(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

    /** Cancels both persistent charging paths; used whenever both service feature toggles are OFF. */
    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
    }
}

internal fun shouldArmChargingOneTimeWork(pluggedValue: Int?): Boolean =
    pluggedValue == 0
