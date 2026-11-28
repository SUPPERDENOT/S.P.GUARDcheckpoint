package com.superdenot.powerguard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.superdenot.powerguard.data.repository.PopupDetectorRepository
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupSimulationScenario
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
    val simulationActive: Boolean = false
)

internal class PopupDetectorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PopupDetectorRepository(application)
    private val _uiState = MutableStateFlow(
        PopupDetectorUiState(
            hasUsagePermission = repository.hasUsagePermission(),
            safePackages = repository.safePackages(),
            isLoadingArchive = true
        )
    )
    val uiState: StateFlow<PopupDetectorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                _uiState.update { current ->
                    if (current.isLoadingArchive) {
                        current.copy(archivedIncidents = archive, isLoadingArchive = false)
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingArchive = false, errorMessage = "Arsip deteksi belum dapat dimuat.")
                }
            }
        }
    }

    private var latestActualIncidents: List<PopupIncident> = emptyList()

    private val scanMutex = Mutex()

    fun scan() {
        viewModelScope.launch {
            scanMutex.withLock { performScanLocked() }
        }
    }

    private suspend fun performScanLocked() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
        try {
            val result = withContext(Dispatchers.IO) { repository.scan() }
            latestActualIncidents = result.incidents.distinctBy { it.packageName }
            val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
            val safe = withContext(Dispatchers.IO) { repository.safePackages() }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLoadingArchive = false,
                    hasUsagePermission = result.hasUsagePermission,
                    hasActivityData = result.hasActivityData,
                    currentIncidents = result.incidents,
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
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    isLoadingArchive = false,
                    errorMessage = "Pemindaian belum dapat diselesaikan. Periksa akses penggunaan lalu coba lagi."
                )
            }
        }
    }

    fun showSimulation() {
        val simulated = PopupSimulationScenario.evaluate(System.currentTimeMillis())
        _uiState.update {
            it.copy(
                currentIncidents = (latestActualIncidents + simulated).distinctBy { it.packageName },
                simulationActive = true,
                errorMessage = null
            )
        }
    }

    fun clearSimulation() {
        _uiState.update {
            it.copy(
                currentIncidents = withoutSimulatedPopupIncidents(it.currentIncidents),
                simulationActive = false
            )
        }
    }

    fun markSafe(incident: PopupIncident) {
        if (incident.isSimulated) {
            _uiState.update {
                it.copy(currentIncidents = it.currentIncidents.filterNot { item ->
                    item.isSimulated && item.packageName == incident.packageName
                })
            }
            return
        }
        viewModelScope.launch {
            scanMutex.withLock {
                val previousState = _uiState.value
                val previousActualIncidents = latestActualIncidents
                _uiState.update {
                    it.copy(
                        isScanning = true,
                        isLoadingArchive = false,
                        currentIncidents = it.currentIncidents.filterNot { item ->
                            !item.isSimulated && item.packageName == incident.packageName
                        },
                        archivedIncidents = it.archivedIncidents.filterNot { item ->
                            item.packageName == incident.packageName
                        },
                        safePackages = it.safePackages + incident.packageName,
                        errorMessage = null,
                        simulationActive = false
                    )
                }
                try {
                    val saved = withContext(Dispatchers.IO) {
                        repository.markSafe(incident.packageName)
                    }
                    // Do not drop the rescan if another scan was already active.
                    val result = withContext(Dispatchers.IO) { repository.scan() }
                    latestActualIncidents = result.incidents.distinctBy { it.packageName }
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update { state ->
                        val simulatedItems = if (previousState.simulationActive) {
                            previousState.currentIncidents.filter { it.isSimulated }
                        } else {
                            emptyList()
                        }
                        state.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = (result.incidents + simulatedItems).distinctBy { it.packageName },
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !saved -> "Aplikasi belum berhasil ditambahkan ke daftar aman."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            },
                            simulationActive = previousState.simulationActive
                        )
                    }
                } catch (cancelled: CancellationException) {
                    latestActualIncidents = previousActualIncidents
                    _uiState.value = previousState.copy(isScanning = false, isLoadingArchive = false)
                    throw cancelled
                } catch (_: Exception) {
                    latestActualIncidents = previousActualIncidents
                    _uiState.value = previousState.copy(
                        isScanning = false,
                        isLoadingArchive = false,
                        errorMessage = "Daftar aman gagal diperbarui."
                    )
                }
            }
        }
    }

    fun restoreSafePackage(packageName: String) {
        viewModelScope.launch {
            scanMutex.withLock {
                _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
                try {
                    val (restored, result) = withContext(Dispatchers.IO) {
                        val didRestore = repository.restoreSafePackage(packageName)
                        didRestore to repository.scan()
                    }
                    latestActualIncidents = result.incidents.distinctBy { it.packageName }
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = result.incidents,
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !restored -> "Aplikasi belum berhasil dipulihkan dari daftar aman."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            },
                            simulationActive = false
                        )
                    }
                } catch (cancelled: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(
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
                _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
                try {
                    val (safeListRestored, result) = withContext(Dispatchers.IO) {
                        val restored = repository.restoreSafePackages()
                        restored to repository.scan()
                    }
                    latestActualIncidents = result.incidents.distinctBy { it.packageName }
                    val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                    val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isLoadingArchive = false,
                            hasUsagePermission = result.hasUsagePermission,
                            hasActivityData = result.hasActivityData,
                            currentIncidents = result.incidents,
                            archivedIncidents = archive,
                            safePackages = safe,
                            lastScanAtMs = result.scannedAtMs,
                            errorMessage = when {
                                !safeListRestored -> "Daftar aman belum berhasil dipulihkan."
                                !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                                else -> null
                            }
                        )
                    }
                } catch (cancelled: CancellationException) {
                    _uiState.update { it.copy(isScanning = false, isLoadingArchive = false) }
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(isScanning = false, isLoadingArchive = false, errorMessage = "Daftar aman gagal dipulihkan.")
                    }
                }
            }
        }
    }

}
