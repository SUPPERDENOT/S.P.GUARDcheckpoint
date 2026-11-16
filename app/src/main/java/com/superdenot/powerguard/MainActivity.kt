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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
                val labels = listOf("Home", "Charging", "Insights", "History", "Settings")
                Scaffold(
                    topBar = {
                        TopAppBar(title = {
                            Text("SUPERD GUARD", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.2.sp, maxLines = 1)
                        })
                    },
                    bottomBar = {
                        NavigationBar(modifier = Modifier.height(62.dp), tonalElevation = 1.dp) {
                            NavigationBarItem(currentTab == 0, { currentTab = 0 }, icon = { Icon(Icons.Rounded.Home, null, modifier = Modifier.size(19.dp)) }, label = { Text(labels[0], fontSize = 9.sp, maxLines = 1) })
                            NavigationBarItem(currentTab == 1, { currentTab = 1 }, icon = { Icon(Icons.Rounded.BatteryChargingFull, null, modifier = Modifier.size(19.dp)) }, label = { Text(labels[1], fontSize = 9.sp, maxLines = 1) })
                            NavigationBarItem(currentTab == 2, { currentTab = 2 }, icon = { Icon(Icons.Rounded.Insights, null, modifier = Modifier.size(19.dp)) }, label = { Text(labels[2], fontSize = 9.sp, maxLines = 1) })
                            NavigationBarItem(currentTab == 3, { currentTab = 3 }, icon = { Icon(Icons.Rounded.History, null, modifier = Modifier.size(19.dp)) }, label = { Text(labels[3], fontSize = 9.sp, maxLines = 1) })
                            NavigationBarItem(currentTab == 4, { currentTab = 4 }, icon = { Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(19.dp)) }, label = { Text(labels[4], fontSize = 9.sp, maxLines = 1) })
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
