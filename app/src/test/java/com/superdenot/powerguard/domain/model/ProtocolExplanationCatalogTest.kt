package com.superdenot.powerguard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolExplanationCatalogTest {
    @Test
    fun everyDetectorDisplayLabelHasAnExplanation() {
        val labels = listOf(
            "AC — Kemampuan Sumber Standar",
            "AC — Kemampuan Sumber Tinggi (≥10 W)",
            "AC — Kemampuan Tegangan 9V+",
            "USB Arus Tinggi (SDP/CDP?)",
            "USB Arus Standar (SDP?)",
            "Fast Charge (HAL)",
            "Deteksi protokol nonaktif",
            "Tidak Terhubung",
            "Tidak Diekspos Perangkat (SELinux)",
            "Tidak Diekspos Perangkat",
            "Nirkabel (Standar Tak Terverifikasi)",
            "AC — Kemampuan Sumber Tidak Tersedia",
            "USB — Arus Maksimum Tidak Tersedia",
            "USB PD PPS",
            "USB PD",
            "USB DCP",
            "USB CDP",
            "USB SDP",
            "AFC",
            "QC",
            "QC 2.0",
            "QC 3.0",
            "VOOC",
            "SuperVOOC",
            "Pump Express",
            "HVDCP"
        )
        labels.forEach { label ->
            val explanation = ProtocolExplanationCatalog.get(label)
            assertTrue(explanation.title.isNotBlank())
            assertTrue(explanation.powerRange.isNotBlank())
            assertTrue(explanation.summary.isNotBlank())
        }
    }

    @Test
    fun capabilityExplanationDoesNotClaimAChargingProtocol() {
        val explanation = ProtocolExplanationCatalog.get("AC — Kemampuan Sumber Tinggi (≥10 W)")
        assertTrue(explanation.summary.contains("bukan daya aktual"))
        assertTrue(ProtocolExplanationCatalog.batterySidePowerExplanation.contains("tegangan baterai × arus baterai"))
        assertTrue(ProtocolExplanationCatalog.comparisonGuidance.contains("pembacaan stabil"))
        assertFalse(explanation.summary.contains("USB Power Delivery sedang digunakan"))
    }

    @Test
    fun unknownLabelGetsHonestFallback() {
        assertEquals(
            "Label ini berasal dari nilai eksplisit perangkat atau belum memiliki penjelasan khusus. Aplikasi tidak mengubahnya menjadi klaim protokol lain.",
            ProtocolExplanationCatalog.get("Mystery HAL Value").summary
        )
    }
}
