package com.superdenot.powerguard.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val activeState = selectDashboardLiveState(isServiceRunning, serviceState, fallbackState)

    val latestHealth by viewModel.latestHealth.collectAsState()
    val designCapacity by viewModel.designCapacityMah.collectAsState()
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
                                color = if (activeState.isFastCharging) CyanFastCharge.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (activeState.isFastCharging) "⚡ FAST CHARGING (${activeState.chargeSource})"
                                    else activeState.chargeSource.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeState.isFastCharging) CyanFastCharge else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                            text = "Rasio Deep Sleep CPU: $deepSleepPct% (Tinggi = minim wakelock latar belakang)",
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
                            } else {
                                Text(
                                    text = "Estimasi Real: Belum dikalibrasi (cas >= 40% dalam 1 sesi)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Kapasitas Desain Pabrik: $designCapacity mAh",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showCapacityDialog = true }) {
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
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Izinkan Abaikan Optimasi Baterai (Wajib)")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldCharge, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bebas pembatasan latar belakang Android", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (!hasUsagePerm) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Aktifkan Akses Penggunaan Aplikasi")
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startManualMonitorService()
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = if (isServiceRunning) {
                                            "✓ Monitor Service berjalan aktif di latar belakang."
                                        } else {
                                            "Memulai layanan pemantauan sensor daya..."
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCharge),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Rounded.CheckCircle else Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceRunning) "Monitor Service Aktif" else "Aktifkan Monitor Service",
                                fontWeight = FontWeight.Bold
                            )
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

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showReadMoreDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanFastCharge),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp)
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
        var inputText by remember { mutableStateOf(designCapacity.toString()) }
        AlertDialog(
            onDismissRequest = { showCapacityDialog = false },
            title = { Text("Kalibrasi Kapasitas Pabrik (mAh)") },
            text = {
                Column {
                    Text(
                        "Masukkan kapasitas baterai resmi spesifikasi ponsel Anda (misal: 5000) untuk akurasi persentase kesehatan baterai.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kapasitas (mAh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    inputText.toIntOrNull()?.let { viewModel.updateDesignCapacity(it) }
                    showCapacityDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCapacityDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showReadMoreDialog) {
        ReadMoreInfoDialog(onDismiss = { showReadMoreDialog = false })
    }
}

@Composable
fun ReadMoreInfoDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = CyanFastCharge,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Superd Power Guard — Info",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InfoSectionItem(
                    title = "1. About SUPERD POWER GUARD",
                    content = "• Nama Aplikasi: Superd Power Guard\n• Deskripsi & Fungsi: Aplikasi diagnostik baterai canggih untuk pemantauan arus (mA), tegangan (mV), daya (Watt), suhu (°C), dan kesehatan baterai secara real-time.\n• Developer: superdenot\n• Versi: v1.0.0 (Build 2026)"
                )
                InfoSectionItem(
                    title = "2. License & Copyright",
                    content = "• Lisensi Utama: MIT License / Open Source Proprietary\n• Informasi Hak Cipta: © 2026 superdenot. Seluruh hak cipta dilindungi undang-undang.\n• Ketentuan Penggunaan: Aplikasi ini disediakan untuk penggunaan personal dan diagnostik perangkat."
                )
                InfoSectionItem(
                    title = "3. Open Source Licenses",
                    content = "• Jetpack Compose & AndroidX Libraries (Google / Apache 2.0)\n• Room Database & WorkManager (Google / Apache 2.0)\n• Kotlin Coroutines & Kotlinx Serialization (JetBrains / Apache 2.0)\n• DataStore Preferences (Google / Apache 2.0)"
                )
                InfoSectionItem(
                    title = "4. Privacy Policy",
                    content = "• Informasi Data: Status baterai lokal, sensor daya hardware, dan statistik penggunaan aplikasi (Usage Stats) 24 jam terakhir.\n• Penyimpanan & Penggunaan: Seluruh data sesi dan riwayat disimpan secara lokal di SQLite Room Database perangkat Anda tanpa pengiriman ke server eksternal.\n• Izin Android: Foreground Service, Package Usage Stats, Ignore Battery Optimizations."
                )
                InfoSectionItem(
                    title = "5. Terms of Use",
                    content = "• Pengguna bertanggung jawab penuh atas penggunaan aplikasi pada perangkat masing-masing.\n• Dilarang melakukan dekompilasi, reverse engineering, atau modifikasi tanpa izin resmi dari developer."
                )
                InfoSectionItem(
                    title = "6. Disclaimer",
                    content = "• Hasil pengukuran arus, tegangan, dan estimasi kesehatan baterai (mAh) memiliki keterbatasan akurasi bergantung pada pembacaan HAL (Hardware Abstraction Layer) kernel masing-masing ponsel.\n• Aplikasi ini BUKAN alat diagnosis kerusakan perangkat keras resmi."
                )
                InfoSectionItem(
                    title = "7. Battery Measurement Notes",
                    content = "• Arus (mA): Menunjukkan arus masuk (positif saat charging) atau arus keluar (negatif saat discharging).\n• Daya & Tegangan: Dihitung langsung dari sensor baterai sistem.\n• Variasi Perangkat: Hasil antarperangkat dapat bervariasi bergantung pada dukungan chip power management (PMIC)."
                )
                InfoSectionItem(
                    title = "8. Security Notes",
                    content = "• Keamanan & Pemrosesan: Superd Power Guard tidak mengumpulkan atau mengirimkan data pribadi pengguna ke server eksternal.\n• Transparansi Izin: Seluruh izin aplikasi digunakan semata-mata untuk fungsi pemantauan daya dan diagnostik."
                )
                InfoSectionItem(
                    title = "9. Version & Build",
                    content = "• Version Name: 1.0.0\n• Build Number: 20261112\n• Konfigurasi: CompileSDK 35, MinSDK 26 (Android 8.0+)\n• Informasi versi diambil otomatis dari konfigurasi build aplikasi."
                )
                InfoSectionItem(
                    title = "10. Credits & Acknowledgements",
                    content = "• Developer Aplikasi: superdenot\n• Teknologi & Framework: Android Jetpack Compose & Material 3\n• Kontributor & Desain: Tim Komunitas RT 01 / Dapur Ken'Chan Ecosystem\n• Ucapan Terima Kasih: Kepada seluruh pengguna dan kontributor open-source yang mendukung pengembangan."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanFastCharge)
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    )
}

@Composable
private fun InfoSectionItem(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanFastCharge
        )
        Spacer(modifier = Modifier.height(4.dp))
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
