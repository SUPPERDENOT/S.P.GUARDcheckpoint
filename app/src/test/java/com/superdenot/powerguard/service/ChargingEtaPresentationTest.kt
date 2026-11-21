package com.superdenot.powerguard.service

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingEtaPresentationTest {
    @Test
    fun etaFormatterUsesHumanReadableIndonesianAndLoadingFallback() {
        assertEquals("Menghitung ETA...", NotificationVisualRenderer.formatChargingEta(null))
        assertEquals("45 menit", NotificationVisualRenderer.formatChargingEta(45L))
        assertEquals("1 jam 5 menit", NotificationVisualRenderer.formatChargingEta(65L))
    }

    @Test
    fun monitorNotificationShortAndExpandedContentIncludeEtaAsEstimate() {
        val state = LiveBatteryState(
            level = 72,
            currentMa = 1_250,
            isCharging = true,
            chargingEtaMinutes = 65L
        )
        assertTrue(NotificationVisualRenderer.formatMonitorContentText(state).contains("Estimasi penuh: 1 jam 5 menit"))
        assertTrue(NotificationVisualRenderer.formatMonitorExpandedText(state).contains("Estimasi penuh: 1 jam 5 menit"))
    }

    @Test
    fun stagnantAnomalyCodeUsesExistingHumanReadableFormatter() {
        assertEquals(
            "Anomali: Pengisian Stagnan",
            NotificationVisualRenderer.formatAnomalyReason("STAGNANT_CHARGING")
        )
    }
}
