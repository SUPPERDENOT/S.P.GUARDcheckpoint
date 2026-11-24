package com.superdenot.powerguard.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.superdenot.powerguard.domain.engine.ChargingEtaEstimator
import com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting
import com.superdenot.powerguard.domain.analytics.ChargingThermalPhaseAnalyzer
import com.superdenot.powerguard.domain.analytics.ChargingThermalSample
import com.superdenot.powerguard.domain.analytics.BatteryHealthSummaryMapper
import com.superdenot.powerguard.domain.engine.ChargeGuardConfiguration
import com.superdenot.powerguard.domain.engine.ChargeGuardRepeatMode
import com.superdenot.powerguard.domain.engine.ChargeGuardSettingsSummaryFormatter
import com.superdenot.powerguard.data.repository.ChargeGuardAlertSoundSetting
import com.superdenot.powerguard.data.repository.ChargeGuardVibrationPattern
import com.superdenot.powerguard.ui.components.CompactSwitch
import com.superdenot.powerguard.ui.components.BatteryGaugeView
import com.superdenot.powerguard.ui.components.ThemePickerDialogs
import com.superdenot.powerguard.ui.components.resolveBatteryGaugeColor
import com.superdenot.powerguard.domain.model.StatusBarParameter
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode
import com.superdenot.powerguard.domain.model.BatteryCurrentScale
import com.superdenot.powerguard.domain.model.StatusBarSettingsSummaryFormatter
import com.superdenot.powerguard.ui.analytics.AnalyticsScreen
import com.superdenot.powerguard.ui.components.ProtocolInfoDialog
import com.superdenot.powerguard.ui.dashboard.ReadMoreInfoDialog
import com.superdenot.powerguard.ui.dashboard.selectDashboardLiveState
import com.superdenot.powerguard.ui.theme.*
import com.superdenot.powerguard.ui.theme.LocalBatterySemanticColors
import com.superdenot.powerguard.ui.theme.LocalBatteryMetricTints
import com.superdenot.powerguard.ui.theme.InstrumentGraphite
import com.superdenot.powerguard.service.LiveStatusBarRenderer
import com.superdenot.powerguard.ui.components.ChargingThermalSummaryPanel
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.util.Locale
import kotlinx.coroutines.delay

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
    val healthSummary = remember(health) {
        health?.let { BatteryHealthSummaryMapper.map(it.estimatedCapacityMah, it.healthPercentage, it.designCapacityMah) }
    }
    val capacity by vm.designCapacitySetting.collectAsState()
    val monitoringEnabled by vm.batteryMonitoringEnabled.collectAsState()
    val batteryOpt by vm.isBatteryOptimizationIgnored.collectAsState()
    val usage by vm.hasUsagePermission.collectAsState()

    val isCriticalThermal = state.isTemperatureAvailable &&
        state.tempCelsius >= ChargingThermalPhaseAnalyzer.HIGH_TEMPERATURE_CELSIUS
    val isWarm = state.isTemperatureAvailable &&
        state.tempCelsius >= ChargingThermalPhaseAnalyzer.WARM_TEMPERATURE_CELSIUS && !isCriticalThermal
    val gaugeStatus = when {
        isCriticalThermal -> "Suhu Tinggi"
        isWarm -> "Hangat"
        state.isChargingStatusAvailable && state.isCharging -> "Mengisi"
        state.isChargingStatusAvailable && state.isLevelAvailable && state.level >= 100 && !state.isCharging -> "Penuh"
        running -> "Aktif"
        else -> "Siaga"
    }
    val gaugeColor = resolveBatteryGaugeColor(
        LocalBatterySemanticColors.current, isCriticalThermal, isWarm,
        state.isChargingStatusAvailable && state.isCharging
    )
    val metricTints = LocalBatteryMetricTints.current
    val themePackConfig by vm.themePackConfig.collectAsState()
    val eta = state.chargingEtaMinutes
        ?.takeIf { state.isCharging && state.isChargingEtaTracking && it > 0L }
        ?.let { ChargingEtaEstimator.formatEta(it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatteryGaugeView(
                        progress = if (state.isLevelAvailable) state.level.coerceIn(0, 100) / 100f else 0f,
                        percentageLabel = if (state.isLevelAvailable) "${state.level}%" else "—",
                        model = themePackConfig.gaugeModel,
                        animation = themePackConfig.chargingAnimation,
                        isCharging = state.isChargingStatusAvailable && state.isCharging && !isCriticalThermal && !isWarm,
                        isCriticalThermal = isCriticalThermal,
                        isWarm = isWarm,
                        isVisibleOnScreen = isActive,
                        modifier = Modifier.size(112.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(gaugeColor, RoundedCornerShape(50)))
                            Spacer(Modifier.width(6.dp))
                            Text(gaugeStatus, style = MaterialTheme.typography.labelLarge,
                                color = gaugeColor, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroMetric("Daya", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "—", Modifier.weight(1f), metricTints.cold)
                            HeroMetric("Arus", if (state.isCurrentAvailable) "${state.currentMa} mA" else "—", Modifier.weight(1f), metricTints.cold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroMetric("Suhu", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f °C", state.tempCelsius) else "—", Modifier.weight(1f), metricTints.warm)
                            HeroMetric("Tegangan", if (state.isVoltageAvailable) String.format(Locale.US, "%.2f V", state.voltageMv / 1000.0) else "—", Modifier.weight(1f), metricTints.cold)
                        }
                        eta?.let {
                            Text("Estimasi penuh · $it", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        state.anomalyAlert?.let {
                            Text("Perlu diperiksa: $it", style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAnomaly, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionHeading("KESEHATAN BATERAI (SOH)", "Estimasi kapasitas dan acuan desain")
                    DiagnosticLine("Kapasitas terestimasi", healthSummary?.let { "${it.estimatedCapacityMah} mAh" } ?: "Belum tersedia")
                    DiagnosticLine("Estimasi SOH", healthSummary?.let { "${String.format(Locale.US, "%.1f", it.healthPercentage)}%" } ?: "Belum tersedia")
                    DiagnosticLine("Kapasitas desain", capacity.capacityMah?.let { "$it mAh" } ?: "Belum tersedia")
                    DiagnosticLine("Sumber", if (capacity.isManualOverride) "Manual" else "Otomatis")
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionHeading("KESIAPAN SISTEM", "Izin dan pemantauan latar belakang")
                    ReadinessLine(
                        "Pemantauan Baterai",
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
                    ReadinessLine("Optimasi baterai", if (batteryOpt) "SIAP" else "BELUM", if (batteryOpt) EmeraldCharge else CrimsonAnomaly)
                    ReadinessLine("Akses pengguna", if (usage) "SIAP" else "BELUM", if (usage) EmeraldCharge else CrimsonAnomaly)
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent
) {
    Column(
        modifier = modifier.background(containerColor, RoundedCornerShape(8.dp)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
            maxLines = 1, softWrap = false)
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
    val currentSamples by vm.currentSessionSamples.collectAsState()
    val thermalSummary = remember(currentSamples) {
        ChargingThermalPhaseAnalyzer.analyze(currentSamples.map { sample ->
            ChargingThermalSample(sample.timestamp, sample.tempCelsius, sample.isTemperatureAvailable,
                sample.currentMa, sample.isCurrentAvailable, sample.voltageMv, sample.isVoltageAvailable,
                sample.isChargingStatusAvailable, sample.isCharging)
        })
    }
    var showProtocolInfo by remember { mutableStateOf(false) }
    var showPowerInfo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PEMBACAAN BATERAI • SISI BATERAI",
                        modifier = Modifier.weight(1f),
                        color = CyanFastCharge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(onClick = { showPowerInfo = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = "Informasi pengukuran daya baterai",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Arus", if (state.isCurrentAvailable) "${state.currentMa} mA" else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Daya baterai", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "Tidak tersedia", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Tegangan", if (state.isVoltageAvailable) String.format(Locale.US, "%.2f V (%d mV)", state.voltageMv / 1000.0, state.voltageMv) else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Suhu", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f°C", state.tempCelsius) else "Tidak tersedia", Modifier.weight(1f))
                }
            }
        }
        item {
            ChargingThermalSummaryPanel(thermalSummary)
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
                    DiagnosticLine("Mode tidur CPU", "${(state.deepSleepRatio * 100).toInt()}%")
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
    if (showPowerInfo) {
        AlertDialog(
            onDismissRequest = { showPowerInfo = false },
            title = { Text("Pengukuran daya baterai") },
            text = { Text("Daya merupakan hasil tegangan × arus pada sisi baterai. Nilai ini bukan batas kemampuan input charger dan tidak mengidentifikasi protokol pengisian.") },
            confirmButton = { TextButton(onClick = { showPowerInfo = false }) { Text("Mengerti") } }
        )
    }
}

@Composable
fun InsightsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val health by vm.latestHealth.collectAsState()
    val healthSummary = remember(health) {
        health?.let { BatteryHealthSummaryMapper.map(it.estimatedCapacityMah, it.healthPercentage, it.designCapacityMah) }
    }
    val capacity by vm.designCapacitySetting.collectAsState()

    Column(Modifier.fillMaxSize().padding(padding).padding(top = 8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ESTIMASI SOH BATERAI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Estimasi SOH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            healthSummary?.let { String.format(Locale.US, "%.1f%%", it.healthPercentage) } ?: "Belum tersedia",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("mAh Efektif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            healthSummary?.let { "${it.estimatedCapacityMah} mAh" } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    if (healthSummary != null) "Snapshot SOH tersimpan • Kapasitas desain ${healthSummary.designCapacityMah} mAh (${if (capacity.isManualOverride) "manual" else "otomatis/default"})"
                    else "Belum ada snapshot SOH yang valid. Estimasi akan tersedia setelah sesi pengisian memenuhi kriteria.",
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
    Column(Modifier.fillMaxSize().padding(padding).padding(top = 8.dp)) {
        AnalyticsScreen(viewModel = vm, contentPadding = PaddingValues(0.dp), initialSubTab = 0, visibleSubTabs = listOf(0))
    }
}

@Composable
fun SettingsScreen(vm: PowerGuardViewModel, padding: PaddingValues, onOpenPermissionSetup: () -> Unit = {}) {
    val context = LocalContext.current
    val chargeGuardConfig by vm.chargeGuardConfig.collectAsState()
    val chargeGuardAlertSound by vm.chargeGuardAlertSound.collectAsState()
    val monitoringEnabled by vm.batteryMonitoringEnabled.collectAsState()
    val error by vm.autoMonitoringError.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val statusBarSetting by vm.statusBarIndicatorSetting.collectAsState()
    val currentScale by vm.batteryCurrentScale.collectAsState()
    val themeConfig by vm.themePackConfig.collectAsState()
    val previewService by vm.liveState.collectAsState()
    val previewFallback by vm.fallbackState.collectAsState()
    val previewRunning by vm.isServiceRunning.collectAsState()
    val previewServiceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
    val previewFallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val previewBatteryState = selectDashboardLiveState(
        previewRunning, previewService, previewFallback, previewServiceAt, previewFallbackAt, SystemClock.elapsedRealtime()
    )
    var showStatusBarSettingsDialog by remember { mutableStateOf(false) }
    var showCapacityDialog by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showChargeGuardDialog by remember { mutableStateOf(false) }
    var draftChargeGuardConfig by remember { mutableStateOf(chargeGuardConfig) }
    var draftChargeGuardSound by remember { mutableStateOf(chargeGuardAlertSound) }
    var pendingExternalAudioUri by remember { mutableStateOf<Uri?>(null) }
    var lastCustomTapAt by remember { mutableLongStateOf(0L) }

    fun openChargeGuardDialog() {
        draftChargeGuardConfig = chargeGuardConfig
        draftChargeGuardSound = chargeGuardAlertSound
        lastCustomTapAt = 0L
        showChargeGuardDialog = true
    }

    fun applyPickedTone(pickedUri: Uri) {
        draftChargeGuardSound = when {
            pickedUri == Settings.System.DEFAULT_NOTIFICATION_URI -> draftChargeGuardSound.copy(
                selection = ChargeGuardAlertSoundSetting.DEFAULT, title = "Nada Default"
            )
            else -> {
                val title = runCatching { RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context) }
                    .getOrNull()?.takeIf { it.isNotBlank() } ?: "Nada pilihan"
                draftChargeGuardSound.copy(selection = pickedUri.toString(), title = title)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingExternalAudioUri
        pendingExternalAudioUri = null
        if (granted && pending != null) applyPickedTone(pending)
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (pickedUri == null) {
                draftChargeGuardSound = draftChargeGuardSound.copy(
                    selection = ChargeGuardAlertSoundSetting.SILENT, title = "Hening"
                )
            } else {
                val permission = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_AUDIO
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Manifest.permission.READ_EXTERNAL_STORAGE
                    else -> null
                }
                val isExternalMedia = pickedUri.scheme == "content" && pickedUri.authority == "media"
                val needsPermission = isExternalMedia && permission != null &&
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    pendingExternalAudioUri = pickedUri
                    audioPermissionLauncher.launch(permission!!)
                } else {
                    applyPickedTone(pickedUri)
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(start = 18.dp, end = 18.dp, top = 8.dp),
        contentPadding = PaddingValues(
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pemantauan Baterai", fontWeight = FontWeight.Bold)
                            Text(
                                "Pantau otomatis saat charger tersambung. OFF akan menghentikan layanan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CompactSwitch(checked = monitoringEnabled, onCheckedChange = vm::setBatteryMonitoringEnabled,
                            contentDescription = "Pemantauan Baterai")
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
                                StatusBarSettingsSummaryFormatter.format(
                                    statusBarSetting.displayMode, statusBarSetting.parameter,
                                    statusBarSetting.rotationParameters, statusBarSetting.rotationIntervalSeconds, currentScale
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        CompactSwitch(checked = statusBarSetting.enabled, onCheckedChange = vm::setStatusBarIndicatorEnabled,
                            contentDescription = "Indikator status bar")
                    }
                    OutlinedButton(
                        onClick = { showStatusBarSettingsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
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
            ThemePickerDialogs(
                config = themeConfig,
                batteryLevel = previewBatteryState.level.takeIf { previewBatteryState.isLevelAvailable },
                isCharging = previewBatteryState.isChargingStatusAvailable && previewBatteryState.isCharging &&
                    !(previewBatteryState.isTemperatureAvailable && previewBatteryState.tempCelsius >= ChargingThermalPhaseAnalyzer.WARM_TEMPERATURE_CELSIUS),
                temperatureCelsius = previewBatteryState.tempCelsius,
                isTemperatureAvailable = previewBatteryState.isTemperatureAvailable,
                onSave = vm::setThemePackConfig
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kapasitas baterai", fontWeight = FontWeight.Bold)
                    Text(
                        "Kapasitas desain: ${capacity.capacityMah?.let { "$it mAh" } ?: "Tidak tersedia"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Sumber: ${if (capacity.isManualOverride) "Manual" else "Otomatis"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { showCapacityDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Edit, null)
                        Text("  Ubah Kapasitas Desain")
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
                        Text("  Buka Pengaturan Awal")
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = ::openChargeGuardDialog),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Peringatan & Nada", fontWeight = FontWeight.Bold)
                        Text(
                            ChargeGuardSettingsSummaryFormatter.formatRules(chargeGuardConfig),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            ChargeGuardSettingsSummaryFormatter.formatAlert(
                                chargeGuardAlertSound.title, chargeGuardAlertSound.vibrationPattern.title
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Atur peringatan dan nada",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    if (showInfo) ReadMoreInfoDialog { showInfo = false }
    if (showChargeGuardDialog) {
        val hasUnsavedChanges = draftChargeGuardConfig != chargeGuardConfig ||
            draftChargeGuardSound != chargeGuardAlertSound
        Dialog(onDismissRequest = { showChargeGuardDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).imePadding(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                    Text(
                        "Peringatan & Nada",
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        Modifier.fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Batas pengisian", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            CompactSwitch(
                                checked = draftChargeGuardConfig.chargeLimitEnabled,
                                onCheckedChange = { draftChargeGuardConfig = draftChargeGuardConfig.copy(chargeLimitEnabled = it) },
                                contentDescription = "Batas pengisian"
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(80, 85, 90, 95, 100).forEach { target ->
                                ChargeGuardChoicePill(
                                    "$target%",
                                    draftChargeGuardConfig.targetLevelPercent == target,
                                    { draftChargeGuardConfig = draftChargeGuardConfig.copy(targetLevelPercent = target) },
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Peringatan suhu tinggi", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            CompactSwitch(
                                checked = draftChargeGuardConfig.highTemperatureEnabled,
                                onCheckedChange = { draftChargeGuardConfig = draftChargeGuardConfig.copy(highTemperatureEnabled = it) },
                                contentDescription = "Peringatan suhu tinggi"
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(40, 42, 45, 50).forEach { threshold ->
                                ChargeGuardChoicePill(
                                    "$threshold°C",
                                    draftChargeGuardConfig.highTemperatureThresholdCelsius == threshold,
                                    { draftChargeGuardConfig = draftChargeGuardConfig.copy(highTemperatureThresholdCelsius = threshold) },
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Pengulangan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChargeGuardRepeatMode.values().forEach { mode ->
                                ChargeGuardChoicePill(
                                    mode.title,
                                    draftChargeGuardConfig.repeatMode == mode,
                                    { draftChargeGuardConfig = draftChargeGuardConfig.copy(repeatMode = mode) },
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Nada & Getar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable {
                                    val existingUri = when (draftChargeGuardSound.selection) {
                                        ChargeGuardAlertSoundSetting.DEFAULT -> Settings.System.DEFAULT_NOTIFICATION_URI
                                        ChargeGuardAlertSoundSetting.SILENT -> null
                                        else -> runCatching { Uri.parse(draftChargeGuardSound.selection) }.getOrNull()
                                    }
                                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                    }
                                    ringtonePickerLauncher.launch(pickerIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${draftChargeGuardSound.title} · Ubah ›",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { vm.previewChargeGuardAlert(draftChargeGuardSound) },
                                modifier = Modifier.height(36.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) { Text("Tes") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChargeGuardVibrationPattern.values().forEach { pattern ->
                                ChargeGuardChoicePill(
                                    pattern.title,
                                    draftChargeGuardSound.vibrationPattern == pattern,
                                    {
                                        draftChargeGuardSound = draftChargeGuardSound.copy(vibrationPattern = pattern)
                                        lastCustomTapAt = 0L
                                    },
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        if (draftChargeGuardSound.vibrationPattern == ChargeGuardVibrationPattern.CUSTOM) {
                            val pulseCount = (draftChargeGuardSound.customVibrationTimings.size + 1) / 2
                            val durationMs = draftChargeGuardSound.customVibrationTimings.sum()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f).height(44.dp).clickable {
                                        val now = SystemClock.elapsedRealtime()
                                        val existing = draftChargeGuardSound.customVibrationTimings
                                        if (pulseCount < 6) {
                                            val next = if (existing.isEmpty()) {
                                                listOf(120L)
                                            } else {
                                                val gap = if (lastCustomTapAt == 0L) 50L
                                                    else (now - lastCustomTapAt - 120L).coerceIn(50L, 1_500L)
                                                existing + gap + 120L
                                            }
                                            draftChargeGuardSound = draftChargeGuardSound.copy(
                                                vibrationPattern = ChargeGuardVibrationPattern.CUSTOM,
                                                customVibrationTimings = next
                                            )
                                        }
                                        lastCustomTapAt = now
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "Ketuk Irama  ·  $pulseCount/6",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    draftChargeGuardSound = draftChargeGuardSound.copy(customVibrationTimings = emptyList())
                                    lastCustomTapAt = 0L
                                }) { Text("Reset") }
                            }
                            Text(
                                if (pulseCount < 2) "Ketuk minimal 2 kali untuk merekam pola."
                                else "Durasi pola: $durationMs ms · $pulseCount pulsa",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Peringatan bekerja saat perangkat mengisi dan data sensor tersedia. Fitur ini memberi notifikasi, bukan memutus aliran charger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showChargeGuardDialog = false },
                            modifier = Modifier.height(36.dp).weight(1f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("Batal") }
                        Button(
                            onClick = {
                                vm.setChargeGuardConfiguration(draftChargeGuardConfig)
                                vm.setChargeGuardAlertSound(draftChargeGuardSound)
                                showChargeGuardDialog = false
                            },
                            enabled = hasUnsavedChanges,
                            modifier = Modifier.height(36.dp).weight(1f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Text("Simpan") }
                    }
                }
            }
        }
    }
    if (showStatusBarSettingsDialog) {
        var draftMode by remember { mutableStateOf(statusBarSetting.displayMode) }
        var draftParameter by remember { mutableStateOf(statusBarSetting.parameter) }
        var draftRotationParameters by remember { mutableStateOf(statusBarSetting.rotationParameters.toSet()) }
        var draftIntervalSeconds by remember { mutableIntStateOf(statusBarSetting.rotationIntervalSeconds) }
        var draftIntervalInput by remember { mutableStateOf(statusBarSetting.rotationIntervalSeconds.toString()) }
        var draftCurrentScale by remember { mutableStateOf(currentScale) }
        var customIntervalEdited by remember {
            mutableStateOf(statusBarSetting.rotationIntervalSeconds !in listOf(2, 3, 5, 10))
        }

        val validCustomInterval = draftIntervalInput.toIntOrNull()?.let { it in 1..60 } == true
        val canSave = draftMode != StatusBarDisplayMode.CUSTOM_ROTATION ||
            (draftRotationParameters.size >= 2 && validCustomInterval)

        Dialog(onDismissRequest = { showStatusBarSettingsDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).imePadding(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
                ) {
                    val previewBaseState = selectDashboardLiveState(
                        previewRunning, previewService, previewFallback, previewServiceAt,
                        previewFallbackAt, SystemClock.elapsedRealtime()
                    )
                    var previewElapsedRealtime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
                    LaunchedEffect(draftMode, draftRotationParameters, draftIntervalSeconds) {
                        if (draftMode == StatusBarDisplayMode.CUSTOM_ROTATION) {
                            while (true) {
                                previewElapsedRealtime = SystemClock.elapsedRealtime()
                                delay(250L)
                            }
                        } else {
                            previewElapsedRealtime = SystemClock.elapsedRealtime()
                        }
                    }
                    val previewParameters = StatusBarParameter.selectableValues.filter { it in draftRotationParameters }
                    val previewSetting = remember(draftMode, draftParameter, previewParameters, draftIntervalSeconds) {
                        StatusBarIndicatorSetting(
                            enabled = true,
                            parameter = draftParameter,
                            displayMode = draftMode,
                            rotationParameters = previewParameters,
                            rotationIntervalSeconds = draftIntervalSeconds
                        )
                    }
                    val previewParameter = previewSetting.parameterAtElapsedRealtime(previewElapsedRealtime)
                    val scaleRatio = (draftCurrentScale.multiplier / currentScale.multiplier)
                        .takeIf { it.isFinite() && it > 0.0 } ?: 1.0
                    val previewReadingState = previewBaseState.copy(
                        currentMa = (previewBaseState.currentMa * scaleRatio).toInt(),
                        powerWatt = (previewBaseState.powerWatt * scaleRatio).toFloat()
                    )
                    val previewDensity = LocalDensity.current.density
                    val previewBitmap = remember(previewReadingState, previewParameter, previewDensity,
                        draftMode, draftRotationParameters, draftIntervalSeconds, draftCurrentScale) {
                        val reading = LiveStatusBarRenderer.format(previewReadingState, previewParameter)
                        LiveStatusBarRenderer.render(previewParameter, reading, previewDensity)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Indikator status bar",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier.size(32.dp).background(InstrumentGraphite, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            previewBitmap?.let { bitmap ->
                                Image(bitmap.asImageBitmap(), contentDescription = "Pratinjau ikon status bar",
                                    modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
                            } ?: Text("—", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Mode tampilan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                StatusBarDisplayMode.SINGLE_METRIC to "Satu metrik",
                                StatusBarDisplayMode.CUSTOM_ROTATION to "Rotasi kustom"
                            ).forEach { (mode, label) ->
                                val selected = draftMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { draftMode = mode }
                                        ),
                                    shape = RoundedCornerShape(8.dp),
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
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            ),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
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

                        Text(
                            "Kalibrasi skala arus",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BatteryCurrentScale.values().forEach { scale ->
                                val selected = draftCurrentScale == scale
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { draftCurrentScale = scale }
                                        ),
                                    shape = RoundedCornerShape(8.dp),
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
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            scale.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "Berlaku global untuk pembacaan CURRENT_NOW dan daya hasil V × I. Default: 1x.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (draftMode == StatusBarDisplayMode.CUSTOM_ROTATION) {
                            Text(
                                "Interval rotasi (detik)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(2, 3, 5, 10).forEach { seconds ->
                                    val selected = draftIntervalSeconds == seconds && !customIntervalEdited
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .selectable(
                                                selected = selected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    draftIntervalSeconds = seconds
                                                    draftIntervalInput = seconds.toString()
                                                    customIntervalEdited = false
                                                }
                                            ),
                                        shape = RoundedCornerShape(8.dp),
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
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$seconds dtk",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                ),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                                val customBorderColor = when {
                                    !validCustomInterval -> MaterialTheme.colorScheme.error
                                    customIntervalEdited -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                Surface(
                                    modifier = Modifier.width(52.dp).height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (customIntervalEdited && validCustomInterval) 1.5.dp else 1.dp,
                                        customBorderColor
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = draftIntervalInput,
                                            onValueChange = { input ->
                                                draftIntervalInput = input.filter(Char::isDigit).take(2)
                                                customIntervalEdited = true
                                                draftIntervalInput.toIntOrNull()
                                                    ?.takeIf { it in 1..60 }
                                                    ?.let { draftIntervalSeconds = it }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            )
                                        )
                                    }
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
                                vm.saveStatusBarIndicatorSettings(
                                    parameter = draftParameter,
                                    displayMode = draftMode,
                                    rotationParameters = draftRotationParameters.toList(),
                                    rotationIntervalSeconds = draftIntervalSeconds,
                                    currentScale = draftCurrentScale
                                )
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
                        "Masukkan kapasitas desain resmi baterai (1000–20000 mAh). Nilai manual digunakan sampai Anda kembali ke Otomatis.",
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
                    }) { Text("Gunakan Otomatis") }
                    TextButton(onClick = { showCapacityDialog = false }) { Text("Batal") }
                }
            }
        )
    }
}


@Composable
private fun ChargeGuardChoicePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(32.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false)),
                fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { parameter ->
                    val selected = parameter in selectedParameters
                    val enabled = !multiSelect || !selected || canDeselectSelected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .selectable(
                                selected = selected,
                                enabled = enabled,
                                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                                onClick = { onParameterClick(parameter) }
                            ),
                        shape = RoundedCornerShape(8.dp),
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
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
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
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

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
