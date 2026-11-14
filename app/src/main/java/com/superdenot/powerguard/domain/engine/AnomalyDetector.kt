package com.superdenot.powerguard.domain.engine

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState

class AnomalyDetector(private val context: Context) {

    private var lowCurrentDurationSeconds = 0
    private val signFlipTimestampsMs = ArrayDeque<Long>()
    private var lastNonZeroSign = 0

    data class AnomalyResult(
        val isAnomaly: Boolean,
        val code: String?,
        val message: String?
    )

    fun reset() {
        lowCurrentDurationSeconds = 0
        signFlipTimestampsMs.clear()
        lastNonZeroSign = 0
    }

    fun evaluate(state: LiveBatteryState, isScreenOn: Boolean): AnomalyResult {
        if (!state.isCharging || state.level >= 95) {
            reset()
            return AnomalyResult(false, null, null)
        }

        val nowMs = System.currentTimeMillis()

        // 1. Deteksi osilasi arus merah-hijau (positif <-> negatif) khas Fake Fast Charging
        val currentSign = when {
            state.currentMa > 30 -> 1
            state.currentMa < -30 -> -1
            else -> 0
        }
        if (currentSign != 0) {
            if (lastNonZeroSign != 0 && currentSign != lastNonZeroSign) {
                signFlipTimestampsMs.addLast(nowMs)
            }
            lastNonZeroSign = currentSign
        }
        while (signFlipTimestampsMs.isNotEmpty() && nowMs - signFlipTimestampsMs.first() > 45_000L) {
            signFlipTimestampsMs.removeFirst()
        }

        if (signFlipTimestampsMs.size >= 4) {
            return AnomalyResult(
                isAnomaly = true,
                code = "FAKE_FAST_CHARGE_OSCILLATION",
                message = "Fake Fast Charging terdeteksi: Arus pengisian berkedip tidak stabil (berubah positif/negatif ${signFlipTimestampsMs.size}x dalam 45 detik). Segera periksa atau ganti kabel & kepala charger."
            )
        }

        // 2. Deteksi arus rendah berkepanjangan saat charger terhubung
        val minExpectedMa = if (state.chargeSource == "AC Adapter" || state.isFastCharging) 450 else 250
        if (state.currentMa < minExpectedMa) {
            lowCurrentDurationSeconds++
        } else {
            lowCurrentDurationSeconds = (lowCurrentDurationSeconds - 1).coerceAtLeast(0)
        }

        // 3. Deteksi Thermal Throttling
        if (state.tempCelsius >= 41.0f && state.currentMa < 500) {
            return AnomalyResult(
                isAnomaly = true,
                code = "THERMAL_THROTTLING",
                message = "Suhu baterai tinggi (${state.tempCelsius}°C). Sistem memangkas arus pengisian menjadi ${state.currentMa} mA."
            )
        }

        // 4. Peringatan cepat (30 detik untuk AC/Fast Charge, 90 detik untuk USB)
        val triggerThresholdSec = if (state.chargeSource == "AC Adapter" || state.isFastCharging) 30 else 90
        if (lowCurrentDurationSeconds >= triggerThresholdSec) {
            val topApp = getTopActiveApps(
                startTime = nowMs - (state.sessionDurationSec * 1000L).coerceAtLeast(60_000L),
                endTime = nowMs
            ).firstOrNull()

            val wakelockHint = if (!isScreenOn && state.deepSleepRatio < 0.25f) {
                " CPU gagal masuk Deep Sleep (Wakelock aktif${topApp?.let { ", terdeteksi: ${it.appName}" } ?: ""})."
            } else {
                " Periksa kabel charger, adaptor, atau port USB yang kotor/longgar."
            }

            val code = when {
                state.currentMa < 0 -> "NET_DRAIN_CHARGING"
                state.chargeSource == "AC Adapter" || state.isFastCharging -> "FAKE_FAST_CHARGING"
                else -> "SLOW_CHARGING"
            }

            return AnomalyResult(
                isAnomaly = true,
                code = code,
                message = "Anomali Pengisian (${state.chargeSource}): Arus masuk hanya ${state.currentMa} mA (${String.format("%.2f", state.powerWatt)} W).$wakelockHint"
            )
        }

        return AnomalyResult(false, null, null)
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
