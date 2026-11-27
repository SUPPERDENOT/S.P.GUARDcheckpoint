package com.superdenot.powerguard.ui.dashboard

import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import java.text.NumberFormat
import java.util.Locale

internal const val OVERVIEW_GRID_COLUMNS = 2
internal const val OVERVIEW_GRID_HORIZONTAL_PADDING_DP = 18f
internal const val OVERVIEW_GRID_GAP_DP = 8f
internal const val OVERVIEW_GRID_MIN_CELL_HEIGHT_DP = 72f

internal object OverviewMetricsGridLayout {
    fun cellWidthDp(screenWidthDp: Float): Float =
        (screenWidthDp - (OVERVIEW_GRID_HORIZONTAL_PADDING_DP * 2f) - OVERVIEW_GRID_GAP_DP) /
            OVERVIEW_GRID_COLUMNS
}

internal data class OverviewMetricsPresentation(
    val estimatedTime: String,
    val peakCurrent: String,
    val powerLimitProfile: String,
    val cellCondition: String
)

internal object OverviewMetricsFormatter {
    fun format(
        isChargingStatusAvailable: Boolean,
        isCharging: Boolean,
        etaMinutes: Long?,
        isEtaTracking: Boolean,
        currentSamples: List<BatterySampleEntity>,
        chargeSource: String,
        isChargeSourceAvailable: Boolean,
        chargingProtocol: String,
        healthPercentage: Float?,
        temperatureCelsius: Float?,
        isTemperatureAvailable: Boolean
    ): OverviewMetricsPresentation = OverviewMetricsPresentation(
        estimatedTime = formatEstimatedTime(isChargingStatusAvailable, isCharging, etaMinutes, isEtaTracking),
        peakCurrent = formatPeakCurrent(isChargingStatusAvailable, isCharging, currentSamples),
        powerLimitProfile = formatPowerLimitProfile(
            isChargingStatusAvailable, isCharging, chargeSource, isChargeSourceAvailable, chargingProtocol
        ),
        cellCondition = formatCellCondition(healthPercentage, temperatureCelsius, isTemperatureAvailable)
    )

    fun formatEstimatedTime(
        isChargingStatusAvailable: Boolean,
        isCharging: Boolean,
        etaMinutes: Long?,
        isEtaTracking: Boolean
    ): String {
        if (!isChargingStatusAvailable || !isCharging || !isEtaTracking || etaMinutes == null || etaMinutes <= 0L) {
            return "Estimasi aktif"
        }
        val hours = etaMinutes / 60L
        val minutes = etaMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours jam $minutes mnt menuju target"
            hours > 0L -> "$hours jam menuju target"
            else -> "$minutes mnt menuju target"
        }
    }

    fun formatPeakCurrent(
        isChargingStatusAvailable: Boolean,
        isCharging: Boolean,
        samples: List<BatterySampleEntity>
    ): String {
        if (!isChargingStatusAvailable) return "Tidak tersedia"
        if (!isCharging) return "—"
        val peak = samples.asSequence()
            .filter { it.isCurrentAvailable && (!it.isChargingStatusAvailable || it.isCharging) }
            .map { kotlin.math.abs(it.currentMa.toLong()) }
            .maxOrNull()
            ?: return "Belum tersedia"
        return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(peak) + " mA"
    }

    fun formatPowerLimitProfile(
        isChargingStatusAvailable: Boolean,
        isCharging: Boolean,
        chargeSource: String,
        isChargeSourceAvailable: Boolean,
        chargingProtocol: String
    ): String {
        if (!isChargingStatusAvailable) return "Status tidak tersedia"
        if (!isCharging) return "Tidak terhubung"
        val source = if (!isChargeSourceAvailable) "Sumber belum diketahui" else when (chargeSource) {
            "AC Adapter" -> "Adaptor AC"
            "USB Port" -> "USB"
            "Wireless" -> "Nirkabel"
            "Unknown Source" -> "Sumber tidak dikenal"
            else -> "Sumber tidak diketahui"
        }
        val capability = when {
            chargingProtocol.startsWith("AC — Kemampuan Sumber Tinggi") -> "≥10 W (metadata)"
            chargingProtocol == "AC — Kemampuan Tegangan 9V+" -> "≥8,5 V (metadata)"
            chargingProtocol == "AC — Kemampuan Sumber Standar" -> "Profil standar"
            chargingProtocol == "AC — Kemampuan Sumber Tidak Tersedia" -> "Kemampuan tidak tersedia"
            chargingProtocol.startsWith("USB Arus Tinggi") -> "Arus maks. ≥900 mA"
            chargingProtocol.startsWith("USB Arus Standar") -> "Arus maks. <900 mA"
            chargingProtocol == "USB — Arus Maksimum Tidak Tersedia" -> "Arus maks. tidak tersedia"
            chargingProtocol.contains("Tidak Diekspos") -> "Metadata tidak diekspos"
            chargingProtocol == "Deteksi protokol nonaktif" -> "Deteksi nonaktif"
            chargingProtocol.isBlank() || chargingProtocol == "Tidak Terhubung" -> "Profil tidak tersedia"
            else -> "Protokol $chargingProtocol"
        }
        return "$source · $capability"
    }

    fun formatCellCondition(
        healthPercentage: Float?,
        temperatureCelsius: Float?,
        isTemperatureAvailable: Boolean
    ): String {
        val soh = healthPercentage?.takeIf { it.isFinite() && it > 0f }
            ?.let { String.format(Locale("id", "ID"), "SOH %.1f%%", it) }
            ?: "SOH belum tersedia"
        val temperature = if (!isTemperatureAvailable || temperatureCelsius == null || !temperatureCelsius.isFinite()) {
            "Suhu tidak tersedia"
        } else when {
            temperatureCelsius >= 40f -> "Suhu tinggi"
            temperatureCelsius >= 38f -> "Suhu hangat"
            else -> "Suhu aman"
        }
        return "$soh · $temperature"
    }

    fun formatSohSummary(healthPercentage: Float?): String =
        healthPercentage?.takeIf { it.isFinite() && it > 0f }
            ?.let { String.format(Locale("id", "ID"), "%.1f%%", it) }
            ?: "Belum tersedia"

    fun formatReadinessSummary(
        isServiceRunning: Boolean,
        monitoringEnabled: Boolean,
        isCharging: Boolean,
        isBatteryOptimizationIgnored: Boolean,
        hasUsagePermission: Boolean
    ): String = when {
        !isBatteryOptimizationIgnored || !hasUsagePermission -> "Perlu Izin"
        isServiceRunning -> "Semua Aktif"
        !monitoringEnabled -> "Monitoring OFF"
        !isCharging -> "Siaga"
        else -> "Menunggu Layanan"
    }
}
