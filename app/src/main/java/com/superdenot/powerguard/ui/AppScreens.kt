package com.superdenot.powerguard.ui

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superdenot.powerguard.ui.analytics.AnalyticsScreen
import com.superdenot.powerguard.ui.dashboard.ReadMoreInfoDialog
import com.superdenot.powerguard.ui.dashboard.selectDashboardLiveState
import com.superdenot.powerguard.ui.theme.*
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import java.util.Locale

@Composable
fun SuperdDashboardScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val service by vm.liveState.collectAsState()
    val fallback by vm.fallbackState.collectAsState()
    val running by vm.isServiceRunning.collectAsState()
    val serviceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
    val fallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val state = selectDashboardLiveState(running, service, fallback, serviceAt, fallbackAt, SystemClock.elapsedRealtime())
    val health by vm.latestHealth.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val auto by vm.autoMonitoringEnabled.collectAsState()
    val batteryOpt by vm.isBatteryOptimizationIgnored.collectAsState()
    val usage by vm.hasUsagePermission.collectAsState()
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var comingSoon by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(top = 14.dp)) {
                Text("CONTROL CENTER", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Ringkasan kondisi daya perangkat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryChargingFull, null, tint = if (state.isCharging) EmeraldCharge else CyanFastCharge, modifier = Modifier.size(27.dp))
                            Text("  ${if (state.isLevelAvailable) "${state.level}%" else "N/A"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        }
                        StatusPill(if (!state.isChargingStatusAvailable) "STATUS N/A" else if (state.isCharging) "CHARGING" else "DISCHARGING", if (!state.isChargingStatusAvailable) MaterialTheme.colorScheme.onSurfaceVariant else if (state.isCharging) EmeraldCharge else AmberThermal)
                    }
                    Text(if (state.isFastCharging) "⚡ FAST CHARGING • ${state.chargeSource}" else state.chargeSource.uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric("Monitoring", if (running) "ACTIVE" else "STANDBY", Modifier.weight(1f))
                        MiniMetric("Auto Start", if (auto) "ON" else "OFF", Modifier.weight(1f))
                    }
                    state.anomalyAlert?.let { Text("PERHATIAN: $it", color = CrimsonAnomaly, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        item {
            ExpandPanel("health", "Battery Health", "Estimasi kapasitas dan acuan desain", expanded, { expanded = expanded.toggle("health") }) {
                Text(health?.let { "Estimasi kapasitas: ${it.estimatedCapacityMah} mAh" } ?: "Estimasi kesehatan belum tersedia.")
                health?.let { Text("Kesehatan: ${String.format(Locale.US, "%.1f", it.healthPercentage)}%") }
                Text("Kapasitas desain: ${capacity.capacityMah?.let { "$it mAh" } ?: "Tidak tersedia"}")
                Text("Sumber: ${if (capacity.isManualOverride) "Manual" else "Automatic"}")
            }
        }
        item {
            ExpandPanel("system", "System Readiness", "Status pemantauan dan izin sistem", expanded, { expanded = expanded.toggle("system") }) {
                Text("Auto Monitoring: ${if (auto) "Aktif" else "Nonaktif"}")
                Text("Optimasi baterai: ${if (batteryOpt) "Dikecualikan" else "Belum dikecualikan"}")
                Text("Usage Access: ${if (usage) "Diizinkan" else "Belum diizinkan"}")
            }
        }
        item {
            ExpandPanel("future", "Fitur Mendatang", "Modul yang sedang disiapkan", expanded, { expanded = expanded.toggle("future") }) {
                ComingSoonButton("Smart Charging Protection") { comingSoon = true }
                ComingSoonButton("Advanced Battery Diagnostics") { comingSoon = true }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (comingSoon) ComingSoonDialog { comingSoon = false }
}

@Composable
fun ChargingScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val service by vm.liveState.collectAsState()
    val fallback by vm.fallbackState.collectAsState()
    val running by vm.isServiceRunning.collectAsState()
    val serviceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
    val fallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val state = selectDashboardLiveState(running, service, fallback, serviceAt, fallbackAt, SystemClock.elapsedRealtime())
    var comingSoon by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("CHARGING", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Live charging analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (state.isLevelAvailable) "${state.level}%" else "N/A", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = EmeraldCharge)
                    StatusPill(if (!state.isChargingStatusAvailable) "STATUS N/A" else if (state.isCharging) "CHARGING • ${state.chargeSource}" else "NOT CHARGING", if (!state.isChargingStatusAvailable) MaterialTheme.colorScheme.onSurfaceVariant else if (state.isCharging) EmeraldCharge else AmberThermal)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric("Current", if (state.isCurrentAvailable) "${state.currentMa} mA" else "N/A", Modifier.weight(1f))
                        MiniMetric("Power", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "N/A", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniMetric("Voltage", if (state.isVoltageAvailable) "${state.voltageMv} mV" else "N/A", Modifier.weight(1f))
                        MiniMetric("Temperature", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f°C", state.tempCelsius) else "N/A", Modifier.weight(1f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                    Text("Session delivered: ${String.format(Locale.US, "%+.2f mAh", state.sessionMahDelivered)}")
                    Text("Session duration: ${state.sessionDurationSec / 60} min")
                    Text("CPU Deep Sleep: ${(state.deepSleepRatio * 100).toInt()}%")
                }
            }
        }
        item {
            Button(onClick = vm::startManualMonitorService, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Monitor Service Active" else "Start Monitor Service")
            }
        }
        item { ComingSoonButton("Smart Charging Limit") { comingSoon = true } }
        item { ComingSoonButton("Charging Schedule") { comingSoon = true } }
    }
    if (comingSoon) ComingSoonDialog { comingSoon = false }
}

@Composable
fun InsightsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("INSIGHTS", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Tren siklus dan aktivitas perangkat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        AnalyticsScreen(
            viewModel = vm,
            contentPadding = PaddingValues(0.dp),
            initialSubTab = 1,
            visibleSubTabs = listOf(1, 2)
        )
    }
}

@Composable
fun HistoryScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("HISTORY", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Riwayat sesi dan kurva pengisian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        AnalyticsScreen(
            viewModel = vm,
            contentPadding = PaddingValues(0.dp),
            initialSubTab = 0,
            visibleSubTabs = listOf(0)
        )
    }
}

@Composable
fun SettingsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val auto by vm.autoMonitoringEnabled.collectAsState()
    val error by vm.autoMonitoringError.collectAsState()
    val batteryOpt by vm.isBatteryOptimizationIgnored.collectAsState()
    val usage by vm.hasUsagePermission.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    var showCapacityDialog by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var permissionStatusRefreshed by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("SETTINGS", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Preferences & system access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto Monitoring", fontWeight = FontWeight.Bold)
                            Text("Mulai pemantauan otomatis saat charger tersambung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = auto, onCheckedChange = vm::setAutoMonitoringEnabled)
                    }
                    error?.let { Text(it, color = CrimsonAnomaly, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Battery Capacity", fontWeight = FontWeight.Bold)
                    Text(
                        "Design capacity: ${capacity.capacityMah?.let { "$it mAh" } ?: "Not available"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Source: ${if (capacity.isManualOverride) "Manual override" else "Automatic"}",
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Android Permissions", fontWeight = FontWeight.Bold)
                    Text("Battery optimization: ${if (batteryOpt) "Exempted" else "Not exempted"}")
                    if (!batteryOpt) OutlinedButton(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }
                        runCatching { context.startActivity(intent) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Battery Optimization Settings") }
                    Text("Usage Access: ${if (usage) "Granted" else "Not granted"}")
                    if (!usage) OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } }, modifier = Modifier.fillMaxWidth()) { Text("Usage Access Settings") }
                    OutlinedButton(
                        onClick = {
                            vm.refreshPermissionsAndUsage()
                            permissionStatusRefreshed = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Refresh, null)
                        Text("  Perbarui Status Izin")
                    }
                    if (permissionStatusRefreshed) {
                        Text(
                            "Status izin sudah diperiksa ulang.",
                            color = EmeraldCharge,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personalization", fontWeight = FontWeight.Bold)
                    ComingSoonButton("Alert Sound Studio") { comingSoon = true }
                    ComingSoonButton("Custom Alert Profiles") { comingSoon = true }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showInfo = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Info, null)
                Text("  About SUPERD POWER GUARD")
            }
        }
    }
    if (comingSoon) ComingSoonDialog { comingSoon = false }
    if (showInfo) ReadMoreInfoDialog { showInfo = false }
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
    Surface(modifier, color = Color.Black.copy(alpha = .16f), shape = RoundedCornerShape(12.dp)) {
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
                Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
