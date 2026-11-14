package com.superdenot.powerguard

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.superdenot.powerguard.worker.DailyRollupWorker
import java.util.concurrent.TimeUnit

class PowerGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleDailyRollup()
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
