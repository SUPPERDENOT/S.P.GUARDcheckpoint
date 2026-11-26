package com.superdenot.powerguard.data

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.AmoledPureAccent
import com.superdenot.powerguard.ui.theme.EvCockpitAccent
import com.superdenot.powerguard.ui.theme.NordicFrostAccent
import com.superdenot.powerguard.ui.theme.BasicAppearanceMode
import com.superdenot.powerguard.ui.theme.MidnightAccent
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
            gaugeModel = BatteryGaugeModel.WAVE,
            chargingAnimation = ChargingArcAnimation.COMET_HEAD
        )

        ThemePreferences(context).save(expected)

        assertEquals(expected, ThemePreferences(context).read())
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
    fun persistsEveryNonDefaultMidnightAccent() {
        listOf(MidnightAccent.PINK, MidnightAccent.CYAN, MidnightAccent.GREEN).forEach { accent ->
            val expected = ThemePackConfig.MidnightViolet(midnightAccent = accent)
            ThemePreferences(context).save(expected)
            assertEquals(expected, ThemePreferences(context).read())
        }
    }

    @Test
    fun persistsEveryNewThemePackAndItsAccentAndGaugeChoices() {
        val configs = listOf(
            ThemePackConfig.AmoledPure(AmoledPureAccent.CLEAN_WHITE, BatteryGaugeModel.WAVE, ChargingArcAnimation.COMET_HEAD),
            ThemePackConfig.EvCockpit(EvCockpitAccent.VOLT_LIME, BatteryGaugeModel.CAPSULE, ChargingArcAnimation.SEGMENTED_FLOW),
            ThemePackConfig.NordicFrost(NordicFrostAccent.TEAL, BatteryGaugeModel.SEGMENTED, ChargingArcAnimation.STATIC),
            ThemePackConfig.Monochrome(BasicAppearanceMode.DARK, BatteryGaugeModel.ARC, ChargingArcAnimation.ENERGY_SWEEP)
        )
        configs.forEach { expected ->
            ThemePreferences(context).save(expected)
            assertEquals(expected, ThemePreferences(context).read())
        }
    }

    @Test
    fun unknownNewThemeAccentKeysFallBackToPackageDefaults() {
        val preferences = context.getSharedPreferences(ThemePreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themePreferences = ThemePreferences(context)
        val cases = listOf(
            Triple(ThemePreferences.PACK_AMOLED_PURE, ThemePreferences.KEY_AMOLED_ACCENT, ThemePackConfig.AmoledPure()),
            Triple(ThemePreferences.PACK_EV_COCKPIT, ThemePreferences.KEY_EV_ACCENT, ThemePackConfig.EvCockpit()),
            Triple(ThemePreferences.PACK_NORDIC_FROST, ThemePreferences.KEY_NORDIC_ACCENT, ThemePackConfig.NordicFrost())
        )
        cases.forEach { (pack, key, expected) ->
            preferences.edit().putString(ThemePreferences.KEY_THEME_PACK, pack).putString(key, "future-accent").commit()
            assertEquals(expected, themePreferences.read())
        }
    }

    @Test
    fun monochromeAppearanceRoundTripsAndUnknownPackFallsBackToBasic() {
        val expected = ThemePackConfig.Monochrome(appearanceMode = BasicAppearanceMode.LIGHT)
        ThemePreferences(context).save(expected)
        assertEquals(expected, ThemePreferences(context).read())
        context.getSharedPreferences(ThemePreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().putString(ThemePreferences.KEY_THEME_PACK, "not_a_real_theme").commit()
        assertEquals(ThemePackConfig.Basic(), ThemePreferences(context).read())
    }

    @Test
    fun newAccentParsersUseSafePackageDefaults() {
        assertEquals(AmoledPureAccent.AMBER_GOLD, AmoledPureAccent.fromKey("future"))
        assertEquals(EvCockpitAccent.ELECTRIC_SKY, EvCockpitAccent.fromKey(null))
        assertEquals(NordicFrostAccent.COBALT_STEEL, NordicFrostAccent.fromKey("future"))
    }

    @Test
    fun safeEnumParsersUseDocumentedDefaults() {
        assertEquals(BasicAppearanceMode.SYSTEM, BasicAppearanceMode.fromKey(null))
        assertEquals(BasicAccent.TERRACOTTA, BasicAccent.fromKey("not-a-color"))
        assertEquals(BatteryGaugeModel.ARC, BatteryGaugeModel.fromKey("not-a-model"))
        assertEquals(ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.fromKey(null))
    }
}
