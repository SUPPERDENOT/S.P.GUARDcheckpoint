package com.superdenot.powerguard

import android.app.Application
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.superdenot.powerguard.receiver.PowerConnectionReceiver
import com.superdenot.powerguard.worker.DailyRollupWorker
import java.util.concurrent.TimeUnit

class PowerGuardApp : Application() {

    private var chargingEventsReceiver: PowerConnectionReceiver? = null

    override fun onCreate() {
        super.onCreate()
        scheduleDailyRollup()
        registerChargingEvents()
    }

    private fun registerChargingEvents() {
        val receiver = PowerConnectionReceiver()
        val filter = IntentFilter().apply {
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        chargingEventsReceiver = receiver
    }

    private fun scheduleDailyRollup() {
        val rollupRequest = PeriodicWorkRequestBuilder<DailyRollupWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "powerguard_daily_rollup",
            ExistingPeriodicWorkPolicy.KEEP,
            rollupRequest
        )
    }
}
