package com.superdenot.powerguard.data

import android.content.Context
import android.content.SharedPreferences
import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.BasicAppearanceMode
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
        }
        editor.apply()
    }

    companion object {
        const val PREFERENCES_NAME = "spg_theme_preferences"
        const val PACK_BASIC = "basic"
        const val PACK_MIDNIGHT_VIOLET = "midnight_violet"
        const val KEY_THEME_PACK = "theme_pack"
        const val KEY_APPEARANCE_MODE = "appearance_mode"
        const val KEY_BASIC_ACCENT = "basic_accent"
        const val KEY_DUAL_TINT = "dual_tint_metrics"
        const val KEY_GAUGE_MODEL = "battery_gauge_model"
        const val KEY_CHARGING_ANIMATION = "charging_arc_animation"
    }
}
