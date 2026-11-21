package com.superdenot.powerguard.domain.engine

import java.io.File
import java.io.IOException

/** Exact protocol labels require explicit sysfs/HAL evidence; BatteryManager is capability-only. */
internal object ChargingProtocolDetector {
    const val NOT_CONNECTED = "Tidak Terhubung"
    const val NOT_EXPOSED = "Tidak Diekspos Perangkat"
    const val SELINUX_NOT_EXPOSED = "Tidak Diekspos Perangkat (SELinux)"

    enum class Source { NONE, USB, AC, WIRELESS, UNKNOWN }
    enum class SysfsAccess { READABLE, DENIED, IO_ERROR, UNAVAILABLE }

    data class Input(
        val source: Source,
        val isCharging: Boolean,
        val maxChargingCurrentMicroAmps: Long? = null,
        val maxChargingVoltageMicroVolts: Long? = null,
        val explicitSysfsProtocol: String? = null,
        val sysfsAccess: SysfsAccess = SysfsAccess.UNAVAILABLE
    )

    fun resolveSource(
        plugged: Int,
        hasPluggedMetadata: Boolean,
        isCharging: Boolean,
        acPluggedValue: Int,
        usbPluggedValue: Int,
        wirelessPluggedValue: Int
    ): Source = when (plugged) {
        acPluggedValue -> Source.AC
        usbPluggedValue -> Source.USB
        wirelessPluggedValue -> Source.WIRELESS
        else -> if (plugged != 0 || (isCharging && !hasPluggedMetadata)) Source.UNKNOWN else Source.NONE
    }

    fun microAmpsToMilliAmps(value: Long?): Long? =
        value?.takeIf { it >= 0L }?.div(1_000L)

    fun microVoltsToMilliVolts(value: Long?): Long? =
        value?.takeIf { it >= 0L }?.div(1_000L)

    fun detect(input: Input): String {
        if (!input.isCharging || input.source == Source.NONE) return NOT_CONNECTED
        input.explicitSysfsProtocol?.let(SysfsChargingProtocolReader::normalizeExplicitProtocol)?.let { return it }

        val currentMa = microAmpsToMilliAmps(input.maxChargingCurrentMicroAmps)
        val voltageMv = microVoltsToMilliVolts(input.maxChargingVoltageMicroVolts)
        return when (input.source) {
            Source.USB -> when {
                currentMa == null -> "USB — kemampuan arus tidak tersedia; SDP/CDP tidak terverifikasi"
                currentMa >= USB_HIGH_CURRENT_THRESHOLD_MA -> "USB arus tinggi — SDP/CDP tidak terverifikasi"
                else -> "USB arus standar — SDP/CDP tidak terverifikasi"
            }
            Source.WIRELESS -> "Pengisian Nirkabel — standar tidak terverifikasi"
            Source.AC -> {
                val maxPowerW = if (currentMa != null && voltageMv != null) {
                    currentMa.toDouble() * voltageMv.toDouble() / 1_000_000.0
                } else null
                when {
                    voltageMv != null && voltageMv >= AC_HIGH_VOLTAGE_THRESHOLD_MV -> "AC — kemampuan sumber 9V+"
                    currentMa != null && currentMa >= AC_HIGH_CURRENT_THRESHOLD_MA -> "AC — kemampuan sumber berdaya tinggi"
                    maxPowerW != null && maxPowerW >= AC_HIGH_POWER_THRESHOLD_W -> "AC — kemampuan sumber berdaya tinggi"
                    currentMa != null || voltageMv != null -> "AC — sumber daya standar; DCP tidak terverifikasi"
                    else -> "AC — kemampuan sumber tidak tersedia; DCP tidak terverifikasi"
                }
            }
            Source.UNKNOWN -> if (input.sysfsAccess == SysfsAccess.DENIED) SELINUX_NOT_EXPOSED else NOT_EXPOSED
            Source.NONE -> NOT_CONNECTED
        }
    }

    private const val USB_HIGH_CURRENT_THRESHOLD_MA = 900L
    private const val AC_HIGH_VOLTAGE_THRESHOLD_MV = 9_000L
    private const val AC_HIGH_CURRENT_THRESHOLD_MA = 3_000L
    private const val AC_HIGH_POWER_THRESHOLD_W = 15.0
}

/** Reads only fixed, read-only power_supply metadata paths. */
internal object SysfsChargingProtocolReader {
    private val allowedPaths = listOf(
        "/sys/class/power_supply/usb/real_type",
        "/sys/class/power_supply/usb/type",
        "/sys/class/power_supply/charger/real_type",
        "/sys/class/power_supply/charger/type",
        "/sys/class/power_supply/battery/charge_type"
    )

    data class Result(
        val protocol: String? = null,
        val access: ChargingProtocolDetector.SysfsAccess
    )

    fun read(): Result {
        var readable = false
        var denied = false
        var ioError = false
        for (path in allowedPaths) {
            try {
                val raw = File(path).readText().take(128).trim()
                readable = true
                normalizeExplicitProtocol(raw)?.let {
                    return Result(it, ChargingProtocolDetector.SysfsAccess.READABLE)
                }
            } catch (_: SecurityException) {
                denied = true
            } catch (_: IOException) {
                ioError = true
            }
        }
        val access = when {
            readable -> ChargingProtocolDetector.SysfsAccess.READABLE
            denied -> ChargingProtocolDetector.SysfsAccess.DENIED
            ioError -> ChargingProtocolDetector.SysfsAccess.IO_ERROR
            else -> ChargingProtocolDetector.SysfsAccess.UNAVAILABLE
        }
        return Result(access = access)
    }

    fun normalizeExplicitProtocol(rawValue: String): String? {
        val aliases = mapOf(
            "USB_PD_PPS" to "USB PD PPS", "PD_PPS" to "USB PD PPS",
            "USB_PD" to "USB PD", "PD" to "USB PD",
            "USB_DCP" to "USB DCP", "DCP" to "USB DCP",
            "USB_CDP" to "USB CDP", "CDP" to "USB CDP",
            "USB_SDP" to "USB SDP", "SDP" to "USB SDP",
            "AFC" to "AFC", "QC" to "QC", "QC2" to "QC", "QC3" to "QC",
            "VOOC" to "VOOC", "SUPERVOOC" to "VOOC",
            "PUMP_EXPRESS" to "Pump Express", "PUMP_EXPRESS_PLUS" to "Pump Express",
            "HVDCP" to "HVDCP"
        )
        val normalized = rawValue.trim().uppercase()
            .replace(Regex("[^A-Z0-9_]+"), "_").trim('_')
        return aliases[normalized]
            ?: aliases.entries.sortedByDescending { it.key.length }
                .firstOrNull { normalized.startsWith("${it.key}_") }?.value
            ?: normalized.split('_').firstNotNullOfOrNull { aliases[it] }
    }
}
