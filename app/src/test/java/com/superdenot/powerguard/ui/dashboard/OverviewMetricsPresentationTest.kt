package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewMetricsPresentationTest {
    @Test
    fun idlePresentationDoesNotInventEtaPeakOrSourcePower() {
        val result = OverviewMetricsFormatter.format(
            isChargingStatusAvailable = true, isCharging = false, etaMinutes = null, isEtaTracking = false,
            currentSamples = listOf(sample(1_200)), chargeSource = "Discharging", isChargeSourceAvailable = true,
            chargingProtocol = "Tidak Terhubung", healthPercentage = null, temperatureCelsius = 36f,
            isTemperatureAvailable = true
        )
        assertEquals("Estimasi aktif", result.estimatedTime)
        assertEquals("—", result.peakCurrent)
        assertEquals("Tidak terhubung", result.powerLimitProfile)
        assertEquals("SOH belum tersedia · Suhu aman", result.cellCondition)
    }

    @Test
    fun chargingPresentationUsesOnlyAvailableEtaCurrentAndSourceMetadata() {
        val result = OverviewMetricsFormatter.format(
            isChargingStatusAvailable = true, isCharging = true, etaMinutes = 84L, isEtaTracking = true,
            currentSamples = listOf(sample(-1_200), sample(1_350), sample(900, false)),
            chargeSource = "AC Adapter", isChargeSourceAvailable = true,
            chargingProtocol = "AC — Kemampuan Sumber Tinggi (≥10 W)",
            healthPercentage = 88.9f, temperatureCelsius = 37.2f, isTemperatureAvailable = true
        )
        assertEquals("1 jam 24 mnt menuju target", result.estimatedTime)
        assertEquals("1.350 mA", result.peakCurrent)
        assertEquals("Adaptor AC · ≥10 W (metadata)", result.powerLimitProfile)
        assertEquals("SOH 88,9% · Suhu aman", result.cellCondition)
    }

    @Test
    fun liveCurrentIsShownImmediatelyWhenNoPersistedSessionSampleExists() {
        assertEquals(760L, OverviewMetricsFormatter.resolvePeakCurrentMa(emptyList(), 760, null))
        assertEquals("760 mA", OverviewMetricsFormatter.formatPeakCurrent(true, true, emptyList(), 760, null))
    }

    @Test
    fun peakCurrentUsesTheMaximumOfPersistedLiveAndInMemoryValues() {
        val persisted = listOf(sample(1_100))
        assertEquals(1_450L, OverviewMetricsFormatter.resolvePeakCurrentMa(persisted, 1_450, 1_300))
        assertEquals(1_500L, OverviewMetricsFormatter.resolvePeakCurrentMa(persisted, 900, 1_500))
    }

    @Test
    fun unavailableSamplesAndUnexposedSourceRemainExplicit() {
        assertEquals("Belum tersedia", OverviewMetricsFormatter.formatPeakCurrent(true, true, emptyList()))
        assertEquals("USB · Metadata tidak diekspos",
            OverviewMetricsFormatter.formatPowerLimitProfile(true, true, "USB Port", true, "Tidak Diekspos Perangkat"))
        assertEquals("Tidak tersedia", OverviewMetricsFormatter.formatPeakCurrent(false, true, emptyList()))
    }

    @Test
    fun compactSummaryRowsReflectServiceAndPermissionState() {
        assertEquals("88,9%", OverviewMetricsFormatter.formatSohSummary(88.9f))
        assertEquals("Belum tersedia", OverviewMetricsFormatter.formatSohSummary(null))
        assertEquals("Semua Aktif", OverviewMetricsFormatter.formatReadinessSummary(true, true, true, true, true, true))
        assertEquals("Perlu Izin", OverviewMetricsFormatter.formatReadinessSummary(false, true, true, false, true, true))
        assertEquals("Perlu Izin", OverviewMetricsFormatter.formatReadinessSummary(true, true, true, true, true, false))
    }

    @Test
    fun twoColumnGridFitsThe360DpWidthBudget() {
        assertEquals(2, OVERVIEW_GRID_COLUMNS)
        assertEquals(158f, OverviewMetricsGridLayout.cellWidthDp(360f), 0f)
        assertTrue(OVERVIEW_GRID_MIN_CELL_HEIGHT_DP >= 72f)
    }

    private fun sample(currentMa: Int, isCharging: Boolean = true) = BatterySampleEntity(
        sessionId = 1L, timestamp = 1_000L, batteryLevel = 60, currentMa = currentMa, voltageMv = 4_000,
        tempCelsius = 36f, powerWatt = 4.8f, isScreenOn = true, isCurrentAvailable = true,
        isChargingStatusAvailable = true, isCharging = isCharging
    )
}
