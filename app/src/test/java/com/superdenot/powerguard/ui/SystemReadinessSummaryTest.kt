package com.superdenot.powerguard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemReadinessSummaryTest {
    @Test
    fun summaryIncludesNotificationBatteryOptimizationAndUsageAccessStates() {
        assertEquals(
            "Status diperbarui • Notifikasi: PASS • Optimasi baterai: BELUM • Akses penggunaan: PASS",
            formatSystemReadinessSnackbarSummary(
                notificationGranted = true,
                batteryOptimizationIgnored = false,
                usageAccessGranted = true
            )
        )
    }
}
