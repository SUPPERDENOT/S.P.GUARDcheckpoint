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
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("FAST_CHARGE"))
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("USB"))
        assertNull(SysfsChargingProtocolReader.normalizeExplicitProtocol("Mains"))
    }

    @Test
    fun usbMetadataProducesOnlyUnverifiedCurrentCapabilityLabels() {
        assertEquals("USB arus tinggi — SDP/CDP tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.USB, currentUa = 900_000L))
        assertEquals("USB arus standar — SDP/CDP tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.USB, currentUa = 899_000L))
        assertEquals("USB — kemampuan arus tidak tersedia; SDP/CDP tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.USB))
    }

    @Test
    fun wirelessMetadataNeverClaimsQiStandard() {
        assertEquals("Pengisian Nirkabel — standar tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.WIRELESS))
    }

    @Test
    fun acNineVoltCapabilityIsConvertedAndHasPriority() {
        assertEquals("AC — kemampuan sumber 9V+",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_000_000L, voltageUv = 9_000_000L))
    }

    @Test
    fun acHighPowerUsesMaximumPowerOrCurrentThreshold() {
        assertEquals("AC — kemampuan sumber berdaya tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 3_000_000L, voltageUv = 5_000_000L))
        assertEquals("AC — kemampuan sumber berdaya tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 3_000_000L))
        assertEquals("AC — kemampuan sumber berdaya tinggi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_500_000L, voltageUv = 6_000_000L))
    }

    @Test
    fun acStandardCapabilityDoesNotClaimDcp() {
        assertEquals("AC — sumber daya standar; DCP tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.AC, currentUa = 2_000_000L, voltageUv = 5_000_000L))
        assertEquals("AC — kemampuan sumber tidak tersedia; DCP tidak terverifikasi",
            detect(ChargingProtocolDetector.Source.AC))
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
