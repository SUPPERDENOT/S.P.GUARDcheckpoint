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
import kotlinx.coroutines.withContext

internal data class PopupDetectorUiState(
    val isScanning: Boolean = false,
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
            archivedIncidents = repository.archivedIncidents(),
            safePackages = repository.safePackages()
        )
    )
    val uiState: StateFlow<PopupDetectorUiState> = _uiState.asStateFlow()
    private var latestActualIncidents: List<PopupIncident> = emptyList()

    fun scan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
            try {
                val result = withContext(Dispatchers.IO) { repository.scan() }
                latestActualIncidents = result.incidents
                val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                _uiState.update {
                    it.copy(
                        isScanning = false,
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
                _uiState.update { it.copy(isScanning = false) }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Pemindaian belum dapat diselesaikan. Periksa akses penggunaan lalu coba lagi."
                    )
                }
            }
        }
    }

    fun showSimulation() {
        val simulated = PopupSimulationScenario.evaluate(System.currentTimeMillis())
        _uiState.update {
            it.copy(
                currentIncidents = latestActualIncidents + simulated,
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
            _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
            try {
                val (safeListSaved, result) = withContext(Dispatchers.IO) {
                    val saved = repository.markSafe(incident.packageName)
                    saved to repository.scan()
                }
                latestActualIncidents = result.incidents
                val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        hasUsagePermission = result.hasUsagePermission,
                        hasActivityData = result.hasActivityData,
                        currentIncidents = result.incidents,
                        archivedIncidents = archive,
                        safePackages = safe,
                        lastScanAtMs = result.scannedAtMs,
                        errorMessage = when {
                            !safeListSaved -> "Aplikasi belum berhasil ditambahkan ke daftar aman."
                            !result.archivePersisted -> "Hasil pemindaian tersedia, tetapi arsip tidak berhasil disimpan."
                            else -> null
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isScanning = false) }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isScanning = false, errorMessage = "Daftar aman gagal diperbarui.")
                }
            }
        }
    }

    fun restoreSafeList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null, simulationActive = false) }
            try {
                val (safeListRestored, result) = withContext(Dispatchers.IO) {
                    val restored = repository.restoreSafePackages()
                    restored to repository.scan()
                }
                latestActualIncidents = result.incidents
                val archive = withContext(Dispatchers.IO) { repository.archivedIncidents() }
                val safe = withContext(Dispatchers.IO) { repository.safePackages() }
                _uiState.update {
                    it.copy(
                        isScanning = false,
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
                _uiState.update { it.copy(isScanning = false) }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isScanning = false, errorMessage = "Daftar aman gagal dipulihkan.") }
            }
        }
    }
}
