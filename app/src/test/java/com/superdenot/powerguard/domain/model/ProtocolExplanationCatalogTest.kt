package com.superdenot.powerguard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolExplanationCatalogTest {
    @Test
    fun everyDetectorDisplayLabelHasAnExplanation() {
        val labels = listOf(
            "AC — Daya Standar",
            "AC — Kemampuan Daya Tinggi",
            "AC — Kemampuan 9V+",
            "USB Arus Tinggi (SDP/CDP?)",
            "USB Arus Standar (SDP?)",
            "Fast Charge (HAL)",
            "Tidak Terhubung",
            "Tidak Diekspos Perangkat (SELinux)",
            "Tidak Diekspos Perangkat",
            "Nirkabel (Standar Tak Terverifikasi)",
            "AC — Kemampuan Daya Tidak Tersedia",
            "USB — Arus Maksimum Tidak Tersedia"
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
        val explanation = ProtocolExplanationCatalog.get("AC — Kemampuan Daya Tinggi")
        assertTrue(explanation.summary.contains("bukan daya aktual"))
        assertFalse(explanation.summary.contains("USB Power Delivery sedang digunakan"))
    }

    @Test
    fun unknownLabelGetsHonestFallback() {
        assertEquals(
            "Nilai ini berasal dari nilai eksplisit perangkat atau belum memiliki penjelasan khusus. Aplikasi tidak mengubahnya menjadi klaim protokol lain.",
            ProtocolExplanationCatalog.get("Mystery HAL Value").summary
        )
    }
}
