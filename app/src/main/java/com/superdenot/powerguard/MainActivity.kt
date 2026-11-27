package com.superdenot.powerguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superdenot.powerguard.ui.*
import com.superdenot.powerguard.ui.components.InfoLongPressContentFactory
import com.superdenot.powerguard.ui.components.InfoLongPressDialog
import com.superdenot.powerguard.ui.components.InfoLongPressContent
import com.superdenot.powerguard.ui.components.infoLongPress
import com.superdenot.powerguard.ui.dashboard.selectDashboardLiveState
import com.superdenot.powerguard.ui.theme.PowerGuardTheme
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.ui.theme.StatusReadyGreen
import com.superdenot.powerguard.ui.theme.StatusErrorRed
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: PowerGuardViewModel = viewModel()
            val themePackConfig by vm.themePackConfig.collectAsState()
            PowerGuardTheme(config = themePackConfig) {
                val context = LocalContext.current
                var currentTab by remember { mutableIntStateOf(0) }
                var headerLongPressInfo by remember { mutableStateOf<InfoLongPressContent?>(null) }
                val setupPrefs = remember { context.getSharedPreferences("spg_initial_setup", 0) }
                var showPermissionSetup by remember { mutableStateOf(!setupPrefs.getBoolean("completed", false)) }
                var notificationGranted by remember {
                    mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                }
                val batteryOptimizationIgnored by vm.isBatteryOptimizationIgnored.collectAsState()
                val usageAccessGranted by vm.hasUsagePermission.collectAsState()
                val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    notificationGranted = it
                    vm.startMonitorIfCharging()
                }
                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    vm.setAppForeground(true)
                }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    vm.setAppForeground(false)
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    vm.startMonitorIfCharging()
                    if (!showPermissionSetup && currentTab != 0) {
                        vm.refreshPermissionsAndUsage()
                    }
                    notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }

                val labels = listOf("Ringkasan", "Pengisian", "Analisis", "Riwayat", "Setelan")
                val headerServiceRunning by vm.isServiceRunning.collectAsState()
                val headerServiceState by vm.liveState.collectAsState()
                val headerFallbackState by vm.fallbackState.collectAsState()
                val headerServiceAt by vm.lastSensorUpdateElapsedRealtime.collectAsState()
                val headerFallbackAt by vm.fallbackUpdatedAtElapsedRealtime.collectAsState()
                val headerBatteryState = selectDashboardLiveState(
                    headerServiceRunning, headerServiceState, headerFallbackState,
                    headerServiceAt, headerFallbackAt, SystemClock.elapsedRealtime()
                )
                val headerLabel = when {
                    headerBatteryState.isChargingStatusAvailable && headerBatteryState.isLevelAvailable && headerBatteryState.level >= 100 && !headerBatteryState.isCharging -> "Penuh"
                    headerBatteryState.isChargingStatusAvailable && headerBatteryState.isCharging -> "Mengisi"
                    headerServiceRunning -> "Aktif"
                    else -> "Siaga"
                }
                val headerStatusColor = when {
                    headerLabel == "Mengisi" || headerLabel == "Penuh" -> EmeraldCharge
                    headerLabel == "Aktif" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(38.dp).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SUPERD POWER GUARD", modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.35.sp,
                                    maxLines = 1, softWrap = false, color = MaterialTheme.colorScheme.onBackground)
                                Surface(
                                    modifier = Modifier.infoLongPress(
                                        onLongPress = {
                                            headerLongPressInfo = InfoLongPressContentFactory.headerStatus(
                                                statusLabel = headerLabel,
                                                isServiceRunning = headerServiceRunning,
                                                isChargingStatusAvailable = headerBatteryState.isChargingStatusAvailable,
                                                isCharging = headerBatteryState.isCharging
                                            )
                                        },
                                        onClick = {}
                                    ),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(headerStatusColor))
                                        Text(headerLabel, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                                            color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(64.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = "Ringkasan", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[0], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Rounded.BatteryChargingFull, contentDescription = "Pengisian", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[1], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Rounded.Insights, contentDescription = "Analisis", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[2], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Rounded.History, contentDescription = "Riwayat", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[3], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Setelan", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[4], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                        }
                    }
                ) { padding ->
                    when (currentTab) {
                        0 -> SuperdDashboardScreen(vm, padding, isActive = !showPermissionSetup)
                        1 -> ChargingScreen(vm, padding)
                        2 -> InsightsScreen(vm, padding)
                        3 -> HistoryScreen(vm, padding)
                        else -> SettingsScreen(vm, padding, onOpenPermissionSetup = { showPermissionSetup = true })
                    }
                }
                headerLongPressInfo?.let { info ->
                    InfoLongPressDialog(
                        title = info.title,
                        body = info.body,
                        onDismiss = { headerLongPressInfo = null }
                    )
                }
                if (showPermissionSetup) {
                    InitialPermissionSetup(
                        notificationGranted = notificationGranted,
                        batteryOptimizationIgnored = batteryOptimizationIgnored,
                        usageAccessGranted = usageAccessGranted,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else notificationGranted = true
                        },
                        onOpenBatteryOptimization = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:" + context.packageName)
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        onOpenUsageAccess = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        },
                        onResumeRefresh = { vm.refreshPermissionsAndUsage() },
                        onContinue = {
                            setupPrefs.edit().putBoolean("completed", true).apply()
                            showPermissionSetup = false
                        }
                    )
                }
            }
        }

    }
}

@Composable
private fun InitialPermissionSetup(
    notificationGranted: Boolean,
    batteryOptimizationIgnored: Boolean,
    usageAccessGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onResumeRefresh: () -> Unit,
    onContinue: () -> Unit
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onResumeRefresh()
    }

    Dialog(onDismissRequest = onContinue) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("PERSIAPAN AWAL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Siapkan Superd Power Guard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Atur akses yang mendukung notifikasi pemantauan, auto-start saat charger tersambung, dan analisis penggunaan. Anda tetap bisa melanjutkan tanpa memberikan akses opsional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SetupPermissionRow("Notifikasi pemantauan", "Disarankan agar kartu layanan terlihat di panel notifikasi.", notificationGranted, "IZINKAN", onRequestNotifications)
                SetupPermissionRow("Optimasi baterai", "Pengecualian membantu Android mengizinkan auto-start saat charger tersambung.", batteryOptimizationIgnored, "ATUR", onOpenBatteryOptimization)
                SetupPermissionRow("Akses pengguna", "Opsional — diperlukan untuk analisis penggunaan aplikasi.", usageAccessGranted, "ATUR", onOpenUsageAccess)
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Lanjut ke aplikasi") }
                Text("Akses opsional tidak akan dipaksakan. Anda dapat mengubahnya kapan saja melalui Setelan.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SetupPermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(if (granted) "PASS" else "BELUM", color = if (granted) StatusReadyGreen else StatusErrorRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
