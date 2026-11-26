package com.superdenot.powerguard.data

import android.content.Context
import android.content.SharedPreferences
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

class ThemePreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): ThemePackConfig {
        val gauge = BatteryGaugeModel.fromKey(preferences.getString(KEY_GAUGE_MODEL, null))
        val animation = ChargingArcAnimation.fromKey(preferences.getString(KEY_CHARGING_ANIMATION, null))
        return when (preferences.getString(KEY_THEME_PACK, PACK_BASIC)) {
            PACK_MIDNIGHT_VIOLET -> ThemePackConfig.MidnightViolet(
                dualTintMetrics = preferences.getBoolean(KEY_DUAL_TINT, true),
                midnightAccent = MidnightAccent.fromKey(preferences.getString(KEY_MIDNIGHT_ACCENT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
            PACK_AMOLED_PURE -> ThemePackConfig.AmoledPure(
                accent = AmoledPureAccent.fromKey(preferences.getString(KEY_AMOLED_ACCENT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
            PACK_EV_COCKPIT -> ThemePackConfig.EvCockpit(
                accent = EvCockpitAccent.fromKey(preferences.getString(KEY_EV_COCKPIT_ACCENT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
            PACK_NORDIC_FROST -> ThemePackConfig.NordicFrost(
                accent = NordicFrostAccent.fromKey(preferences.getString(KEY_NORDIC_FROST_ACCENT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
            PACK_MONOCHROME -> ThemePackConfig.Monochrome(
                variant = MonochromeVariant.fromKey(preferences.getString(KEY_MONOCHROME_VARIANT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
            else -> ThemePackConfig.Basic(
                appearanceMode = BasicAppearanceMode.fromKey(preferences.getString(KEY_APPEARANCE_MODE, null)),
                accent = BasicAccent.fromKey(preferences.getString(KEY_BASIC_ACCENT, null)),
                gaugeModel = gauge,
                chargingAnimation = animation
            )
        }
    }

    fun save(config: ThemePackConfig) {
        val editor = preferences.edit()
            .putString(KEY_GAUGE_MODEL, config.gaugeModel.key)
            .putString(KEY_CHARGING_ANIMATION, config.chargingAnimation.key)
        when (config) {
            is ThemePackConfig.Basic -> editor
                .putString(KEY_THEME_PACK, PACK_BASIC)
                .putString(KEY_APPEARANCE_MODE, config.appearanceMode.key)
                .putString(KEY_BASIC_ACCENT, config.accent.key)
            is ThemePackConfig.MidnightViolet -> editor
                .putString(KEY_THEME_PACK, PACK_MIDNIGHT_VIOLET)
                .putBoolean(KEY_DUAL_TINT, config.dualTintMetrics)
                .putString(KEY_MIDNIGHT_ACCENT, config.midnightAccent.key)
            is ThemePackConfig.AmoledPure -> editor
                .putString(KEY_THEME_PACK, PACK_AMOLED_PURE)
                .putString(KEY_AMOLED_ACCENT, config.accent.key)
            is ThemePackConfig.EvCockpit -> editor
                .putString(KEY_THEME_PACK, PACK_EV_COCKPIT)
                .putString(KEY_EV_COCKPIT_ACCENT, config.accent.key)
            is ThemePackConfig.NordicFrost -> editor
                .putString(KEY_THEME_PACK, PACK_NORDIC_FROST)
                .putString(KEY_NORDIC_FROST_ACCENT, config.accent.key)
            is ThemePackConfig.Monochrome -> editor
                .putString(KEY_THEME_PACK, PACK_MONOCHROME)
                .putString(KEY_MONOCHROME_VARIANT, config.variant.key)
        }
        editor.apply()
    }

    companion object {
        const val PREFERENCES_NAME = "spg_theme_preferences"
        const val PACK_BASIC = "basic"
        const val PACK_MIDNIGHT_VIOLET = "midnight_violet"
        const val PACK_AMOLED_PURE = "amoled_pure"
        const val PACK_EV_COCKPIT = "ev_cockpit"
        const val PACK_NORDIC_FROST = "nordic_frost"
        const val PACK_MONOCHROME = "monochrome"
        const val KEY_THEME_PACK = "theme_pack"
        const val KEY_APPEARANCE_MODE = "appearance_mode"
        const val KEY_BASIC_ACCENT = "basic_accent"
        const val KEY_DUAL_TINT = "dual_tint_metrics"
        const val KEY_MIDNIGHT_ACCENT = "midnight_accent"
        const val KEY_AMOLED_ACCENT = "amoled_accent"
        const val KEY_EV_COCKPIT_ACCENT = "ev_cockpit_accent"
        const val KEY_NORDIC_FROST_ACCENT = "nordic_frost_accent"
        const val KEY_MONOCHROME_VARIANT = "monochrome_variant"
        const val KEY_GAUGE_MODEL = "battery_gauge_model"
        const val KEY_CHARGING_ANIMATION = "charging_arc_animation"
    }
}
