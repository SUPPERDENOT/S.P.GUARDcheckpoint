package com.superdenot.powerguard.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telecom.TelecomManager
import com.superdenot.powerguard.data.local.PopupDetectorPreferences
import com.superdenot.powerguard.domain.security.AppPackageSignals
import com.superdenot.powerguard.domain.security.PopupHijackDetector
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.ScreenUsageEvent
import com.superdenot.powerguard.domain.security.ScreenUsageEventType
import java.util.concurrent.TimeUnit

internal data class PopupDetectorScanResult(
    val hasUsagePermission: Boolean,
    val hasActivityData: Boolean,
    val archivePersisted: Boolean,
    val incidents: List<PopupIncident>,
    val scannedAtMs: Long
)

internal class PopupDetectorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val preferences = PopupDetectorPreferences(appContext)

    fun hasUsagePermission(): Boolean {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun scan(nowMs: Long = System.currentTimeMillis()): PopupDetectorScanResult {
        if (!hasUsagePermission()) return PopupDetectorScanResult(false, false, true, emptyList(), nowMs)
        val events = readUsageEvents(nowMs)
        val browserPackages = resolveBrowserPackages()
        val homePackage = resolveHomePackage()
        val keyboardPackage = Settings.Secure.getString(
            appContext.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        )?.let { android.content.ComponentName.unflattenFromString(it)?.packageName }
        val dialerPackage = runCatching {
            (appContext.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
        }.getOrNull()

        val signals = events.mapNotNull { it.packageName }.distinct().associateWith { packageName ->
            resolveSignals(packageName, browserPackages, homePackage, keyboardPackage, dialerPackage)
        }
        val incidents = PopupHijackDetector.evaluateEvents(
            events = events,
            appSignals = signals,
            safePackages = preferences.safePackages(),
            nowMs = nowMs
        )
        val archivePersisted = preferences.appendIncidents(incidents)
        return PopupDetectorScanResult(
            hasUsagePermission = true,
            hasActivityData = events.any { it.eventType == ScreenUsageEventType.ACTIVITY_RESUMED },
            archivePersisted = archivePersisted,
            incidents = incidents,
            scannedAtMs = nowMs
        )
    }

    fun archivedIncidents(): List<PopupIncident> = preferences.archivedIncidents()
    fun safePackages(): Set<String> = preferences.safePackages()
    fun markSafe(packageName: String): Boolean = preferences.markSafe(packageName)
    fun restoreSafePackage(packageName: String): Boolean = preferences.restoreSafePackage(packageName)
    fun restoreSafePackages(): Boolean = preferences.restoreSafePackages()

    private fun readUsageEvents(nowMs: Long): List<ScreenUsageEvent> {
        val manager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startMs = nowMs - TimeUnit.HOURS.toMillis(24)
        val usageEvents = manager.queryEvents(startMs, nowMs) ?: return emptyList()
        val event = UsageEvents.Event()
        val result = ArrayList<ScreenUsageEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = when {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND -> ScreenUsageEventType.ACTIVITY_RESUMED
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND -> ScreenUsageEventType.ACTIVITY_PAUSED
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE -> ScreenUsageEventType.SCREEN_INTERACTIVE
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN -> ScreenUsageEventType.KEYGUARD_HIDDEN
                else -> ScreenUsageEventType.OTHER
            }
            if (type != ScreenUsageEventType.OTHER) {
                result += ScreenUsageEvent(event.packageName, type, event.timeStamp)
            }
        }
        return result
    }

    private fun resolveBrowserPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolved = runCatching {
            queryActivities(intent).mapNotNull { it.activityInfo?.packageName }.toSet()
        }.getOrDefault(emptySet())
        return resolved + FALLBACK_BROWSER_PACKAGES
    }

    private fun resolveHomePackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(
                    intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }?.activityInfo?.packageName
        }.getOrNull()
    }

    private fun resolveSignals(
        packageName: String,
        browserPackages: Set<String>,
        homePackage: String?,
        keyboardPackage: String?,
        dialerPackage: String?
    ): AppPackageSignals {
        val base = AppPackageSignals(
            packageName = packageName,
            isBrowser = packageName in browserPackages,
            isHomeLauncher = packageName == homePackage,
            isKeyboard = packageName == keyboardPackage,
            isPackageInstaller = packageName in PACKAGE_INSTALLER_PACKAGES,
            isPermissionController = packageName in PERMISSION_CONTROLLER_PACKAGES,
            isSystemUi = packageName == "com.android.systemui",
            isDialer = packageName == dialerPackage || packageName in DIALER_PACKAGES,
            isSettings = packageName == "com.android.settings",
            isSelf = packageName == appContext.packageName,
            isDocumentPicker = packageName in DOCUMENT_PICKER_PACKAGES
        )
        if (packageName in SYSTEM_EXCLUDED_PACKAGES || base.isExcluded) return base.copy(isSystemApp = true)

        val appInfo = try {
            getApplicationInfo(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return base.copy(isMetadataResolved = false)
        } catch (_: SecurityException) {
            return base.copy(isMetadataResolved = false)
        }
        val firstInstallTime = runCatching { getPackageInfo(packageName).firstInstallTime }.getOrNull()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        val hasLauncher = runCatching { queryActivities(launcherIntent).isNotEmpty() }.getOrDefault(true)
        val isSystem = appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val overlayAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                (appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager)
                    .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, appInfo.uid, packageName) ==
                    AppOpsManager.MODE_ALLOWED
            }.getOrDefault(false)
        } else {
            runCatching {
                @Suppress("DEPRECATION")
                (appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager)
                    .checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, appInfo.uid, packageName) ==
                    AppOpsManager.MODE_ALLOWED
            }.getOrDefault(false)
        }
        return base.copy(
            isMetadataResolved = true,
            isSystemApp = isSystem,
            hasLauncherActivity = hasLauncher,
            firstInstallTimeMs = firstInstallTime,
            hasOverlayPermission = overlayAllowed
        )
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }

    private fun getPackageInfo(packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

    private fun queryActivities(intent: Intent) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

    private companion object {
        val FALLBACK_BROWSER_PACKAGES = setOf(
            "com.android.chrome", "com.google.android.apps.chrome", "com.brave.browser",
            "org.mozilla.firefox", "org.mozilla.firefox_beta", "com.opera.browser",
            "com.sec.android.app.sbrowser", "com.microsoft.emmx", "com.kiwibrowser.browser"
        )
        val PACKAGE_INSTALLER_PACKAGES = setOf(
            "com.android.packageinstaller", "com.google.android.packageinstaller"
        )
        val PERMISSION_CONTROLLER_PACKAGES = setOf(
            "com.android.permissioncontroller", "com.google.android.permissioncontroller"
        )
        val DIALER_PACKAGES = setOf("com.android.dialer", "com.google.android.dialer")
        val DOCUMENT_PICKER_PACKAGES = setOf(
            "com.android.documentsui", "com.google.android.documentsui",
            "com.sec.android.gallery3d", "com.google.android.apps.photos"
        )
        val SYSTEM_EXCLUDED_PACKAGES = setOf("com.android.systemui", "com.android.settings")
    }
}
