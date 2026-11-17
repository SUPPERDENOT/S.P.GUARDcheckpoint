package com.superdenot.powerguard.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
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
import com.superdenot.powerguard.data.repository.DesignCapacitySetting
import com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting
import com.superdenot.powerguard.domain.model.StatusBarParameter
import com.superdenot.powerguard.domain.model.AppUsageSummary
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.service.BatteryMonitorService
import com.superdenot.powerguard.worker.DailyRollupWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class PowerGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatteryRepository(application)
    private val autoStartRequestInFlight = AtomicBoolean(false)
    private val _fallbackLiveState = MutableStateFlow(repository.readDirectSensorSnapshot())
    private val _fallbackUpdatedAtElapsedRealtime = MutableStateFlow(SystemClock.elapsedRealtime())
    val liveState: StateFlow<LiveBatteryState> = repository.liveServiceState
    val isServiceRunning: StateFlow<Boolean> = repository.isServiceRunning
    val lastSensorUpdateElapsedRealtime: StateFlow<Long> = repository.lastSensorUpdateElapsedRealtime
    val fallbackUpdatedAtElapsedRealtime: StateFlow<Long> = _fallbackUpdatedAtElapsedRealtime.asStateFlow()
    val lastMonitorError: StateFlow<String?> = repository.lastMonitorError
    val fallbackState: StateFlow<LiveBatteryState> = _fallbackLiveState.asStateFlow()

    val sessions: StateFlow<List<ChargingSessionEntity>> = repository.allSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestHealth: StateFlow<BatteryHealthEntity?> = repository.latestHealthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyRollups: StateFlow<List<DailyRollupEntity>> = repository.recentRollupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val designCapacitySetting: StateFlow<DesignCapacitySetting> = repository.designCapacitySettingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DesignCapacitySetting(null, false))
    val batteryMonitoringEnabled: StateFlow<Boolean> = repository.batteryMonitoringEnabledFlow
        .catch { error ->
            Log.e("PowerGuard", "Gagal mengamati preferensi Battery Monitoring; pemantauan dinonaktifkan", error)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val statusBarIndicatorSetting: StateFlow<StatusBarIndicatorSetting> =
        repository.statusBarIndicatorSettingFlow
            .catch { error ->
                Log.e("PowerGuard", "Gagal mengamati preferensi indikator status bar", error)
                emit(StatusBarIndicatorSetting())
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                StatusBarIndicatorSetting()
            )

    private val _autoMonitoringError = MutableStateFlow<String?>(null)
    val autoMonitoringError: StateFlow<String?> = _autoMonitoringError.asStateFlow()

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
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = runCatching { repository.isBatteryMonitoringEnabled() }.getOrElse { error ->
                Log.e("PowerGuard", "Preferensi Battery Monitoring gagal dibaca; layanan ditahan", error)
                _autoMonitoringError.value = "Preferensi pemantauan tidak dapat dibaca; layanan dinonaktifkan."
                false
            }
            val charging = repository.readDirectSensorSnapshot().isCharging
            when {
                !enabled && BatteryMonitorService.isStartingOrRunning ->
                    runCatching { BatteryMonitorService.requestStopMonitoring(getApplication()) }
                enabled && charging && !BatteryMonitorService.isStartingOrRunning ->
                    requestBatteryMonitorStartSafely()
                else -> runCatching { BatteryMonitorService.recoverOrReconcileOnAppLaunch(getApplication()) }
            }
        }
    }

    fun setStatusBarIndicatorEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.setStatusBarIndicatorEnabled(enabled) }
                .onFailure { error ->
                    Log.e("PowerGuard", "Gagal menyimpan status indikator status bar", error)
                }
        }
    }

    fun setStatusBarParameter(parameter: StatusBarParameter) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.setStatusBarParameter(parameter) }
                .onFailure { error ->
                    Log.e("PowerGuard", "Gagal menyimpan parameter indikator status bar", error)
                }
        }
    }

    fun setBatteryMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setBatteryMonitoringEnabled(enabled)
                _autoMonitoringError.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _autoMonitoringError.value = "Pengaturan Battery Monitoring gagal disimpan."
                Log.e("PowerGuard", "Gagal menyimpan preferensi Battery Monitoring: ${error.message}", error)
                return@launch
            }
            if (!enabled) {
                runCatching { BatteryMonitorService.requestStopMonitoring(getApplication()) }
                    .onFailure { error ->
                        _autoMonitoringError.value = "Pengaturan OFF tersimpan, tetapi layanan belum dapat dihentikan sepenuhnya."
                        Log.e("PowerGuard", "Gagal mengirim perintah STOP layanan", error)
                    }
            } else if (repository.readDirectSensorSnapshot().isCharging) {
                requestBatteryMonitorStartSafely()
            }
        }
    }

    private fun requestBatteryMonitorStartSafely() {
        if (BatteryMonitorService.isStartingOrRunning ||
            !autoStartRequestInFlight.compareAndSet(false, true)
        ) return
        try {
            BatteryMonitorService.requestStartService(getApplication(), manual = false)
            _autoMonitoringError.value = null
        } catch (error: Exception) {
            _autoMonitoringError.value =
                "Preferensi ON tersimpan, tetapi layanan belum aktif. Periksa izin dan pembatasan Android."
            Log.e("PowerGuard", "Battery Monitoring gagal dimulai: ${error.message}", error)
        } finally {
            autoStartRequestInFlight.set(false)
        }
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
                isServiceDataFresh = {
                    val lastUpdate = lastSensorUpdateElapsedRealtime.value
                    val age = SystemClock.elapsedRealtime() - lastUpdate
                    isServiceRunning.value && lastUpdate > 0L && age in 0L..5_000L
                },
                readSnapshot = repository::readDirectSensorSnapshot,
                onSnapshot = {
                    _fallbackLiveState.value = it
                    _fallbackUpdatedAtElapsedRealtime.value = SystemClock.elapsedRealtime()
                },
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

    fun useAutomaticDesignCapacity() {
        viewModelScope.launch {
            repository.useAutomaticDesignCapacity()
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
