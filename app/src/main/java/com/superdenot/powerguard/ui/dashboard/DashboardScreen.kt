package com.superdenot.powerguard.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.superdenot.powerguard.ui.components.PowerActionButton
import com.superdenot.powerguard.ui.components.PowerSaveButton
import com.superdenot.powerguard.ui.components.POWER_BUTTON_CLOSE_DELAY_MS
import com.superdenot.powerguard.ui.components.powerButtonClickEffect
import com.superdenot.powerguard.ui.dashboard.hasDesignCapacityDraftChanges
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import android.os.SystemClock
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.superdenot.powerguard.BuildConfig
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CrimsonAnomaly
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: PowerGuardViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val serviceState by viewModel.liveState.collectAsState()
    val fallbackState by viewModel.fallbackState.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val serviceUpdatedAt by viewModel.lastSensorUpdateElapsedRealtime.collectAsState()
    val fallbackUpdatedAt by viewModel.fallbackUpdatedAtElapsedRealtime.collectAsState()
    val activeState = selectDashboardLiveState(
        isServiceRunning = isServiceRunning,
        serviceState = serviceState,
        fallbackState = fallbackState,
        serviceUpdatedAtElapsedRealtime = serviceUpdatedAt,
        fallbackUpdatedAtElapsedRealtime = fallbackUpdatedAt,
        nowElapsedRealtime = SystemClock.elapsedRealtime()
    )

    val latestHealth by viewModel.latestHealth.collectAsState()
    val designCapacity by viewModel.designCapacitySetting.collectAsState()
    val batteryMonitoringEnabled by viewModel.batteryMonitoringEnabled.collectAsState()
    val autoMonitoringError by viewModel.autoMonitoringError.collectAsState()
    val isBatteryOptIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val hasUsagePerm by viewModel.hasUsagePermission.collectAsState()

    var showCapacityDialog by remember { mutableStateOf(false) }
    var showReadMoreDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            activeState.anomalyAlert?.let { alertMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonAnomaly.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = CrimsonAnomaly,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ANOMALI PENGISIAN TERDETEKSI",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonAnomaly
                                )
                                Text(
                                    text = alertMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                val currentColor = when {
                    activeState.isCharging && activeState.currentMa > 300 -> EmeraldCharge
                    activeState.isCharging && activeState.currentMa <= 300 -> CrimsonAnomaly
                    else -> CyanFastCharge
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = currentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${activeState.level}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Surface(
                                modifier = Modifier.widthIn(max = 190.dp),
                                color = if (activeState.isCharging) EmeraldCharge.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (activeState.isCharging) activeState.chargingProtocol else "TIDAK MENGISI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    maxLines = 2,
                                    softWrap = true,
                                    color = if (activeState.isCharging) EmeraldCharge else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val sign = if (activeState.currentMa > 0) "+" else ""
                        Text(
                            text = "$sign${activeState.currentMa} mA",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = currentColor
                        )
                        Text(
                            text = if (activeState.isCharging) "Arus masuk bersih ke sel baterai"
                            else "Arus keluar (beban sistem saat ini)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Daya",
                        value = String.format(Locale.US, "%.2f W", activeState.powerWatt),
                        icon = Icons.Rounded.Bolt,
                        accent = CyanFastCharge,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Tegangan",
                        value = "${activeState.voltageMv} mV",
                        icon = Icons.Rounded.Bolt,
                        accent = EmeraldCharge,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Suhu",
                        value = "${String.format(Locale.US, "%.1f", activeState.tempCelsius)}°C",
                        icon = Icons.Rounded.Thermostat,
                        accent = if (activeState.tempCelsius >= 40f) CrimsonAnomaly else AmberThermal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sesi Pengisian & Efisiensi Tidur CPU",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Daya Terisi (Sesi Ini)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.2f mAh", activeState.sessionMahDelivered),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeState.sessionMahDelivered >= 0) EmeraldCharge else CrimsonAnomaly
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Durasi Sesi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val mins = activeState.sessionDurationSec / 60
                                val secs = activeState.sessionDurationSec % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val deepSleepPct = (activeState.deepSleepRatio * 100).toInt()
                        Text(
                            text = "Rasio mode tidur CPU: $deepSleepPct% (Tinggi = minim wakelock latar belakang)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kesehatan & Kapasitas Baterai",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (latestHealth != null) {
                                Text(
                                    text = "Estimasi Real: ${latestHealth!!.estimatedCapacityMah} mAh (${String.format(Locale.US, "%.1f", latestHealth!!.healthPercentage)}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldCharge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Acuan desain saat dicatat: ${latestHealth!!.designCapacityMah} mAh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Estimasi Real: Belum dikalibrasi (cas >= 40% dalam 1 sesi)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Kapasitas Desain: ${designCapacity.capacityMah?.let { "$it mAh" } ?: "Tidak tersedia"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (designCapacity.isManualOverride) "Sumber: Manual Override" else if (designCapacity.capacityMah != null) "Sumber: Otomatis" else "Sumber: Otomatis (tidak tersedia)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val capacityEditInteractionSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { showCapacityDialog = true },
                            interactionSource = capacityEditInteractionSource,
                            modifier = Modifier.powerButtonClickEffect(capacityEditInteractionSource)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Ubah Kapasitas Desain",
                                tint = CyanFastCharge
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Pemantauan Baterai", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Kontrol utama tersedia di Setelan. Layanan hanya berjalan saat Battery Monitoring ON dan charger tersambung.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                isServiceRunning -> "Status: AKTIF — layanan sedang berjalan"
                                !batteryMonitoringEnabled -> "Status: OFF"
                                !activeState.isCharging -> "Status: SIAGA — menunggu charger"
                                !isBatteryOptIgnored -> "Status: OFF — izin latar belakang diperlukan"
                                else -> "Status: OFF — menunggu aktivasi sistem"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isServiceRunning) EmeraldCharge else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        autoMonitoringError?.let { error ->
                            Text(error, style = MaterialTheme.typography.bodySmall, color = CrimsonAnomaly)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = EmeraldCharge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kesiapan Sistem Latar Belakang",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isBatteryOptIgnored) {
                            val batteryOptimizationInteractionSource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                interactionSource = batteryOptimizationInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .powerButtonClickEffect(batteryOptimizationInteractionSource)
                            ) {
                                Text("Izinkan Abaikan Optimasi Baterai untuk Auto-start")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldCharge, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pengecualian optimasi baterai aktif", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (!hasUsagePerm) {
                            val usageAccessInteractionSource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                interactionSource = usageAccessInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .powerButtonClickEffect(usageAccessInteractionSource)
                            ) {
                                Text("Aktifkan Akses Penggunaan Aplikasi")
                            }
                        }

                    }
                }
            }

            // --- READ MORE SECTION (Settings / App Info) ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Superd Power Guard — Info",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tentang aplikasi, lisensi, privasi, & catatan pengukuran",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val readMoreInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showReadMoreDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanFastCharge),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp).powerButtonClickEffect(readMoreInteractionSource),
                            interactionSource = readMoreInteractionSource
                        ) {
                            Text("READ MORE", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showCapacityDialog) {
        DesignCapacityDialog(
            capacityMah = designCapacity.capacityMah,
            isManualOverride = designCapacity.isManualOverride,
            onDismiss = { showCapacityDialog = false },
            onSaveManual = {
                viewModel.updateDesignCapacity(it)
                showCapacityDialog = false
            },
            onUseAutomatic = {
                viewModel.useAutomaticDesignCapacity()
                showCapacityDialog = false
            }
        )
    }

    if (showReadMoreDialog) {
        ReadMoreInfoDialog(onDismiss = { showReadMoreDialog = false })
    }
}

@Composable
fun DesignCapacityDialog(
    capacityMah: Int?,
    isManualOverride: Boolean,
    onDismiss: () -> Unit,
    onSaveManual: (Int) -> Unit,
    onUseAutomatic: () -> Unit
) {
    var inputText by remember(capacityMah, isManualOverride) { mutableStateOf(capacityMah?.toString().orEmpty()) }
    val parsedCapacity = inputText.toIntOrNull()
    val validCapacity = parsedCapacity?.let { it in 1000..20000 } == true
    val hasChanges = hasDesignCapacityDraftChanges(parsedCapacity, capacityMah, savedIsManualMode = isManualOverride)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).imePadding(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                Text(
                    "Kapasitas Desain Baterai",
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Masukkan kapasitas desain resmi baterai (1000–20000 mAh). Nilai manual digunakan sampai Anda kembali ke Otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kapasitas (mAh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputText.isNotEmpty() && !validCapacity
                    )
                    PowerActionButton(
                        onClick = onUseAutomatic,
                        enabled = canUseAutomaticDesignCapacity(isManualOverride),
                        modifier = Modifier.wrapContentWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("Gunakan Otomatis") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PowerActionButton(onClick = onDismiss, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Batal") }
                    PowerSaveButton(
                        clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS,
                        onClick = { parsedCapacity?.let(onSaveManual) },
                        enabled = hasChanges
                    ) { Text("Simpan") }
                }
            }
        }
    }
}

@Composable
fun ReadMoreInfoDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp).heightIn(max = 680.dp).imePadding(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUPERD POWER GUARD — Info", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        .verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                InfoSectionItem(
                    title = "1. Tentang Aplikasi",
                    content = "SUPERD POWER GUARD adalah aplikasi diagnostik dan pemantauan baterai Android. Aplikasi menampilkan data baterai yang tersedia dari sistem, menganalisis pola pengisian, dan menyimpan riwayat sesi secara lokal.\n\nDeveloper: superdenot\nApplication ID: com.superdenot.superdguard\nVersi: ${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE})"
                )
                InfoSectionItem(
                    title = "2. Fitur Utama",
                    content = "• Pemantauan level, arus, tegangan, daya, dan suhu baterai.\n• Deteksi sumber daya yang dilaporkan Android (AC, USB, atau wireless).\n• Analisis indikasi anomali pengisian berdasarkan heuristik.\n• Pemantauan otomatis saat charger tersambung dan finalisasi sesi saat dilepas.\n• Riwayat sesi dan ringkasan data baterai yang tersimpan di perangkat."
                )
                InfoSectionItem(
                    title = "3. Lisensi Aplikasi & Hak Cipta",
                    content = "© 2026 superdenot. Hak cipta atas aplikasi dan kode SUPERD POWER GUARD tetap berada pada pemegang hak cipta. Repository saat ini tidak menyertakan berkas LICENSE yang memberikan lisensi open-source untuk kode aplikasi. Artinya, kode aplikasi tidak otomatis boleh disalin, dimodifikasi, atau didistribusikan ulang. Lisensi komponen pihak ketiga dijelaskan pada bagian berikutnya dan tidak mengubah status lisensi kode aplikasi."
                )
                InfoSectionItem(
                    title = "4. Pemberitahuan Sumber Terbuka & Pihak Ketiga",
                    content = "Dependency yang dideklarasikan langsung pada konfigurasi build mencakup AndroidX Core, Activity, Lifecycle, Jetpack Compose, Material 3, Room, WorkManager, DataStore, Kotlin Coroutines, JUnit 4, Robolectric, dan AndroidX Test.\n\nLisensi komponen tersebut mengikuti proyek masing-masing; antara lain Apache License 2.0 untuk banyak komponen AndroidX, Kotlin, Coroutines, Room, WorkManager, DataStore, dan Robolectric; serta Eclipse Public License 1.0 untuk JUnit 4. Daftar ini adalah ringkasan dependency langsung, bukan inventaris lengkap seluruh dependency transitif. Setiap komponen tetap dimiliki oleh pengembang/pemegang hak cipta masing-masing. Distribusi aplikasi wajib tetap menyertakan pemberitahuan dan teks lisensi yang diwajibkan oleh dependency yang benar-benar ikut dalam paket."
                )
                InfoSectionItem(
                    title = "5. Privasi & Data",
                    content = "Data pemantauan baterai, sampel, dan riwayat sesi dikelola secara lokal menggunakan Room Database. Preferensi aplikasi disimpan menggunakan DataStore. Fitur statistik penggunaan aplikasi memerlukan akses Usage Access dari pengaturan Android. Aplikasi tidak menyediakan akun atau fitur sinkronisasi cloud, dan AndroidManifest saat ini tidak mendeklarasikan izin INTERNET. Namun, Android dapat memiliki mekanisme pencadangan perangkat; manifest mengaktifkan allowBackup, sehingga perilaku pencadangan data aplikasi juga bergantung pada versi Android dan pengaturan pencadangan perangkat. Jangan memasukkan informasi pribadi ke aplikasi jika perangkat digunakan bersama."
                )
                InfoSectionItem(
                    title = "6. Izin Android",
                    content = "Manifest mendeklarasikan izin untuk foreground service khusus, notifikasi, menerima siaran saat perangkat selesai boot, meminta pengecualian optimasi baterai, serta PACKAGE_USAGE_STATS untuk statistik penggunaan aplikasi. Status charger dan boot diproses melalui BroadcastReceiver yang terdaftar di manifest. Akses Usage Stats dan pengecualian optimasi baterai memerlukan persetujuan/tindakan pengguna melalui pengaturan Android. Notifikasi juga mengikuti izin dan kebijakan versi Android yang digunakan."
                )
                InfoSectionItem(
                    title = "7. Catatan Pengukuran",
                    content = "• Level baterai, arus, tegangan, suhu, dan status pengisian dibaca dari API/sensor yang disediakan Android dan perangkat; nilai yang tersedia serta akurasinya dapat berbeda antar model dan kondisi.\n• Nilai daya merupakan hasil perhitungan dari data tegangan dan arus yang dilaporkan perangkat, bukan pengukuran laboratorium independen.\n• Protokol pengisian hanya ditampilkan jika benar-benar diekspos oleh sistem atau perangkat. Arus, tegangan, dan daya baterai tidak cukup untuk mengidentifikasi protokol adaptor.\n• Peringatan anomali adalah indikasi berbasis aturan/heuristik aplikasi, bukan diagnosis kerusakan baterai atau perangkat."
                )
                InfoSectionItem(
                    title = "8. Batasan & Disclaimer",
                    content = "SUPERD POWER GUARD disediakan sebagai alat pemantauan dan informasi, bukan alat ukur laboratorium, layanan sertifikasi, atau alat diagnosis resmi produsen. Hasil dapat dipengaruhi sensor, fuel gauge, kernel, implementasi vendor, dan kebijakan Android. Aplikasi ini tidak menjamin dapat memutus aliran charger atau membatasi pengisian pada semua perangkat. Jangan menggunakan hasil aplikasi sebagai satu-satunya dasar untuk menyimpulkan kondisi baterai atau menentukan tindakan perbaikan."
                )
                InfoSectionItem(
                    title = "9. Teknologi & Credits",
                    content = "SUPERD POWER GUARD dikembangkan menggunakan Kotlin dan Android Jetpack, termasuk Jetpack Compose, Material 3, AndroidX, Kotlin Coroutines, Room, DataStore, serta WorkManager. Penghargaan diberikan kepada para pengembang dan komunitas yang memelihara proyek-proyek tersebut. Nama, merek dagang, hak cipta, dan lisensi setiap teknologi tetap menjadi milik pemilik masing-masing. Tidak ada afiliasi resmi dengan Google atau proyek pihak ketiga yang dinyatakan."
                )
                InfoSectionItem(
                    title = "10. Status Versi",
                    content = "Versi dan version code diambil langsung dari konfigurasi build aplikasi: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}). Aplikasi masih dalam tahap pengembangan dan validasi lintas perangkat. Keberhasilan build dan unit test tidak berarti seluruh fitur telah diuji atau dijamin bekerja pada semua model ponsel."
                )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End) {
                    PowerActionButton(
                        onClick = onDismiss,
                        clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSectionItem(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
