package com.superdenot.powerguard.ui.theme

import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeGaugeSummaryFormatterTest {
    @Test
    fun formatsBasicThemeAndGaugeAsTwoCompactLines() {
        assertEquals(
            "Basic · Gelap · Cobalt" to "Wave · Comet",
            ThemeGaugeSummaryFormatter.format(
                ThemePackConfig.Basic(
                    appearanceMode = BasicAppearanceMode.DARK,
                    accent = BasicAccent.COBALT,
                    gaugeModel = BatteryGaugeModel.WAVE,
                    chargingAnimation = ChargingArcAnimation.COMET_HEAD
                )
            )
        )
    }

    @Test
    fun formatsMidnightVioletDualTintState() {
        assertEquals(
            "Midnight Violet · Dua warna" to "Kapsul · Statis",
            ThemeGaugeSummaryFormatter.format(
                ThemePackConfig.MidnightViolet(
                    dualTintMetrics = true,
                    gaugeModel = BatteryGaugeModel.CAPSULE,
                    chargingAnimation = ChargingArcAnimation.STATIC
                )
            )
        )
    }
}
