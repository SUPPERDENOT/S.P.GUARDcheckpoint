package com.superdenot.powerguard.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import com.superdenot.powerguard.data.repository.BatteryRepository
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.service.BatteryMonitorService
import com.superdenot.powerguard.worker.DailyRollupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PowerGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatteryRepository(application)

    private val _fallbackLiveState = MutableStateFlow(repository.readDirectSensorSnapshot())
    val liveState: StateFlow<LiveBatteryState> = repository.liveServiceState
    val isServiceRunning: StateFlow<Boolean> = repository.isServiceRunning
    val fallbackState: StateFlow<LiveBatteryState> = _fallbackLiveState.asStateFlow()

    val sessions: StateFlow<List<ChargingSessionEntity>> = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestHealth: StateFlow<BatteryHealthEntity?> = repository.latestHealthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyRollups: StateFlow<List<DailyRollupEntity>> = repository.recentRollupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val designCapacityMah: StateFlow<Int> = repository.customDesignCapacityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5000)

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    val selectedSessionSamples: StateFlow<List<BatterySampleEntity>> = _selectedSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getSamplesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appUsageList = MutableStateFlow<List<AppUsageSummary>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageSummary>> = _appUsageList.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(repository.hasUsageStatsPermission())
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(checkBatteryOptimizationIgnored())
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    init {
        // Hanya mulai service otomatis saat startup jika charger benar-benar tersambung;
        // jika tidak mengisi daya, rekonsiliasi sesi atau checkpoint yang belum selesai dengan aman.
        startMonitorIfCharging()
        startFallbackSensorLoop()
        refreshPermissionsAndUsage()
    }

    fun startMonitorIfCharging() {
        val snapshot = repository.readDirectSensorSnapshot()
        if (snapshot.isCharging) {
            startMonitorService(manual = false)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    BatteryMonitorService.recoverOrReconcileOnAppLaunch(getApplication())
                }
            }
        }
    }

    fun startManualMonitorService() {
        val snapshot = repository.readDirectSensorSnapshot()
        startMonitorService(manual = !snapshot.isCharging)
    }

    fun startMonitorService(manual: Boolean = false) {
        val context = getApplication<Application>()
        runCatching {
            BatteryMonitorService.requestStartService(context, manual = manual)
        }
    }

    private fun startFallbackSensorLoop() {
        val poller = FallbackSensorPoller()
        viewModelScope.launch(Dispatchers.IO) {
            poller.runWhileActive(
                isServiceRunning = { isServiceRunning.value },
                readSnapshot = repository::readDirectSensorSnapshot,
                onSnapshot = { _fallbackLiveState.value = it },
                onReadError = { error ->
                    Log.w(
                        "PowerGuard",
                        "Gagal memperbarui snapshot sensor fallback; akan mencoba lagi: ${error.message}",
                        error
                    )
                }
            )
        }
    }

    fun selectSessionForCurve(sessionId: Long?) {
        _selectedSessionId.value = sessionId
    }

    fun updateDesignCapacity(newCapacityMah: Int) {
        viewModelScope.launch {
            repository.saveCustomDesignCapacity(newCapacityMah)
        }
    }

    fun triggerManualRollup() {
        val request = OneTimeWorkRequestBuilder<DailyRollupWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    fun refreshPermissionsAndUsage() {
        _hasUsagePermission.value = repository.hasUsageStatsPermission()
        _isBatteryOptimizationIgnored.value = checkBatteryOptimizationIgnored()
        if (_hasUsagePermission.value) {
            viewModelScope.launch(Dispatchers.IO) {
                _appUsageList.value = repository.getTopAppsLast24Hours()
            }
        }
    }

    private fun checkBatteryOptimizationIgnored(): Boolean {
        val context = getApplication<Application>()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
