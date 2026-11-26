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
    fun formatsAllSixStandaloneThemePackNamesAndSelectedVariants() {
        val expected = listOf(
            ThemePackConfig.AmoledPure(AmoledPureAccent.CLEAN_WHITE) to "AMOLED Pure · Putih Bersih",
            ThemePackConfig.EvCockpit(EvCockpitAccent.VOLT_LIME) to "EV Cockpit · Volt Lime",
            ThemePackConfig.NordicFrost(NordicFrostAccent.TEAL) to "Nordic Frost · Teal",
            ThemePackConfig.Monochrome(MonochromeVariant.LIGHT_PAPER) to "Monochrome · Kertas Terang"
        )
        expected.forEach { (config, summary) ->
            assertEquals(summary to "Busur · Sweep", ThemeGaugeSummaryFormatter.format(config))
        }
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
    fun livePreviewPreservesEveryThemeVariantAndGaugeSelection() {
        val configs = listOf(
            ThemePackConfig.Basic(BasicAppearanceMode.LIGHT, BasicAccent.COBALT, BatteryGaugeModel.WAVE, ChargingArcAnimation.COMET_HEAD),
            ThemePackConfig.MidnightViolet(false, MidnightAccent.PINK, BatteryGaugeModel.CAPSULE, ChargingArcAnimation.STATIC),
            ThemePackConfig.AmoledPure(AmoledPureAccent.CLEAN_WHITE, BatteryGaugeModel.BLOCK, ChargingArcAnimation.SEGMENTED_FLOW),
            ThemePackConfig.EvCockpit(EvCockpitAccent.VOLT_LIME, BatteryGaugeModel.WAVE, ChargingArcAnimation.COMET_HEAD),
            ThemePackConfig.NordicFrost(NordicFrostAccent.TEAL, BatteryGaugeModel.CAPSULE, ChargingArcAnimation.STATIC),
            ThemePackConfig.Monochrome(MonochromeVariant.LIGHT_PAPER, BatteryGaugeModel.BLOCK, ChargingArcAnimation.ENERGY_SWEEP)
        )
        configs.forEach { config ->
            assertEquals(config, themePreviewConfig(config))
        }
    }

    @Test
    fun switchingThemePacksRetainsGaugeAndAnimationSettings() {
        val source = ThemePackConfig.MidnightViolet(
            gaugeModel = BatteryGaugeModel.WAVE,
            chargingAnimation = ChargingArcAnimation.COMET_HEAD
        )
        val targets = ThemePackKind.values().map(source::withPack)
        targets.forEach { target ->
            assertEquals(BatteryGaugeModel.WAVE, target.gaugeModel)
            assertEquals(ChargingArcAnimation.COMET_HEAD, target.chargingAnimation)
        }
        assertEquals(6, targets.map { it::class }.toSet().size)
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
        val amoled = ThemePackConfig.AmoledPure()
        assertTrue(hasThemeGaugeUnsavedChanges(amoled.copy(accent = AmoledPureAccent.CLEAN_WHITE), amoled))
    }
}
