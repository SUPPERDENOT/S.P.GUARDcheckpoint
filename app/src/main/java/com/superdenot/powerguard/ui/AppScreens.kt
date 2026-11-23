package com.superdenot.powerguard.ui

import android.content.ClipData
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.superdenot.powerguard.domain.engine.ChargingEtaEstimator
import com.superdenot.powerguard.domain.analytics.BatteryHealthEstimator
import com.superdenot.powerguard.domain.model.StatusBarParameter
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode
import com.superdenot.powerguard.domain.model.BatteryCurrentScale
import com.superdenot.powerguard.service.AutoStartDiagnosticLog
import com.superdenot.powerguard.ui.analytics.AnalyticsScreen
import com.superdenot.powerguard.ui.components.ProtocolInfoDialog
import com.superdenot.powerguard.ui.dashboard.ReadMoreInfoDialog
import com.superdenot.powerguard.ui.dashboard.selectDashboardLiveState
import com.superdenot.powerguard.ui.theme.*
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SuperdDashboardScreen(vm: PowerGuardViewModel, padding: PaddingValues, isActive: Boolean = true) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isActive) vm.refreshPermissionsAndUsage()
    }
    val service by vm.liveState.collectAsState()
    val fallback by vm.fallbackState.collectAsState()
    val running by vm.isServiceRunning.collectAsState()
    val serviceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
    val fallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val state = selectDashboardLiveState(running, service, fallback, serviceAt, fallbackAt, SystemClock.elapsedRealtime())
    val health by vm.latestHealth.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val monitoringEnabled by vm.batteryMonitoringEnabled.collectAsState()
    val batteryOpt by vm.isBatteryOptimizationIgnored.collectAsState()
    val usage by vm.hasUsagePermission.collectAsState()

    Box(Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("RINGKASAN DAYA", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Kondisi baterai saat ini", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Pembacaan langsung dari sistem perangkat", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("LEVEL BATERAI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text(if (state.isLevelAvailable) "${state.level}%" else "—", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        StatusPill(
                            if (!state.isChargingStatusAvailable) "STATUS TIDAK TERSEDIA" else if (state.isCharging) "MENGISI" else "TIDAK MENGISI",
                            if (!state.isChargingStatusAvailable) MaterialTheme.colorScheme.onSurfaceVariant else if (state.isCharging) EmeraldCharge else AmberThermal
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        if (state.isCharging) "Sumber: ${state.chargeSource}  •  Protokol / kemampuan sumber: ${state.chargingProtocol}" else "Perangkat sedang menggunakan daya baterai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniMetric("Arus baterai", if (state.isCurrentAvailable) "${state.currentMa} mA" else "—", Modifier.weight(1f))
                        MiniMetric(
                            "Pemantauan",
                            when {
                                running -> "Aktif"
                                !monitoringEnabled -> "OFF"
                                !state.isCharging -> "Siaga"
                                !batteryOpt -> "Izin diperlukan"
                                else -> "Menunggu sistem"
                            },
                            Modifier.weight(1f)
                        )
                    }
                    state.anomalyAlert?.let {
                        Surface(color = CrimsonAnomaly.copy(alpha = .10f), shape = RoundedCornerShape(10.dp)) {
                            Text("Perlu diperiksa: $it", Modifier.padding(12.dp), color = CrimsonAnomaly, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("PARAMETER BATERAI", "Nilai pengukuran yang tersedia")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Daya", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "—", Modifier.weight(1f))
                    MiniMetric("Tegangan", if (state.isVoltageAvailable) "${state.voltageMv} mV" else "—", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Suhu", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f°C", state.tempCelsius) else "—", Modifier.weight(1f))
                    MiniMetric("Sesi", "${state.sessionDurationSec / 60} menit", Modifier.weight(1f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading("KESEHATAN BATERAI", "Estimasi kapasitas dan acuan desain")
                    DiagnosticLine("Kapasitas terestimasi", health?.let { "${it.estimatedCapacityMah} mAh" } ?: "Belum tersedia")
                    DiagnosticLine("Estimasi kesehatan", health?.let { "${String.format(Locale.US, "%.1f", it.healthPercentage)}%" } ?: "Belum tersedia")
                    DiagnosticLine("Kapasitas desain", capacity.capacityMah?.let { "$it mAh" } ?: "Belum tersedia")
                    DiagnosticLine("Sumber", if (capacity.isManualOverride) "Manual" else "Otomatis")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading("KESIAPAN SISTEM", "Izin dan pemantauan latar belakang")
                    ReadinessLine(
                        "Battery Monitoring",
                        when {
                            running -> "AKTIF"
                            !monitoringEnabled -> "OFF"
                            !state.isCharging -> "SIAGA — menunggu charger"
                            !batteryOpt -> "OFF — izin latar belakang diperlukan"
                            else -> "OFF — menunggu aktivasi sistem"
                        },
                        when {
                            running -> EmeraldCharge
                            !monitoringEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            !state.isCharging -> AmberThermal
                            !batteryOpt -> CrimsonAnomaly
                            else -> AmberThermal
                        }
                    )
                    ReadinessLine("Optimasi baterai", if (batteryOpt) "PASS" else "BELUM", if (batteryOpt) EmeraldCharge else CrimsonAnomaly)
                    ReadinessLine("Akses pengguna", if (usage) "PASS" else "BELUM", if (usage) EmeraldCharge else CrimsonAnomaly)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    }
}

@Composable
fun ChargingScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val service by vm.liveState.collectAsState()
    val fallback by vm.fallbackState.collectAsState()
    val running by vm.isServiceRunning.collectAsState()
    val serviceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
    val fallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val state = selectDashboardLiveState(running, service, fallback, serviceAt, fallbackAt, SystemClock.elapsedRealtime())
    var showProtocolInfo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DIAGNOSTIK PENGISIAN", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text(
                    "Analisis Pengisian",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("LEVEL BATERAI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text(if (state.isLevelAvailable) "${state.level}%" else "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(if (!state.isChargingStatusAvailable) "STATUS TIDAK TERSEDIA" else if (state.isCharging) "MENGISI" else "TIDAK MENGISI", if (!state.isChargingStatusAvailable) MaterialTheme.colorScheme.onSurfaceVariant else if (state.isCharging) EmeraldCharge else AmberThermal)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DiagnosticLine("Sumber daya", if (state.isCharging) state.chargeSource else "Tidak terhubung")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Protokol / kemampuan sumber",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { showProtocolInfo = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = "Penjelasan protokol pengisian",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Text(
                            state.chargingProtocol,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            maxLines = 2
                        )
                    }
                    DiagnosticLine(
                        "Estimasi penuh",
                        if (state.isCharging && state.isChargingEtaTracking) ChargingEtaEstimator.formatEta(state.chargingEtaMinutes)
                        else if (state.isCharging) "Menunggu pemantauan"
                        else "Tidak mengisi"
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "PEMBACAAN BATERAI • SISI BATERAI",
                    color = CyanFastCharge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "Daya merupakan hasil tegangan × arus baterai, bukan batas kemampuan sumber.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Arus", if (state.isCurrentAvailable) "${state.currentMa} mA" else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Daya baterai", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "Tidak tersedia", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Tegangan", if (state.isVoltageAvailable) "${state.voltageMv} mV" else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Suhu", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f°C", state.tempCelsius) else "Tidak tersedia", Modifier.weight(1f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "RINGKASAN SESI",
                        color = CyanFastCharge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    DiagnosticLine("Muatan bersih tercatat", String.format(Locale.US, "%+.2f mAh", state.sessionMahDelivered))
                    DiagnosticLine("Durasi sesi", "${state.sessionDurationSec / 60} menit")
                    DiagnosticLine("CPU deep sleep", "${(state.deepSleepRatio * 100).toInt()}%")
                }
            }
        }
    }
    if (showProtocolInfo) {
        ProtocolInfoDialog(
            protocolLabel = state.chargingProtocol,
            onDismiss = { showProtocolInfo = false }
        )
    }
}

@Composable
fun InsightsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val sessions by vm.sessions.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val designCapacityMah = capacity.capacityMah ?: BatteryHealthEstimator.DEFAULT_DESIGN_CAPACITY_MAH
    val estimate = remember(sessions, designCapacityMah) {
        BatteryHealthEstimator.estimate(sessions, designCapacityMah)
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("INSIGHTS", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("Pola penggunaan daya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ESTIMASI KESEHATAN BATERAI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Estimated SOH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            estimate?.let { String.format(Locale.US, "%.1f%%", it.healthPercentage) } ?: "Belum tersedia",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("mAh Efektif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            estimate?.let { "${it.effectiveCapacityMah} mAh" } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    if (estimate != null) "Berdasarkan ${estimate.validSessionCount} sesi selesai • Kapasitas desain $designCapacityMah mAh (${if (capacity.isManualOverride) "manual" else "otomatis/default"})"
                    else "Belum cukup sesi valid. Estimasi memerlukan sesi selesai dengan kenaikan SoC minimal 15%. Kapasitas desain $designCapacityMah mAh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Nilai ini merupakan estimasi berbasis data sesi, bukan pengukuran kapasitas laboratorium.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(Modifier.weight(1f)) {
            AnalyticsScreen(viewModel = vm, contentPadding = PaddingValues(0.dp), initialSubTab = 1, visibleSubTabs = listOf(1, 2))
        }
    }
}

@Composable
fun HistoryScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("RIWAYAT", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("Sesi dan kurva pengisian", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        AnalyticsScreen(viewModel = vm, contentPadding = PaddingValues(0.dp), initialSubTab = 0, visibleSubTabs = listOf(0))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: PowerGuardViewModel, padding: PaddingValues, onOpenPermissionSetup: () -> Unit = {}) {
    val context = LocalContext.current
    val monitoringEnabled by vm.batteryMonitoringEnabled.collectAsState()
    val error by vm.autoMonitoringError.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val statusBarSetting by vm.statusBarIndicatorSetting.collectAsState()
    val currentScale by vm.batteryCurrentScale.collectAsState()
    var showStatusBarSettingsDialog by remember { mutableStateOf(false) }
    var rotationIntervalInput by remember(statusBarSetting.rotationIntervalSeconds) { mutableStateOf(statusBarSetting.rotationIntervalSeconds.toString()) }
    var currentScaleMenuExpanded by remember { mutableStateOf(false) }
    var showCapacityDialog by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var isExportingDiagnosticLog by remember { mutableStateOf(false) }
    val screenScope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(start = 18.dp, end = 18.dp, top = 4.dp),
        contentPadding = PaddingValues(
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PENGATURAN", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Preferensi aplikasi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Battery Monitoring", fontWeight = FontWeight.Bold)
                            Text(
                                "Pantau otomatis saat charger tersambung. OFF akan menghentikan layanan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = monitoringEnabled, onCheckedChange = vm::setBatteryMonitoringEnabled)
                    }
                    error?.let { Text(it, color = CrimsonAnomaly, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).clickable { showStatusBarSettingsDialog = true },
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Indikator status bar", fontWeight = FontWeight.Bold)
                            Text(
                                if (statusBarSetting.displayMode == com.superdenot.powerguard.domain.model.StatusBarDisplayMode.CUSTOM_ROTATION) {
                                    "Rotasi kustom • ${statusBarSetting.rotationParameters.joinToString(" / ") { it.label }} • ${statusBarSetting.rotationIntervalSeconds} dtk"
                                } else {
                                    "Satu metrik • ${statusBarSetting.parameter.label}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = statusBarSetting.enabled, onCheckedChange = vm::setStatusBarIndicatorEnabled)
                    }
                    TextButton(onClick = { showStatusBarSettingsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Atur indikator")
                    }
                    Text(
                        "Perubahan diterapkan pada notifikasi pemantauan yang sedang aktif. Tampilan ikon dapat berbeda menurut versi Android dan perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kalibrasi skala pembacaan sensor", fontWeight = FontWeight.Bold)
                    Text("Mengoreksi arus BatteryManager CURRENT_NOW dan daya sisi baterai hasil V × I. Default 1x mempertahankan pembacaan standar. Ubah hanya jika hasil sensor terbukti berbeda faktor 1.000.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        OutlinedButton(onClick = { currentScaleMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(currentScale.label) }
                        DropdownMenu(expanded = currentScaleMenuExpanded, onDismissRequest = { currentScaleMenuExpanded = false }) {
                            BatteryCurrentScale.values().forEach { scale ->
                                DropdownMenuItem(text = { Text(scale.label) }, onClick = {
                                    vm.setBatteryCurrentScale(scale); currentScaleMenuExpanded = false
                                })
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kapasitas baterai", fontWeight = FontWeight.Bold)
                    Text(
                        "Design capacity: ${capacity.capacityMah?.let { "$it mAh" } ?: "Tidak tersedia"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Source: ${if (capacity.isManualOverride) "Manual" else "Automatic"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { showCapacityDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Edit, null)
                        Text("  Edit Design Capacity")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Persiapan Izin", fontWeight = FontWeight.Bold)
                    Text("Periksa dan atur akses Android yang mendukung pemantauan serta analisis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenPermissionSetup, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Settings, null)
                        Text("  Buka Initial Setup")
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Diagnostik Auto Start", fontWeight = FontWeight.Bold)
                    Text(
                        "Simpan catatan teknis aktivasi monitoring ke file lokal untuk dianalisis. Tidak berisi data sensor atau konten pribadi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        enabled = !isExportingDiagnosticLog,
                        onClick = {
                            isExportingDiagnosticLog = true
                            screenScope.launch {
                                try {
                                    val snapshot = AutoStartDiagnosticLog.createExportSnapshot(context)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        snapshot
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        clipData = ClipData.newUri(
                                            context.contentResolver,
                                            "Auto Start Diagnostic Log",
                                            uri
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan diagnostic log"))
                                } catch (error: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Gagal menyiapkan diagnostic log: ${error.javaClass.simpleName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isExportingDiagnosticLog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isExportingDiagnosticLog) "Menyiapkan log..." else "Bagikan Diagnostic Log")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personalisasi", fontWeight = FontWeight.Bold)
                    ComingSoonButton("Studio nada peringatan") { comingSoon = true }
                    ComingSoonButton("Profil peringatan khusus") { comingSoon = true }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showInfo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Info, null)
                Text("  Tentang SUPERD POWER GUARD")
            }
        }
    }
    if (comingSoon) ComingSoonDialog { comingSoon = false }
    if (showInfo) ReadMoreInfoDialog { showInfo = false }
    if (showStatusBarSettingsDialog) {
        var draftMode by remember { mutableStateOf(statusBarSetting.displayMode) }
        var draftParameter by remember { mutableStateOf(statusBarSetting.parameter) }
        var draftRotationParameters by remember { mutableStateOf(statusBarSetting.rotationParameters.toSet()) }
        var draftIntervalSeconds by remember { mutableIntStateOf(statusBarSetting.rotationIntervalSeconds) }
        var draftIntervalInput by remember { mutableStateOf(statusBarSetting.rotationIntervalSeconds.toString()) }

        val validCustomInterval = draftIntervalInput.toIntOrNull()?.let { it in 1..60 } == true
        val canSave = draftMode != StatusBarDisplayMode.CUSTOM_ROTATION ||
            (draftRotationParameters.size >= 2 && validCustomInterval)

        Dialog(onDismissRequest = { showStatusBarSettingsDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Indikator status bar",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Mode tampilan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = draftMode == StatusBarDisplayMode.SINGLE_METRIC,
                                onClick = { draftMode = StatusBarDisplayMode.SINGLE_METRIC },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Satu metrik", maxLines = 1, fontSize = 12.sp)
                            }
                            SegmentedButton(
                                selected = draftMode == StatusBarDisplayMode.CUSTOM_ROTATION,
                                onClick = { draftMode = StatusBarDisplayMode.CUSTOM_ROTATION },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Rotasi kustom", maxLines = 1, fontSize = 12.sp)
                            }
                        }

                        Text(
                            "Pilihan parameter",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBarParameterGrid(
                            selectedParameters = if (draftMode == StatusBarDisplayMode.SINGLE_METRIC) {
                                setOf(draftParameter)
                            } else {
                                draftRotationParameters
                            },
                            multiSelect = draftMode == StatusBarDisplayMode.CUSTOM_ROTATION,
                            canDeselectSelected = draftRotationParameters.size > 2,
                            onParameterClick = { parameter ->
                                if (draftMode == StatusBarDisplayMode.SINGLE_METRIC) {
                                    draftParameter = parameter
                                } else {
                                    val updated = draftRotationParameters.toMutableSet()
                                    if (parameter in updated) {
                                        if (updated.size > 2) updated.remove(parameter)
                                    } else {
                                        updated.add(parameter)
                                    }
                                    if (updated.size >= 2) draftRotationParameters = updated
                                }
                            }
                        )

                        if (draftMode == StatusBarDisplayMode.CUSTOM_ROTATION) {
                            Text(
                                "Interval rotasi (detik)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 5, 10).forEach { seconds ->
                                    val selected = draftIntervalSeconds == seconds
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .selectable(
                                                selected = selected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    draftIntervalSeconds = seconds
                                                    draftIntervalInput = seconds.toString()
                                                }
                                            ),
                                        shape = MaterialTheme.shapes.small,
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selected) 1.5.dp else 1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "$seconds dtk",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("Interval kustom", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Rentang 1–60 detik",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("Detik", style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(
                                        value = draftIntervalInput,
                                        onValueChange = { input ->
                                            draftIntervalInput = input.filter(Char::isDigit).take(2)
                                            draftIntervalInput.toIntOrNull()
                                                ?.takeIf { it in 1..60 }
                                                ?.let { draftIntervalSeconds = it }
                                        },
                                        modifier = Modifier.width(92.dp),
                                        singleLine = true,
                                        isError = !validCustomInterval,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                            if (!validCustomInterval) {
                                Text(
                                    "Masukkan angka 1–60 untuk menyimpan interval.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showStatusBarSettingsDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = canSave,
                            onClick = {
                                vm.setStatusBarParameter(draftParameter)
                                if (draftMode == StatusBarDisplayMode.CUSTOM_ROTATION) {
                                    vm.setStatusBarRotationParameters(draftRotationParameters.toList())
                                    vm.setStatusBarRotationIntervalSeconds(draftIntervalSeconds)
                                }
                                vm.setStatusBarDisplayMode(draftMode)
                                showStatusBarSettingsDialog = false
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    if (showCapacityDialog) {
        var inputText by remember(capacity.capacityMah) {
            mutableStateOf(capacity.capacityMah?.toString().orEmpty())
        }
        AlertDialog(
            onDismissRequest = { showCapacityDialog = false },
            title = { Text("Kapasitas Desain Baterai") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Masukkan kapasitas desain resmi baterai (1000–20000 mAh). Nilai manual digunakan sampai Anda kembali ke Automatic.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.filter(Char::isDigit) },
                        label = { Text("Kapasitas (mAh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = inputText.toIntOrNull()?.let { it in 1000..20000 } == true,
                    onClick = {
                        inputText.toIntOrNull()?.let(vm::updateDesignCapacity)
                        showCapacityDialog = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.useAutomaticDesignCapacity()
                        showCapacityDialog = false
                    }) { Text("Gunakan Automatic") }
                    TextButton(onClick = { showCapacityDialog = false }) { Text("Batal") }
                }
            }
        )
    }
}


@Composable
private fun StatusBarParameterGrid(
    selectedParameters: Set<StatusBarParameter>,
    multiSelect: Boolean,
    canDeselectSelected: Boolean,
    onParameterClick: (StatusBarParameter) -> Unit
) {
    val rows = listOf(
        listOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT),
        listOf(StatusBarParameter.VOLTAGE, StatusBarParameter.TEMPERATURE),
        listOf(StatusBarParameter.BATTERY_PERCENTAGE, StatusBarParameter.RESOURCE_SOURCE)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { parameter ->
                    val selected = parameter in selectedParameters
                    val enabled = !multiSelect || !selected || canDeselectSelected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selected,
                                enabled = enabled,
                                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                                onClick = { onParameterClick(parameter) }
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when (parameter) {
                                    StatusBarParameter.POWER -> "Daya (W)"
                                    StatusBarParameter.CURRENT -> "Arus (A/mA)"
                                    StatusBarParameter.VOLTAGE -> "Tegangan (V)"
                                    StatusBarParameter.TEMPERATURE -> "Suhu (°C)"
                                    StatusBarParameter.BATTERY_PERCENTAGE -> "Level (%)"
                                    StatusBarParameter.RESOURCE_SOURCE -> "Sumber (AC/USB)"
                                    StatusBarParameter.CHARGING_STATUS -> "Status"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReadinessLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DiagnosticLine(
    label: String,
    value: String,
    valueWeight: Float = 1f,
    valueMaxLines: Int = Int.MAX_VALUE,
    valueSoftWrap: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = valueMaxLines,
            softWrap = valueSoftWrap,
            modifier = Modifier.weight(valueWeight)
        )
    }
}

@Composable
private fun ExpandPanel(id: String, title: String, subtitle: String, expanded: Set<String>, onToggle: () -> Unit, body: @Composable () -> Unit) {
    val open = id in expanded
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CyanFastCharge)
            }
            if (open) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { body() }
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(label: String, accent: Color) {
    Surface(color = accent.copy(alpha = .14f), shape = RoundedCornerShape(50)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ComingSoonButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text(label, Modifier.weight(1f))
        StatusPill("COMING SOON", AmberThermal)
    }
}

@Composable
fun ComingSoonDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Bolt, null, tint = CyanFastCharge, modifier = Modifier.size(30.dp)) },
        title = { Text("Coming Soon", fontWeight = FontWeight.Bold) },
        text = { Text("Fitur ini sedang disiapkan dan akan tersedia pada pembaruan SUPERD POWER GUARD berikutnya.") },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanFastCharge)) {
                Text("Mengerti", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
