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
import com.superdenot.powerguard.domain.model.StatusBarParameter
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

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        if (state.isCharging) "Sumber: ${state.chargeSource}  •  Protokol: ${state.chargingProtocol ?: "Tidak diketahui"}" else "Perangkat sedang menggunakan daya baterai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniMetric("Arus baterai", if (state.isCurrentAvailable) "${state.currentMa} mA" else "—", Modifier.weight(1f))
                        MiniMetric("Pemantauan", if (running) "Aktif" else "Siaga", Modifier.weight(1f))
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
            ExpandPanel("health", "Kesehatan baterai", "Estimasi kapasitas dan acuan desain", emptySet(), {}) {
                Text(health?.let { "Estimasi kapasitas: ${it.estimatedCapacityMah} mAh" } ?: "Belum cukup data untuk estimasi kesehatan.")
                health?.let { Text("Estimasi kesehatan: ${String.format(Locale.US, "%.1f", it.healthPercentage)}%") }
                Text("Kapasitas desain: ${capacity.capacityMah?.let { "$it mAh" } ?: "Belum tersedia"}")
                Text("Sumber kapasitas: ${if (capacity.isManualOverride) "Manual" else "Otomatis"}")
            }
        }
        item {
            ExpandPanel("system", "Kesiapan sistem", "Izin dan pemantauan latar belakang", emptySet(), {}) {
                Text("Pemantauan otomatis: ${if (auto) "Aktif" else "Nonaktif"}")
                Text("Optimasi baterai: ${if (batteryOpt) "Dikecualikan" else "Belum dikecualikan"}")
                Text("Akses penggunaan: ${if (usage) "Diizinkan" else "Belum diizinkan"}")
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
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

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DIAGNOSTIK PENGISIAN", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text("Analisis pengisian baterai", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Pantau sumber daya, pembacaan baterai, dan perkembangan sesi.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("LEVEL BATERAI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text(if (state.isLevelAvailable) "${state.level}%" else "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(if (!state.isChargingStatusAvailable) "STATUS TIDAK TERSEDIA" else if (state.isCharging) "MENGISI" else "TIDAK MENGISI", if (!state.isChargingStatusAvailable) MaterialTheme.colorScheme.onSurfaceVariant else if (state.isCharging) EmeraldCharge else AmberThermal)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DiagnosticLine("Sumber daya", if (state.isCharging) state.chargeSource else "Tidak terhubung")
                    DiagnosticLine("Protokol", state.chargingProtocol ?: "Tidak diketahui — tidak diekspos perangkat")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("PEMBACAAN BATERAI", "Nilai pada sisi baterai, bukan keluaran adaptor")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Arus", if (state.isCurrentAvailable) "${state.currentMa} mA" else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Daya", if (state.isPowerAvailable) String.format(Locale.US, "%.2f W", state.powerWatt) else "Tidak tersedia", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric("Tegangan", if (state.isVoltageAvailable) "${state.voltageMv} mV" else "Tidak tersedia", Modifier.weight(1f))
                    MiniMetric("Suhu", if (state.isTemperatureAvailable) String.format(Locale.US, "%.1f°C", state.tempCelsius) else "Tidak tersedia", Modifier.weight(1f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading("RINGKASAN SESI", "Perubahan yang tercatat sejak pengisian dimulai")
                    DiagnosticLine("Muatan bersih tercatat", String.format(Locale.US, "%+.2f mAh", state.sessionMahDelivered))
                    DiagnosticLine("Durasi sesi", "${state.sessionDurationSec / 60} menit")
                    DiagnosticLine("CPU deep sleep", "${(state.deepSleepRatio * 100).toInt()}%")
                }
            }
        }
        item {
            OutlinedButton(onClick = vm::startManualMonitorService, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(if (running) "Layanan pemantauan aktif" else "Mulai pemantauan manual")
            }
        }
    }
}

@Composable
fun InsightsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("INSIGHTS", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("Pola penggunaan daya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        AnalyticsScreen(viewModel = vm, contentPadding = PaddingValues(0.dp), initialSubTab = 1, visibleSubTabs = listOf(1, 2))
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

@Composable
fun SettingsScreen(vm: PowerGuardViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val auto by vm.autoMonitoringEnabled.collectAsState()
    val error by vm.autoMonitoringError.collectAsState()
    val batteryOpt by vm.isBatteryOptimizationIgnored.collectAsState()
    val usage by vm.hasUsagePermission.collectAsState()
    val capacity by vm.designCapacitySetting.collectAsState()
    val statusBarSetting by vm.statusBarIndicatorSetting.collectAsState()
    var statusBarParameterMenuExpanded by remember { mutableStateOf(false) }
    var showCapacityDialog by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var permissionStatusRefreshed by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("PENGATURAN", color = CyanFastCharge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Preferensi aplikasi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pemantauan otomatis", fontWeight = FontWeight.Bold)
                            Text("Mulai memantau saat pengisi daya tersambung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = auto, onCheckedChange = vm::setAutoMonitoringEnabled)
                    }
                    error?.let { Text(it, color = CrimsonAnomaly, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Indikator status bar", fontWeight = FontWeight.Bold)
                            Text(
                                "Tampilkan satu parameter baterai pada ikon di status bar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = statusBarSetting.enabled,
                            onCheckedChange = vm::setStatusBarIndicatorEnabled
                        )
                    }
                    Text("Parameter yang ditampilkan", style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(
                            enabled = statusBarSetting.enabled,
                            onClick = { statusBarParameterMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(statusBarSetting.parameter.label)
                        }
                        DropdownMenu(
                            expanded = statusBarParameterMenuExpanded,
                            onDismissRequest = { statusBarParameterMenuExpanded = false }
                        ) {
                            StatusBarParameter.values().forEach { parameter ->
                                DropdownMenuItem(
                                    text = { Text(parameter.label) },
                                    onClick = {
                                        vm.setStatusBarParameter(parameter)
                                        statusBarParameterMenuExpanded = false
                                    }
                                )
                            }
                        }
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Akses Android", fontWeight = FontWeight.Bold)
                    Text("Battery optimization: ${if (batteryOpt) "Dikecualikan" else "Belum dikecualikan"}")
                    if (!batteryOpt) OutlinedButton(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }
                        runCatching { context.startActivity(intent) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Pengaturan optimasi baterai") }
                    Text("Usage Access: ${if (usage) "Diizinkan" else "Belum diizinkan"}")
                    if (!usage) OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } }, modifier = Modifier.fillMaxWidth()) { Text("Pengaturan akses penggunaan") }
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
