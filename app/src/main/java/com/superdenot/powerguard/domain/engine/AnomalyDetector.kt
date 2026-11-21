package com.superdenot.powerguard.domain.engine

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.os.SystemClock
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState

internal fun isNetDrainWhilePowerConnected(state: LiveBatteryState): Boolean =
    state.isPowerConnected && state.isCurrentAvailable && state.currentMa < 0

class AnomalyDetector(private val context: Context) {

    private val chargingProgressTracker = ChargingProgressTracker()
    private val stagnantWatchdog = CcCvStagnantWatchdog()

    data class AnomalyResult(
        val isAnomaly: Boolean,
        val code: String?,
        val message: String?
    )

    fun reset() {
        chargingProgressTracker.reset()
        stagnantWatchdog.reset()
    }

    fun evaluate(state: LiveBatteryState, isScreenOn: Boolean): AnomalyResult {
        if ((!state.isCharging && !state.isPowerConnected) || state.level >= 100) {
            reset()
            return AnomalyResult(false, null, null)
        }

        if (state.isCharging && state.isTemperatureAvailable && state.tempCelsius >= 41.0f &&
            state.isCurrentAvailable && state.currentMa in 0 until 500
        ) {
            // Pause/reset progress timers while thermal control may intentionally limit charging.
            chargingProgressTracker.reset()
            stagnantWatchdog.reset()
            return AnomalyResult(
                isAnomaly = true,
                code = "THERMAL_THROTTLING",
                message = "Suhu baterai tinggi (" + state.tempCelsius + "°C). Sistem mungkin membatasi arus pengisian (" + state.currentMa + " mA)."
            )
        }

        val nowElapsedMs = SystemClock.elapsedRealtime()
        val stagnantMinutes = stagnantWatchdog.evaluate(state, nowElapsedMs)
        if (stagnantMinutes != null) {
            val phase = if (state.level < 80) "CC" else "CV"
            return AnomalyResult(
                isAnomaly = true,
                code = "STAGNANT_CHARGING",
                message = "Arus baterai sisi pengisian berada di bawah ambang fase $phase tanpa kenaikan level selama $stagnantMinutes menit. Ini indikasi pengisian stagnan, bukan identifikasi protokol atau bukti kerusakan hardware."
            )
        }
        val issue = chargingProgressTracker.evaluate(state, nowElapsedMs)
            ?: return AnomalyResult(false, null, null)

        val topApp = if (!isScreenOn && state.deepSleepRatio < 0.25f) {
            val now = System.currentTimeMillis()
            getTopActiveApps(
                startTime = now - (state.sessionDurationSec * 1000L).coerceAtLeast(60_000L),
                endTime = now
            ).firstOrNull()
        } else null

        return when (issue) {
            ChargingProgressTracker.Issue.NO_CHARGING_INPUT -> {
                val netDrain = isNetDrainWhilePowerConnected(state)
                AnomalyResult(
                    isAnomaly = true,
                    code = if (netDrain) "NET_DRAIN_CHARGING" else "CHARGING_NO_INPUT",
                    message = if (netDrain) {
                        "Daya eksternal terhubung, tetapi arus baterai terukur menunjukkan net discharge (${state.currentMa} mA) secara berkelanjutan. Ini adalah pembacaan pada sisi baterai, bukan identifikasi protokol atau bukti kerusakan hardware."
                    } else {
                        "Status Android menunjukkan sedang mengisi, tetapi arus baterai terukur hampir nol selama sedikitnya 3 menit. Label pengisian dari ponsel tidak membuktikan daya benar-benar masuk. Periksa adaptor, kabel, dan konektor."
                    }
                )
            }
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
            ChargingProgressTracker.Issue.CHARGING_PROGRESS_NOT_OBSERVED -> AnomalyResult(
                isAnomaly = true,
                code = "CHARGING_PROGRESS_NOT_OBSERVED",
                message = "Status Android menunjukkan pengisian, tetapi persentase baterai tidak bertambah selama sedikitnya 90 menit. Ini belum membuktikan adaptor gagal: beban perangkat, pembatasan suhu, atau pembacaan level baterai dapat memengaruhi hasil. Periksa arus, suhu, dan penggunaan perangkat."
            )
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
