package com.superdenot.powerguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.superdenot.powerguard.ui.theme.PowerGuardTheme
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PowerGuardTheme {
                val context = LocalContext.current
                val vm: PowerGuardViewModel = viewModel()
                var currentTab by remember { mutableIntStateOf(0) }
                val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    vm.startMonitorIfCharging()
                }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    vm.startMonitorIfCharging()
                    vm.refreshPermissionsAndUsage()
                }

                val labels = listOf("Overview", "Charging", "Insights", "History", "Settings")
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
                        TopAppBar(
                            title = {
                                androidx.compose.foundation.layout.Column {
                                    Text(
                                        "SUPERD POWER GUARD",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.45.sp
                                    )
                                    Text(
                                        "BATTERY DIAGNOSTIC SYSTEM",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.1.sp
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(72.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = "Overview", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[0], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Rounded.BatteryChargingFull, contentDescription = "Charging", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[1], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Rounded.Insights, contentDescription = "Insights", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[2], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Rounded.History, contentDescription = "History", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[3], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(21.dp)) },
                                label = { Text(labels[4], fontSize = 11.sp, maxLines = 1) },
                                colors = navColors
                            )
                        }
                    }
                ) { padding ->
                    when (currentTab) {
                        0 -> SuperdDashboardScreen(vm, padding)
                        1 -> ChargingScreen(vm, padding)
                        2 -> InsightsScreen(vm, padding)
                        3 -> HistoryScreen(vm, padding)
                        else -> SettingsScreen(vm, padding)
                    }
                }
            }
        }
    }
}
