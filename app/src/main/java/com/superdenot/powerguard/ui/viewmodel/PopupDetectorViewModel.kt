package com.superdenot.powerguard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.superdenot.powerguard.data.repository.PopupDetectorRepository
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSimulationScenario
import com.superdenot.powerguard.domain.security.deduplicatePopupIncidents
import com.superdenot.powerguard.domain.security.withoutSimulatedPopupIncidents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class TimelineRiskFilter(val label: String) {
    ALL("Semua"),
    SUSPICIOUS("Mencurigakan"),
    NEEDS_REVIEW("Perlu Dicek")
}

internal data class PopupDetectorUiState(
    val isScanning: Boolean = false,
    val isLoadingArchive: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val hasActivityData: Boolean = false,
    val currentIncidents: List<PopupIncident> = emptyList(),
    val archivedIncidents: List<PopupIncident> = emptyList(),
    val safePackages: Set<String> = emptySet(),
    val lastScanAtMs: Long? = null,
    val errorMessage: String? = null,
    val simulationActive: Boolean = false,
    val timelineFilter: TimelineRiskFilter = TimelineRiskFilter.ALL
) {
    /** Current detected apps only; simulated and safe-list entries are never exposed as suspects. */
    val suspects: List<PopupIncident>
        get() = deduplicatePopupIncidents(currentIncidents, safePackages)

    /** One deduplicated, real-app entry per package across current results and persisted history. */
    val timelineEvents: List<PopupIncident>
        get() = deduplicatePopupIncidents(currentIncidents + archivedIncidents, safePackages)
            .sortedByDescending { it.lastSeenAtMs }

    val suspiciousCount: Int
        get() = timelineEvents.count { it.status == PopupRiskStatus.SUSPICIOUS }

    val needsReviewCount: Int
        get() = timelineEvents.count { it.status == PopupRiskStatus.NEEDS_REVIEW }

    val filteredTimelineEvents: List<PopupIncident>
        get() = when (timelineFilter) {
            TimelineRiskFilter.ALL -> timelineEvents
            TimelineRiskFilter.SUSPICIOUS -> timelineEvents.filter { it.status == PopupRiskStatus.SUSPICIOUS }
            TimelineRiskFilter.NEEDS_REVIEW -> timelineEvents.filter { it.status == PopupRiskStatus.NEEDS_REVIEW }
        }
}

/**
 * Reconciles a safe-list mutation and the cached raw scan snapshot in one immutable state update.
 * Derived suspects/timeline properties are consequently filtered from the same StateFlow version.
 */
internal fun reconcilePopupDetectorSafeList(
    state: PopupDetectorUiState,
    safePackages: Set<String>,
    latestRawIncidents: List<PopupIncident>? = null
): PopupDetectorUiState = state.copy(
    safePackages = safePackages,
    currentIncidents = latestRawIncidents ?: state.currentIncidents
)

internal class PopupDetectorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PopupDetectorRepository(application)
    private val _uiState = MutableStateFlow(
        PopupDetectorUiState(
            hasUsagePermission = runCatching { repository.hasUsagePermission() }.getOrDefault(false),
            safePackages = repository.safePackages(),
            isLoadingArchive = true
        )
    )
    val uiState: StateFlow<PopupDetectorUiState> = _uiState.asStateFlow()

    private var latestRawScanReport: List<PopupIncident>? = null
    private val scanMutex = Mutex()

    init {
        viewModelScope.launch {
            try {
                val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                _uiState.update { current ->
                    if (current.isLoadingArchive) current.copy(
                        archivedIncidents = archive,
                        isLoadingArchive = false
                    ) else current
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (security: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoadingArchive = false,
                        hasUsagePermission = false,
                        errorMessage = "Akses Penggunaan tidak tersedia. Periksa kembali izin SPG di Pengaturan Android."
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingArchive = false, errorMessage = "Arsip deteksi belum dapat dimuat.")
                }
            }
        }
    }

    fun runScan() {
        viewModelScope.launch {
            scanMutex.withLock { runScanLocked() }
        }
    }

    /** Compatibility entry point used by existing UI and Quick Settings call sites. */
    fun scan() = runScan()

    /** runScan is serialized with add/remove safe-list operations through the same scanMutex. */
    private suspend fun runScanLocked() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
        try {
            val result = withContext(Dispatchers.IO) { repository.scan() }
            latestRawScanReport = result.rawIncidents
            val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
            val safe = withContext(Dispatchers.IO) { repository.safePackages() }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLoadingArchive = false,
                    hasUsagePermission = result.hasUsagePermission,
                    hasActivityData = result.hasActivityData,
                    currentIncidents = result.rawIncidents,
                    archivedIncidents = archive,
                    safePackages = safe,
                    lastScanAtMs = result.scannedAtMs,
                    errorMessage = if (result.archivePersisted) null else
                        "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan.",
                    simulationActive = false
                )
            }
        } catch (cancelled: CancellationException) {
            _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
            throw cancelled
        } catch (security: SecurityException) {
            latestRawScanReport = emptyList()
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLoadingArchive = false,
                    hasUsagePermission = false,
                    currentIncidents = emptyList(),
                    errorMessage = "Akses Penggunaan ditolak Android. Aktifkan kembali izin lalu pindai ulang."
                )
            }
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLoadingArchive = false,
                    errorMessage = "Pemindaian gagal diselesaikan. Periksa izin Akses Penggunaan lalu coba lagi."
                )
            }
        }
    }

    fun setTimelineFilter(filter: TimelineRiskFilter) {
        _uiState.update { it.copy(timelineFilter = filter) }
    }

    fun showSimulation() {
        val simulated = PopupSimulationScenario.evaluate(System.currentTimeMillis())
        _uiState.update {
            val realIncidents = latestRawScanReport ?: withoutSimulatedPopupIncidents(it.currentIncidents)
            it.copy(
                currentIncidents = realIncidents + simulated,
                simulationActive = true,
                errorMessage = null
            )
        }
    }

    fun clearSimulation() {
        _uiState.update {
            it.copy(
                currentIncidents = latestRawScanReport ?: withoutSimulatedPopupIncidents(it.currentIncidents),
                simulationActive = false
            )
        }
    }

    /** Compatibility entry point for the existing UI callback. */
    fun markSafe(incident: PopupIncident) = addToSafeList(incident)

    /** Adds a package to the safe list; serialized with scans and removals. */
    fun addToSafeList(incident: PopupIncident) {
        if (incident.isSimulated) return
        viewModelScope.launch {
            scanMutex.withLock {
                val packageName = incident.packageName
                _uiState.update { current ->
                    reconcilePopupDetectorSafeList(
                        state = current.copy(isScanning = true, isLoadingArchive = false, errorMessage = null),
                        safePackages = current.safePackages + packageName,
                        latestRawIncidents = latestRawScanReport
                    ).copy(simulationActive = false)
                }
                try {
                    val saved = withContext(Dispatchers.IO) { repository.markSafe(packageName) }
                    val result = withContext(Dispatchers.IO) { repository.scan() }
                    latestRawScanReport = result.rawIncidents
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = result.rawIncidents,
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !saved -> "Aplikasi belum berhasil ditambahkan ke daftar aman."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            },
                            simulationActive = false
                        )
                    }
                } catch (cancelled: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
                    throw cancelled
                } catch (security: SecurityException) {
                    latestRawScanReport = emptyList()
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, emptyList()).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = false,
                            errorMessage = "Akses Penggunaan ditolak Android. Periksa kembali izin di Pengaturan."
                        )
                    }
                } catch (_: Exception) {
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, latestRawScanReport).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            errorMessage = "Daftar aman gagal diperbarui."
                        )
                    }
                }
            }
        }
    }

    /** Compatibility entry point for the existing UI callback. */
    fun restoreSafePackage(packageName: String) = removeFromSafeList(packageName)

    /** Removes one package from the safe list; serialized with scans and additions. */
    fun removeFromSafeList(packageName: String) {
        viewModelScope.launch {
            scanMutex.withLock {
                _uiState.update { current ->
                    reconcilePopupDetectorSafeList(
                        state = current.copy(isScanning = true, errorMessage = null, simulationActive = false),
                        safePackages = current.safePackages - packageName,
                        latestRawIncidents = latestRawScanReport
                    )
                }
                try {
                    val restored = withContext(Dispatchers.IO) { repository.restoreSafePackage(packageName) }
                    val result = withContext(Dispatchers.IO) { repository.scan() }
                    latestRawScanReport = result.rawIncidents
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = result.rawIncidents,
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !restored -> "Aplikasi belum berhasil dipulihkan dari daftar aman."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            }
                        )
                    }
                } catch (cancelled: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
                    throw cancelled
                } catch (security: SecurityException) {
                    latestRawScanReport = emptyList()
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, emptyList()).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = false,
                            errorMessage = "Akses Penggunaan ditolak Android. Periksa kembali izin di Pengaturan."
                        )
                    }
                } catch (_: Exception) {
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, latestRawScanReport).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            errorMessage = "Aplikasi gagal dipulihkan dari daftar aman."
                        )
                    }
                }
            }
        }
    }

    fun restoreSafeList() {
        viewModelScope.launch {
            scanMutex.withLock {
                _uiState.update { current ->
                    reconcilePopupDetectorSafeList(
                        state = current.copy(isScanning = true, errorMessage = null, simulationActive = false),
                        safePackages = emptySet(),
                        latestRawIncidents = latestRawScanReport
                    )
                }
                try {
                    val restored = withContext(Dispatchers.IO) { repository.restoreSafePackages() }
                    val result = withContext(Dispatchers.IO) { repository.scan() }
                    latestRawScanReport = result.rawIncidents
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = result.rawIncidents,
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !restored -> "Daftar aman belum berhasil dipulihkan."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            }
                        )
                    }
                } catch (cancelled: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
                    throw cancelled
                } catch (security: SecurityException) {
                    latestRawScanReport = emptyList()
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, emptyList()).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = false,
                            errorMessage = "Akses Penggunaan ditolak Android. Periksa kembali izin di Pengaturan."
                        )
                    }
                } catch (_: Exception) {
                    val safe = runCatching { repository.safePackages() }
                        .getOrDefault(_uiState.value.safePackages)
                    _uiState.update {
                        reconcilePopupDetectorSafeList(it, safe, latestRawScanReport).copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            errorMessage = "Daftar aman gagal dipulihkan."
                        )
                    }
                }
            }
        }
    }
}
