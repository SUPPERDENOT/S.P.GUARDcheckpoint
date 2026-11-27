package com.superdenot.powerguard.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class UiCopyRegressionTest {
    @Test
    fun obsoleteLongFooterAndRepeatedHistoryCopyStayOutOfTabScreens() {
        val appScreens = readMainSource("com/superdenot/powerguard/ui/AppScreens.kt")
        val analytics = readMainSource("com/superdenot/powerguard/ui/analytics/AnalyticsScreen.kt")
        val chargingCurve = readMainSource("com/superdenot/powerguard/ui/components/ChargingCurveChart.kt")

        assertFalse(appScreens.contains("SectionHeading(\"KESEHATAN BATERAI (SOH)\""))
        assertFalse(appScreens.contains("SectionHeading(\"KESIAPAN SISTEM\""))
        assertFalse(appScreens.contains("Perubahan diterapkan pada notifikasi pemantauan yang sedang aktif. Tampilan ikon dapat berbeda menurut versi Android dan perangkat."))
        assertFalse(analytics.contains("Ketuk sesi lain untuk memuat kurva sesi tersebut"))
        assertFalse(chargingCurve.contains("Distribusi zona suhu"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/$relativePath"),
            File("app/src/main/java/$relativePath")
        )
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Source file not found for UI copy regression check: $relativePath")
    }
}
