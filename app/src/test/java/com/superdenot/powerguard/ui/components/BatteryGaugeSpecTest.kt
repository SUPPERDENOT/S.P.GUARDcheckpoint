package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryGaugeSpecTest {
    private val palette = BatterySemanticColors(
        normal = Color.Blue,
        charging = Color.Green,
        warm = Color.Yellow,
        criticalThermal = Color.Red
    )

    @Test
    fun criticalThermalAlwaysWinsOverWarmAndCharging() {
        assertEquals(
            Color.Red,
            resolveBatteryGaugeColor(
                semanticColors = palette,
                isCriticalThermal = true,
                isWarm = true,
                isCharging = true
            )
        )
    }

    @Test
    fun warmWinsOverCharging() {
        assertEquals(
            Color.Yellow,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = true, isCharging = true)
        )
    }

    @Test
    fun normalChargingUsesGreenAndNormalIdleUsesThemeAccent() {
        assertEquals(
            Color.Green,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = true)
        )
        assertEquals(
            Color.Blue,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = false)
        )
    }

    @Test
    fun segmentedGaugeUsesTwentyFivePercentStepsAcrossZeroToOneHundred() {
        assertEquals(0, segmentedGaugeLitCount(0))
        assertEquals(0, segmentedGaugeLitCount(4))
        assertEquals(1, segmentedGaugeLitCount(5))
        assertEquals(10, segmentedGaugeLitCount(50))
        assertEquals(19, segmentedGaugeLitCount(99))
        assertEquals(20, segmentedGaugeLitCount(100))
        assertEquals(0, segmentedGaugeLitCount(-20))
        assertEquals(20, segmentedGaugeLitCount(140))
    }
}
