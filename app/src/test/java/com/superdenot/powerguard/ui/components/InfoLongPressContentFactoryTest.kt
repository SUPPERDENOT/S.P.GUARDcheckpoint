package com.superdenot.powerguard.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoLongPressContentFactoryTest {
    @Test
    fun statusBarCardExplanationKeepsIndicatorSeparateFromMonitoringNotification() {
        val info = InfoLongPressContentFactory.statusBarIndicator()
        assertEquals("Indikator status bar", info.title)
        assertTrue(info.body.contains("parameter baterai"))
        assertTrue(info.body.contains("Monitoring Notification"))
        assertTrue(info.body.contains("panel notifikasi"))
    }

    @Test
    fun headerPillExplanationSeparatesChargingFromActualServiceState() {
        val info = InfoLongPressContentFactory.headerStatus(
            statusLabel = "Mengisi",
            isServiceRunning = false,
            isChargingStatusAvailable = true,
            isCharging = true
        )
        assertEquals("Status pemantauan dan pengisian", info.title)
        assertTrue(info.body.contains("Status pengisian dari perangkat: Mengisi"))
        assertTrue(info.body.contains("Layanan pemantauan latar belakang: Tidak berjalan"))
        assertTrue(info.body.contains("dua informasi berbeda"))
    }

    @Test
    fun headerPillDoesNotInventChargingStateWhenUnavailable() {
        val info = InfoLongPressContentFactory.headerStatus(
            statusLabel = "Siaga",
            isServiceRunning = false,
            isChargingStatusAvailable = false,
            isCharging = false
        )
        assertTrue(info.body.contains("Status pengisian dari perangkat: Tidak tersedia"))
    }
}
