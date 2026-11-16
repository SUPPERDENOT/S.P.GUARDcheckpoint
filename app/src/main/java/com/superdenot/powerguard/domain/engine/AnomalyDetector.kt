package com.superdenot.powerguard.domain.engine

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.os.SystemClock
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState

class AnomalyDetector(private val context: Context) {

    private val chargingProgressTracker = ChargingProgressTracker()

    data class AnomalyResult(
        val isAnomaly: Boolean,
        val code: String?,
        val message: String?
    )

    fun reset() {
        chargingProgressTracker.reset()
    }

    fun evaluate(state: LiveBatteryState, isScreenOn: Boolean): AnomalyResult {
        if (!state.isCharging || state.level >= 95) {
            reset()
            return AnomalyResult(false, null, null)
        }

        if (state.isTemperatureAvailable && state.tempCelsius >= 41.0f &&
            state.isCurrentAvailable && state.currentMa < 500
        ) {
            return AnomalyResult(
                isAnomaly = true,
                code = "THERMAL_THROTTLING",
                message = "Suhu baterai tinggi (" + state.tempCelsius + "°C). Sistem mungkin membatasi arus pengisian (" + state.currentMa + " mA)."
            )
        }

        val issue = chargingProgressTracker.evaluate(state, SystemClock.elapsedRealtime())
            ?: return AnomalyResult(false, null, null)

        val topApp = if (!isScreenOn && state.deepSleepRatio < 0.25f) {
            val now = System.currentTimeMillis()
            getTopActiveApps(
                startTime = now - (state.sessionDurationSec * 1000L).coerceAtLeast(60_000L),
                endTime = now
            ).firstOrNull()
        } else null

        return when (issue) {
            ChargingProgressTracker.Issue.NO_CHARGING_INPUT -> AnomalyResult(
                isAnomaly = true,
                code = "CHARGING_NO_INPUT",
                message = "Status Android menunjukkan sedang mengisi, tetapi arus baterai terukur hampir nol selama sedikitnya 3 menit. Label Fast Charging dari ponsel tidak membuktikan daya benar-benar masuk. Periksa adaptor, kabel, dan konektor."
            )
            ChargingProgressTracker.Issue.CHARGING_PROGRESS_STALLED -> {
                val wakelockHint = if (topApp != null) {
                    " Aktivitas layar mati terdeteksi pada " + topApp.appName + "; beban perangkat mungkin ikut menghambat kenaikan baterai."
                } else ""
                AnomalyResult(
                    isAnomaly = true,
                    code = "CHARGING_PROGRESS_STALLED",
                    message = "Pengisian belum menunjukkan kenaikan level baterai selama sedikitnya 45 menit, sementara arus terukur rendah (≤300 mA). Periksa sumber daya, kabel, konektor, dan beban perangkat." + wakelockHint
                )
            }
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTopActiveApps(startTime: Long, endTime: Long): List<AppUsageSummary> {
        if (!hasUsageStatsPermission()) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        val pm = context.packageManager
        return stats
            .filter { it.totalTimeInForeground > 60_000L && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                val totalMs = list.sumOf { it.totalTimeInForeground }
                val lastUsed = list.maxOf { it.lastTimeUsed }
                val label = runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrDefault(pkg)

                AppUsageSummary(
                    packageName = pkg,
                    appName = label,
                    foregroundTimeMinutes = totalMs / 60_000L,
                    lastTimeUsed = lastUsed
                )
            }
            .sortedByDescending { it.foregroundTimeMinutes }
            .take(10)
    }
}
