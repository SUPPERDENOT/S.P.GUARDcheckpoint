package com.superdenot.powerguard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal fun isNotificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

internal fun formatSystemReadinessSnackbarSummary(
    notificationGranted: Boolean,
    batteryOptimizationIgnored: Boolean,
    usageAccessGranted: Boolean
): String {
    fun status(granted: Boolean) = if (granted) "PASS" else "BELUM"

    return "Status diperbarui • " +
        "Notifikasi: ${status(notificationGranted)} • " +
        "Optimasi baterai: ${status(batteryOptimizationIgnored)} • " +
        "Akses penggunaan: ${status(usageAccessGranted)}"
}
