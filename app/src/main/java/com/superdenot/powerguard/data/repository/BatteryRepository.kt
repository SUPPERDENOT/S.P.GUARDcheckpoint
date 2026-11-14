package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import com.superdenot.powerguard.domain.engine.AnomalyDetector
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.service.BatteryMonitorService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "powerguard_prefs")

class BatteryRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).batteryDao()
    private val engine = BatteryMonitorEngine(context)
    private val anomalyDetector = AnomalyDetector(context)

    val liveServiceState: StateFlow<LiveBatteryState> = BatteryMonitorService.liveState
    val isServiceRunning: StateFlow<Boolean> = BatteryMonitorService.isRunningFlow
    val lastSensorUpdateElapsedRealtime: StateFlow<Long> = BatteryMonitorService.lastSensorUpdateElapsedRealtime
    val lastMonitorError: StateFlow<String?> = BatteryMonitorService.lastMonitorError

    val allSessionsFlow: Flow<List<ChargingSessionEntity>> = dao.getAllSessionsFlow()
    val latestHealthFlow: Flow<BatteryHealthEntity?> = dao.getLatestHealthFlow()
    val recentRollupsFlow: Flow<List<DailyRollupEntity>> = dao.getRecentRollupsFlow()

    val customDesignCapacityFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DESIGN_CAPACITY] ?: engine.designCapacityMah
    }

    suspend fun saveCustomDesignCapacity(capacityMah: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DESIGN_CAPACITY] = capacityMah.coerceIn(1000, 20000)
        }
    }

    fun readDirectSensorSnapshot(): LiveBatteryState {
        return engine.readCurrentState()
    }

    fun getSamplesForSession(sessionId: Long): Flow<List<BatterySampleEntity>> {
        return dao.getSamplesForSessionFlow(sessionId)
    }

    fun hasUsageStatsPermission(): Boolean {
        return anomalyDetector.hasUsageStatsPermission()
    }

    fun getTopAppsLast24Hours(): List<AppUsageSummary> {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.HOURS.toMillis(24)
        return anomalyDetector.getTopActiveApps(yesterday, now)
    }

    companion object {
        private val KEY_DESIGN_CAPACITY = intPreferencesKey("custom_design_capacity_mah")
    }
}
