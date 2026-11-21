package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingProtocolDetectorTest {
    private fun detect(
        source: ChargingProtocolDetector.Source,
        currentUa: Long? = null,
        voltageUv: Long? = null,
        protocol: String? = null,
        access: ChargingProtocolDetector.SysfsAccess = ChargingProtocolDetector.SysfsAccess.UNAVAILABLE,
        charging: Boolean = true
    ) = ChargingProtocolDetector.detect(
        ChargingProtocolDetector.Input(
            source = source,
            isCharging = charging,
            maxChargingCurrentMicroAmps = currentUa,
            maxChargingVoltageMicroVolts = voltageUv,
            explicitSysfsProtocol = protocol,
            sysfsAccess = access
        )
    )

    @Test
    fun defaultLiveStateUsesNotConnectedRatherThanProtocolUnavailable() {
        assertEquals(ChargingProtocolDetector.NOT_CONNECTED, LiveBatteryState().chargingProtocol)
    }

    @Test
    fun pluggedSourceResolutionDistinguishesAnExplicitUnpluggedValueFromMissingMetadata() {
        assertEquals(ChargingProtocolDetector.Source.NONE,
            ChargingProtocolDetector.resolveSource(0, true, true, 1, 2, 4))
        assertEquals(ChargingProtocolDetector.Source.UNKNOWN,
            ChargingProtocolDetector.resolveSource(0, false, true, 1, 2, 4))
        assertEquals(ChargingProtocolDetector.Source.UNKNOWN,
            ChargingProtocolDetector.resolveSource(8, true, true, 1, 2, 4))
    }

    @Test
    fun convertsMicroUnitsToMilliUnitsWithoutFloatingPointRounding() {
        assertEquals(2_450L, ChargingProtocolDetector.microAmpsToMilliAmps(2_450_000L))
        assertEquals(9_000L, ChargingProtocolDetector.microVoltsToMilliVolts(9_000_000L))
        assertEquals(2L, ChargingProtocolDetector.microAmpsToMilliAmps(2_999L))
        assertNull(ChargingProtocolDetector.microAmpsToMilliAmps(-1L))
        assertNull(ChargingProtocolDetector.microVoltsToMilliVolts(null))
    }

    @Test
    fun unpluggedOrNotChargingNeverReportsAProtocolOrSourceCapability() {
        assertEquals(ChargingProtocolDetector.NOT_CONNECTED,
            detect(ChargingProtocolDetector.Source.NONE, charging = false))
        assertEquals(ChargingProtocolDetector.NOT_CONNECTED,
            detect(ChargingProtocolDetector.Source.AC, currentUa = 3_000_000L, charging = false))
    }

    @Test
    fun explicitSysfsProtocolTakesPrecedenceOverMetadata() {
        val cases = mapOf(
            "USB_PD" to "USB PD", "USB_PD_PPS" to "USB PD PPS",
            "USB_DCP" to "USB DCP", "USB_CDP" to "USB CDP", "USB_SDP" to "USB SDP",
            "AFC" to "AFC", "QC" to "QC", "VOOC" to "VOOC",
            "PUMP_EXPRESS" to "Pump Express"
        )
        cases.forEach { (raw, expected) ->
            assertEquals(expected, detect(ChargingProtocolDetector.Source.USB,
                currentUa = 500_000L, protocol = raw))
        }
    }

    @Test
    fun genericFastChargeAndSourceTypesAreNotAcceptedAsSpecificProtocols() {
        assertEquals("USB PD PPS", SysfsChargingProtocolReader.normalizeExplicitProtocol("USB_PD_PPS_9V"))
        assertEquals("Fast Charge (HAL)", SysfsChargingProtocolReader.normalizeExplicitProtocol("FAST_CHARGE"))
        assertEquals("Fast Charge (HAL)", SysfsChargingProtocolReader.normalizeExplicitProtocol("Fast"))
        assertEquals("Fast Charge (HAL)", SysfsChargingProtocolReader.normalizeExplicitProtocol("Adaptive"))
        assertEquals("Fast Charge (HAL)", SysfsChargingProtocolReader.normalizeExplicitProtocol("FASTCHARGE"))
        assertEquals("SuperVOOC", SysfsChargingProtocolReader.normalizeExplicitProtocol("SUPERVOOC"))
        assertEquals("QC 3.0", SysfsChargingProtocolReader.normalizeExplicitProtocol("QC3"))
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("UNKNOWN_PD"))
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("USB"))
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("Mains"))
    }

    @Test
    fun usbMetadataProducesOnlyUnverifiedCurrentCapabilityLabels() {
        assertEquals("USB Arus Tinggi (SDP/CDP?)",
            detect(ChargingProtocolDetector.Source.USB, currentUa = 900_000L))
        assertEquals("USB Arus Standar (SDP?)",
            detect(ChargingProtocolDetector.Source.USB, currentUa = 899_000L))
        assertEquals("USB — Arus Maksimum Tidak Tersedia",
            detect(ChargingProtocolDetector.Source.USB))
    }

    @Test
    fun unisocAndAndroidPowerSupplyReadOnlyPathsAreAllowlisted() {
        val paths = SysfsChargingProtocolReader.allowedPaths
        assertEquals(true, "/sys/class/power_supply/battery/charge_type" in paths)
        assertEquals(true, "/sys/class/power_supply/sprd-charger/type" in paths)
        assertEquals(true, "/sys/class/power_supply/sc27xx-fgu/type" in paths)
        assertEquals(true, "/sys/class/power_supply/usb/usb_type" in paths)
        assertEquals(true, "/sys/class/power_supply/usb/real_type" in paths)
        assertEquals(true, "/sys/class/power_supply/usb/type" in paths)
    }

    @Test
    fun unisocChargeTypeFastAndAdaptiveRemainGenericHalEvidence() {
        assertEquals("Fast Charge (HAL)", detect(ChargingProtocolDetector.Source.AC, protocol = "Fast"))
        assertEquals("Fast Charge (HAL)", detect(ChargingProtocolDetector.Source.AC, protocol = "Adaptive"))
    }

    @Test
    fun wirelessMetadataNeverClaimsQiStandard() {
        assertEquals("Pengisian Nirkabel — standar tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.WIRELESS))
    }

    @Test
    fun acNineVoltCapabilityIsConvertedAndHasPriority() {
        assertEquals("AC — Kemampuan 9V+",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_000_000L, voltageUv = 8_500_000L))
    }

    @Test
    fun acHighPowerUsesMaximumCapabilityOfAtLeastTenWatts() {
        assertEquals("AC — Kemampuan Daya Tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_000_000L, voltageUv = 5_000_000L))
        assertEquals("AC — Kemampuan Daya Tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 3_000_000L, voltageUv = 5_000_000L))
        assertEquals("AC — Kemampuan Daya Tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_500_000L, voltageUv = 6_000_000L))
    }

    @Test
    fun acStandardCapabilityDoesNotClaimDcp() {
        assertEquals("AC — Daya Standar",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 1_000_000L, voltageUv = 5_000_000L))
        assertEquals("AC — Kemampuan Daya Tidak Tersedia",
            detect(ChargingProtocolDetector.Source.AC))
    }

    @Test
    fun permissionDeniedIoErrorsAreDistinguishedFromMissingSysfsNodes() {
        assertEquals(true, SysfsChargingProtocolReader.isPermissionDeniedIOExceptionMessage(
            "open failed: EACCES (Permission denied)"
        ))
        assertEquals(false, SysfsChargingProtocolReader.isPermissionDeniedIOExceptionMessage(
            "No such file or directory"
        ))
        assertEquals(false, SysfsChargingProtocolReader.isPermissionDeniedIOExceptionMessage(null))
    }

    @Test
    fun detectionDisabledHasAnExplicitHudOnlyLabel() {
        assertEquals("Deteksi protokol nonaktif", ChargingProtocolDetector.DETECTION_DISABLED)
    }

    @Test
    fun unknownPlugSourceAddsSelinuxSuffixOnlyWhenAccessWasDenied() {
        assertEquals(ChargingProtocolDetector.SELINUX_NOT_EXPOSED,
            detect(ChargingProtocolDetector.Source.UNKNOWN, access = ChargingProtocolDetector.SysfsAccess.DENIED))
        assertEquals(ChargingProtocolDetector.NOT_EXPOSED,
            detect(ChargingProtocolDetector.Source.UNKNOWN, access = ChargingProtocolDetector.SysfsAccess.IO_ERROR))
        assertEquals(ChargingProtocolDetector.NOT_EXPOSED,
            detect(ChargingProtocolDetector.Source.UNKNOWN, access = ChargingProtocolDetector.SysfsAccess.UNAVAILABLE))
    }
}
