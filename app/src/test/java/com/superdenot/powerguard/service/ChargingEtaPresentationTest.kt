package com.superdenot.powerguard.service

import com.superdenot.powerguard.domain.engine.ChargingEtaEstimator
import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingEtaPresentationTest {
    @Test
    fun etaFormatterUsesHumanReadableIndonesianAndLoadingFallback() {
        assertEquals("Menghitung ETA...", ChargingEtaEstimator.formatEta(null))
        assertEquals("45 menit", ChargingEtaEstimator.formatEta(45L))
        assertEquals("1 jam 5 menit", ChargingEtaEstimator.formatEta(65L))
    }

    @Test
    fun monitorNotificationCollapsedTitleAndExpandedContentIncludeEtaAsEstimate() {
        val state = LiveBatteryState(
            level = 72,
            currentMa = 1_250,
            isCharging = true,
            chargingEtaMinutes = 65L,
            isChargingEtaTracking = true
        )
        assertEquals(
            "Mengisi Daya • 72% • ±1j 5m",
            NotificationVisualRenderer.resolveMonitorContentTitle(state)
        )
        assertEquals(
            "+1250 mA (0.0W) • 0.0°C • +0.0 mAh",
            NotificationVisualRenderer.formatMonitorContentText(state)
        )
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
