package com.superdenot.powerguard.ui.theme

import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            "Midnight · Ungu · Dua warna" to "Kapsul · Statis",
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
            MidnightAccent.VIOLET to "Midnight · Ungu · Satu warna",
            MidnightAccent.PINK to "Midnight · Pink · Satu warna",
            MidnightAccent.CYAN to "Midnight · Cyan · Satu warna",
            MidnightAccent.GREEN to "Midnight · Green · Satu warna"
        )
        expected.forEach { (accent, summary) ->
            assertEquals(
                summary to "Busur · Sweep",
                ThemeGaugeSummaryFormatter.format(ThemePackConfig.MidnightViolet(
                    dualTintMetrics = false,
                    midnightAccent = accent
                ))
            )
        }
    }

    @Test
    fun liveBasicPreviewCarriesDraftAppearanceAndAccentIntoThemeWrapperConfig() {
        val draft = ThemePackConfig.Basic(
            appearanceMode = BasicAppearanceMode.LIGHT,
            accent = BasicAccent.COBALT,
            gaugeModel = BatteryGaugeModel.WAVE,
            chargingAnimation = ChargingArcAnimation.COMET_HEAD
        )

        val preview = themePreviewConfig(draft) as ThemePackConfig.Basic

        assertEquals(BasicAppearanceMode.LIGHT, preview.appearanceMode)
        assertEquals(BasicAccent.COBALT, preview.accent)
        assertEquals(BatteryGaugeModel.WAVE, preview.gaugeModel)
        assertEquals(ChargingArcAnimation.COMET_HEAD, preview.chargingAnimation)
        assertEquals(basicAccentColor(BasicAccent.COBALT), basicAccentColor(preview.accent))
    }

    @Test
    fun themeGaugeDirtyStateTracksEveryPersistedConfigurationOption() {
        val saved = ThemePackConfig.MidnightViolet()
        assertFalse(hasThemeGaugeUnsavedChanges(saved, saved.copy()))
        assertTrue(hasThemeGaugeUnsavedChanges(saved.copy(gaugeModel = BatteryGaugeModel.WAVE), saved))
        assertTrue(hasThemeGaugeUnsavedChanges(saved.copy(chargingAnimation = ChargingArcAnimation.STATIC), saved))
        assertTrue(hasThemeGaugeUnsavedChanges(saved.copy(midnightAccent = MidnightAccent.PINK), saved))
        assertTrue(hasThemeGaugeUnsavedChanges(saved.copy(dualTintMetrics = false), saved))
        val basic = ThemePackConfig.Basic()
        assertTrue(hasThemeGaugeUnsavedChanges(basic.copy(accent = BasicAccent.COBALT), basic))
    }
}
