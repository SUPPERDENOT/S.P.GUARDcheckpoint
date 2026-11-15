package com.superdenot.powerguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                val labels = listOf("Home", "Charging", "Insights", "History", "Settings")
                Scaffold(
                    topBar = {
                        TopAppBar(title = {
                            Text("SUPERD POWER GUARD", fontWeight = FontWeight.Bold)
                        })
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(currentTab == 0, { currentTab = 0 }, icon = { Icon(Icons.Rounded.Home, null) }, label = { Text(labels[0]) })
                            NavigationBarItem(currentTab == 1, { currentTab = 1 }, icon = { Icon(Icons.Rounded.BatteryChargingFull, null) }, label = { Text(labels[1]) })
                            NavigationBarItem(currentTab == 2, { currentTab = 2 }, icon = { Icon(Icons.Rounded.Insights, null) }, label = { Text(labels[2]) })
                            NavigationBarItem(currentTab == 3, { currentTab = 3 }, icon = { Icon(Icons.Rounded.History, null) }, label = { Text(labels[3]) })
                            NavigationBarItem(currentTab == 4, { currentTab = 4 }, icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text(labels[4]) })
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
