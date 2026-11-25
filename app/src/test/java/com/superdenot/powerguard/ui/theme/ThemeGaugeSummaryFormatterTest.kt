package com.superdenot.powerguard.ui.theme

import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import com.superdenot.powerguard.ui.theme.MidnightAccent
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
            "Midnight Violet · Ungu · Dua warna" to "Kapsul · Statis",
            ThemeGaugeSummaryFormatter.format(
                ThemePackConfig.MidnightViolet(
                    dualTintMetrics = true,
                    gaugeModel = BatteryGaugeModel.CAPSULE,
                    chargingAnimation = ChargingArcAnimation.STATIC
                )
            )
        )
    }

    @Test
    fun formatsMidnightAccentInThemeSummary() {
        val expected = mapOf(
            MidnightAccent.VIOLET to "Midnight Violet · Ungu · Satu warna",
            MidnightAccent.PINK to "Midnight Violet · Pink · Satu warna",
            MidnightAccent.CYAN to "Midnight Violet · Cyan · Satu warna",
            MidnightAccent.GREEN to "Midnight Violet · Green · Satu warna"
        )
        expected.forEach { (accent, summary) ->
            assertEquals(
                summary to "Arc · Sweep",
                ThemeGaugeSummaryFormatter.format(ThemePackConfig.MidnightViolet(
                    dualTintMetrics = false,
                    midnightAccent = accent
                ))
            )
        }
    }
}
