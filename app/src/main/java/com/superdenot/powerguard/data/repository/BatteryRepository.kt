package com.superdenot.powerguard.data.repository

import android.content.Context
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import com.superdenot.powerguard.domain.benchmark.AccessoryBenchmarkSample
import com.superdenot.powerguard.domain.benchmark.AccessoryBenchmarkSession
import com.superdenot.powerguard.domain.engine.AnomalyDetector
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.service.BatteryMonitorService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class BatteryRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).batteryDao()
    private val engine = BatteryMonitorEngine(context)
    private val anomalyDetector = AnomalyDetector(context)

    val liveServiceState: StateFlow<LiveBatteryState> = BatteryMonitorService.liveState
    val isServiceRunning: StateFlow<Boolean> = BatteryMonitorService.isRunningFlow
    val lastSensorUpdateElapsedRealtime: StateFlow<Long> = BatteryMonitorService.lastSensorUpdateElapsedRealtime
    val lastMonitorError: StateFlow<String?> = BatteryMonitorService.lastMonitorError

    val allSessionsFlow: Flow<List<ChargingSessionEntity>> = dao.getAllSessionsFlow()
    val accessoryBenchmarkSessionsFlow: Flow<List<AccessoryBenchmarkSession>> =
        dao.getLabeledSessionsWithSamplesFlow().map { rows ->
            rows.map { row ->
                AccessoryBenchmarkSession(
                    sessionId = row.session.id,
                    accessoryLabel = row.session.accessoryLabel.orEmpty(),
                    startTimeMillis = row.session.startTime,
                    endTimeMillis = row.session.endTime,
                    startLevelPercent = row.session.startLevel,
                    endLevelPercent = row.session.endLevel,
                    averageCurrentMa = row.session.avgCurrentMa.takeIf { row.session.hasCurrentData },
                    maxTemperatureCelsius = row.session.maxTempCelsius.takeIf { row.session.hasTemperatureData },
                    samples = row.samples.map { sample ->
                        AccessoryBenchmarkSample(
                            timestampMillis = sample.timestamp,
                            currentMa = sample.currentMa,
                            powerWatt = sample.powerWatt,
                            tempCelsius = sample.tempCelsius,
                            isCurrentAvailable = sample.isCurrentAvailable,
                            isPowerAvailable = sample.isPowerAvailable,
                            isTemperatureAvailable = sample.isTemperatureAvailable
                        )
                    }
                )
            }
        }
    val latestHealthFlow: Flow<BatteryHealthEntity?> = dao.getLatestHealthFlow()
    val recentRollupsFlow: Flow<List<DailyRollupEntity>> = dao.getRecentRollupsFlow()

    val designCapacitySettingFlow: Flow<DesignCapacitySetting> =
        DesignCapacitySettings.observe(context, engine)
    val batteryMonitoringEnabledFlow: Flow<Boolean> = BatteryMonitoringSettings.observe(context)
    val statusBarIndicatorSettingFlow: Flow<StatusBarIndicatorSetting> =
        StatusBarIndicatorSettings.observe(context)

    suspend fun isBatteryMonitoringEnabled(): Boolean = BatteryMonitoringSettings.isEnabled(context)
    suspend fun isStatusBarIndicatorEnabled(): Boolean = StatusBarIndicatorSettings.observe(context).first().enabled
    suspend fun setBatteryMonitoringEnabled(enabled: Boolean) = BatteryMonitoringSettings.setEnabled(context, enabled)
    suspend fun setStatusBarIndicatorEnabled(enabled: Boolean) =
        StatusBarIndicatorSettings.setEnabled(context, enabled)
    suspend fun setStatusBarParameter(parameter: com.superdenot.powerguard.domain.model.StatusBarParameter) =
        StatusBarIndicatorSettings.setParameter(context, parameter)

    suspend fun saveCustomDesignCapacity(capacityMah: Int) {
        DesignCapacitySettings.saveManual(context, capacityMah)
    }

    suspend fun useAutomaticDesignCapacity() {
        DesignCapacitySettings.useAutomatic(context)
    }

    fun readDirectSensorSnapshot(includeProtocolDetection: Boolean = false): LiveBatteryState {
        return engine.readCurrentState(includeProtocolDetection = includeProtocolDetection)
    }

    fun getSamplesForSession(sessionId: Long): Flow<List<BatterySampleEntity>> {
        return dao.getSamplesForSessionFlow(sessionId)
    }

    suspend fun setAccessoryLabel(sessionId: Long, label: String?): Boolean {
        val normalizedLabel = label?.trim()?.takeIf { it.isNotEmpty() }
        return dao.updateAccessoryLabel(sessionId, normalizedLabel) == 1
    }

    fun hasUsageStatsPermission(): Boolean {
        return anomalyDetector.hasUsageStatsPermission()
    }

    fun getTopAppsLast24Hours(): List<AppUsageSummary> {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.HOURS.toMillis(24)
        return anomalyDetector.getTopActiveApps(yesterday, now)
    }

}
