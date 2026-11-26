package com.superdenot.powerguard.data

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import com.superdenot.powerguard.ui.theme.AmoledPureAccent
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.BasicAppearanceMode
import com.superdenot.powerguard.ui.theme.EvCockpitAccent
import com.superdenot.powerguard.ui.theme.MidnightAccent
import com.superdenot.powerguard.ui.theme.MonochromeVariant
import com.superdenot.powerguard.ui.theme.NordicFrostAccent
import com.superdenot.powerguard.ui.theme.ThemePackConfig
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class ThemePreferencesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(ThemePreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun unknownKeysFallBackWithoutEnumValueOfCrash() {
        val preferences = context.getSharedPreferences(ThemePreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString(ThemePreferences.KEY_THEME_PACK, "future_pack")
            .putString(ThemePreferences.KEY_APPEARANCE_MODE, "unknown_mode")
            .putString(ThemePreferences.KEY_BASIC_ACCENT, "unknown_accent")
            .putString(ThemePreferences.KEY_GAUGE_MODEL, "unknown_gauge")
            .putString(ThemePreferences.KEY_CHARGING_ANIMATION, "unknown_animation")
            .commit()

        assertEquals(
            ThemePackConfig.Basic(
                appearanceMode = BasicAppearanceMode.SYSTEM,
                accent = BasicAccent.TERRACOTTA,
                gaugeModel = BatteryGaugeModel.ARC,
                chargingAnimation = ChargingArcAnimation.ENERGY_SWEEP
            ),
            ThemePreferences(context).read()
        )
    }

    @Test
    fun persistsBasicAppearanceAccentAndGaugePreferences() {
        val expected = ThemePackConfig.Basic(
            appearanceMode = BasicAppearanceMode.DARK,
            accent = BasicAccent.COBALT,
            gaugeModel = BatteryGaugeModel.SEGMENTED,
            chargingAnimation = ChargingArcAnimation.SEGMENTED_FLOW
        )
        ThemePreferences(context).save(expected)
        assertEquals(expected, ThemePreferences(context).read())
    }

    @Test
    fun persistsMidnightVioletAndAllGaugePreferences() {
        val expected = ThemePackConfig.MidnightViolet(
            dualTintMetrics = false,
            midnightAccent = MidnightAccent.GREEN,
            gaugeModel = BatteryGaugeModel.WAVE,
            chargingAnimation = ChargingArcAnimation.COMET_HEAD
        )
        ThemePreferences(context).save(expected)
        assertEquals(expected, ThemePreferences(context).read())
    }

    @Test
    fun persistsEveryStandaloneThemePackVariantAndGaugePreference() {
        val preferences = ThemePreferences(context)
        val configs = listOf(
            ThemePackConfig.AmoledPure(AmoledPureAccent.AMBER_GOLD, BatteryGaugeModel.ARC, ChargingArcAnimation.ENERGY_SWEEP),
            ThemePackConfig.AmoledPure(AmoledPureAccent.CLEAN_WHITE, BatteryGaugeModel.CAPSULE, ChargingArcAnimation.STATIC),
            ThemePackConfig.EvCockpit(EvCockpitAccent.ELECTRIC_SKY, BatteryGaugeModel.BLOCK, ChargingArcAnimation.SEGMENTED_FLOW),
            ThemePackConfig.EvCockpit(EvCockpitAccent.VOLT_LIME, BatteryGaugeModel.WAVE, ChargingArcAnimation.COMET_HEAD),
            ThemePackConfig.NordicFrost(NordicFrostAccent.COBALT_STEEL, BatteryGaugeModel.ARC, ChargingArcAnimation.STATIC),
            ThemePackConfig.NordicFrost(NordicFrostAccent.TEAL, BatteryGaugeModel.BLOCK, ChargingArcAnimation.ENERGY_SWEEP),
            ThemePackConfig.Monochrome(MonochromeVariant.DARK_CHARCOAL, BatteryGaugeModel.CAPSULE, ChargingArcAnimation.COMET_HEAD),
            ThemePackConfig.Monochrome(MonochromeVariant.LIGHT_PAPER, BatteryGaugeModel.WAVE, ChargingArcAnimation.SEGMENTED_FLOW)
        )

        configs.forEach { expected ->
            preferences.save(expected)
            assertEquals(expected, preferences.read())
        }
    }

    @Test
    fun standaloneAccentAndVariantParsersUseStableKeysAndSafeDefaults() {
        assertEquals(AmoledPureAccent.AMBER_GOLD, AmoledPureAccent.fromKey("amber_gold"))
        assertEquals(AmoledPureAccent.CLEAN_WHITE, AmoledPureAccent.fromKey("clean_white"))
        assertEquals(AmoledPureAccent.AMBER_GOLD, AmoledPureAccent.fromKey("unknown"))
        assertEquals(EvCockpitAccent.ELECTRIC_SKY, EvCockpitAccent.fromKey("electric_sky"))
        assertEquals(EvCockpitAccent.VOLT_LIME, EvCockpitAccent.fromKey("volt_lime"))
        assertEquals(NordicFrostAccent.COBALT_STEEL, NordicFrostAccent.fromKey("cobalt_steel"))
        assertEquals(NordicFrostAccent.TEAL, NordicFrostAccent.fromKey("teal"))
        assertEquals(MonochromeVariant.DARK_CHARCOAL, MonochromeVariant.fromKey("dark_charcoal"))
        assertEquals(MonochromeVariant.LIGHT_PAPER, MonochromeVariant.fromKey("light_paper"))
    }

    @Test
    fun midnightAccentParserSupportsAllKeysAndFallsBackToViolet() {
        assertEquals(MidnightAccent.VIOLET, MidnightAccent.fromKey("ungu"))
        assertEquals(MidnightAccent.PINK, MidnightAccent.fromKey("pink"))
        assertEquals(MidnightAccent.CYAN, MidnightAccent.fromKey("cyan"))
        assertEquals(MidnightAccent.GREEN, MidnightAccent.fromKey("green"))
        assertEquals(MidnightAccent.VIOLET, MidnightAccent.fromKey(null))
        assertEquals(MidnightAccent.VIOLET, MidnightAccent.fromKey("future-accent"))
    }

    @Test
    fun safeEnumParsersUseDocumentedDefaults() {
        assertEquals(BasicAppearanceMode.SYSTEM, BasicAppearanceMode.fromKey(null))
        assertEquals(BasicAccent.TERRACOTTA, BasicAccent.fromKey("not-a-color"))
        assertEquals(BatteryGaugeModel.ARC, BatteryGaugeModel.fromKey("not-a-model"))
        assertEquals(ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.fromKey(null))
    }
}
