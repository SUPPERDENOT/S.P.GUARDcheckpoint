package com.superdenot.powerguard.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.superdenot.powerguard.data.repository.BatteryMonitoringSettings
import com.superdenot.powerguard.service.AutoStartCoordinator
import com.superdenot.powerguard.service.AutoStartRequestResult
import com.superdenot.powerguard.service.AutoStartTrigger
import kotlinx.coroutines.CancellationException

internal enum class ChargingAutoStartWorkOutcome {
    COMPLETE,
    RETRY
}

internal fun AutoStartRequestResult.toChargingAutoStartWorkOutcome(): ChargingAutoStartWorkOutcome =
    when (this) {
        AutoStartRequestResult.MONITORING_DISABLED,
        AutoStartRequestResult.REQUESTED,
        AutoStartRequestResult.ALREADY_IN_FLIGHT,
        AutoStartRequestResult.NOT_CHARGING,
        AutoStartRequestResult.SERVICE_ALREADY_ACTIVE -> ChargingAutoStartWorkOutcome.COMPLETE

        AutoStartRequestResult.DEFERRED_BY_PLATFORM,
        AutoStartRequestResult.FAILED -> ChargingAutoStartWorkOutcome.RETRY
    }

class ChargingAutoStartWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = try {
            BatteryMonitoringSettings.isEnabled(applicationContext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Tidak dapat membaca preferensi Battery Monitoring; worker akan mencoba lagi", error)
            return Result.retry()
        }

        if (!enabled) {
            Log.i(TAG, "Battery Monitoring OFF; charging Auto Start worker selesai")
            return Result.success()
        }

        val requestResult = AutoStartCoordinator.requestStart(
            context = applicationContext,
            trigger = AutoStartTrigger.WORK_MANAGER_CHARGING
        )
        return when (requestResult.toChargingAutoStartWorkOutcome()) {
            ChargingAutoStartWorkOutcome.COMPLETE -> Result.success()
            ChargingAutoStartWorkOutcome.RETRY -> Result.retry()
        }
    }

    private companion object {
        const val TAG = "SPG_CHARGING_WORK"
    }
}
