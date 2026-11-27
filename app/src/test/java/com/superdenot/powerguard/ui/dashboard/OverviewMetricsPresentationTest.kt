package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewMetricsPresentationTest {
    @Test
    fun idlePresentationDoesNotInventEtaPeakOrSourcePower() {
        val result = OverviewMetricsFormatter.format(
            isChargingStatusAvailable = true,
            isCharging = false,
            etaMinutes = null,
            isEtaTracking = false,
            currentSamples = listOf(sample(currentMa = 1_200)),
            chargeSource = "Discharging",
            isChargeSourceAvailable = true,
            chargingProtocol = "Tidak Terhubung",
            healthPercentage = null,
            temperatureCelsius = 36f,
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
            isChargingStatusAvailable = true,
            isCharging = true,
            etaMinutes = 84L,
            isEtaTracking = true,
            currentSamples = listOf(sample(-1_200), sample(1_350), sample(900, isCharging = false)),
            chargeSource = "AC Adapter",
            isChargeSourceAvailable = true,
            chargingProtocol = "AC — Kemampuan Sumber Tinggi (≥10 W)",
            healthPercentage = 88.9f,
            temperatureCelsius = 37.2f,
            isTemperatureAvailable = true
        )

        assertEquals("1 jam 24 mnt menuju target", result.estimatedTime)
        assertEquals("1.350 mA", result.peakCurrent)
        assertEquals("Adaptor AC · ≥10 W (metadata)", result.powerLimitProfile)
        assertEquals("SOH 88,9% · Suhu aman", result.cellCondition)
    }

    @Test
    fun unavailableSamplesAndUnexposedSourceRemainExplicit() {
        assertEquals(
            "Belum tersedia",
            OverviewMetricsFormatter.formatPeakCurrent(true, true, emptyList())
        )
        assertEquals(
            "USB · Metadata tidak diekspos",
            OverviewMetricsFormatter.formatPowerLimitProfile(
                true, true, "USB Port", true, "Tidak Diekspos Perangkat"
            )
        )
        assertEquals(
            "Tidak tersedia",
            OverviewMetricsFormatter.formatPeakCurrent(false, true, emptyList())
        )
    }

    @Test
    fun compactSummaryRowsReflectServiceAndPermissionState() {
        assertEquals("88,9%", OverviewMetricsFormatter.formatSohSummary(88.9f))
        assertEquals("Belum tersedia", OverviewMetricsFormatter.formatSohSummary(null))
        assertEquals(
            "Semua Aktif",
            OverviewMetricsFormatter.formatReadinessSummary(
                isServiceRunning = true,
                monitoringEnabled = true,
                isCharging = true,
                isBatteryOptimizationIgnored = true,
                hasUsagePermission = true
            )
        )
        assertEquals(
            "Perlu Izin",
            OverviewMetricsFormatter.formatReadinessSummary(
                isServiceRunning = false,
                monitoringEnabled = true,
                isCharging = true,
                isBatteryOptimizationIgnored = false,
                hasUsagePermission = true
            )
        )
    }

    private fun sample(
        currentMa: Int,
        isCharging: Boolean = true
    ) = BatterySampleEntity(
        sessionId = 1L,
        timestamp = 1_000L,
        batteryLevel = 60,
        currentMa = currentMa,
        voltageMv = 4_000,
        tempCelsius = 36f,
        powerWatt = 4.8f,
        isScreenOn = true,
        isCurrentAvailable = true,
        isChargingStatusAvailable = true,
        isCharging = isCharging
    )
}
