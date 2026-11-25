package com.superdenot.powerguard.ui.theme

import com.superdenot.powerguard.ui.components.BatteryGaugeModel
import com.superdenot.powerguard.ui.components.ChargingArcAnimation

enum class BasicAppearanceMode(val key: String, val label: String) {
    SYSTEM("system", "Sistem"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap");

    companion object {
        fun fromKey(raw: String?): BasicAppearanceMode =
            values().firstOrNull { it.key == raw } ?: SYSTEM
    }
}

enum class BasicAccent(val key: String, val label: String) {
    TERRACOTTA("terracotta", "Terracotta"),
    EMERALD("emerald", "Emerald"),
    COBALT("cobalt", "Cobalt");

    companion object {
        fun fromKey(raw: String?): BasicAccent =
            values().firstOrNull { it.key == raw } ?: TERRACOTTA
    }
}

enum class MidnightAccent(val key: String, val label: String) {
    VIOLET("ungu", "Ungu"),
    PINK("pink", "Pink"),
    CYAN("cyan", "Cyan"),
    GREEN("green", "Green");

    companion object {
        fun fromKey(raw: String?): MidnightAccent =
            values().firstOrNull { it.key == raw } ?: VIOLET
    }
}

sealed interface ThemePackConfig {
    val gaugeModel: BatteryGaugeModel
    val chargingAnimation: ChargingArcAnimation

    data class Basic(
        val appearanceMode: BasicAppearanceMode = BasicAppearanceMode.SYSTEM,
        val accent: BasicAccent = BasicAccent.TERRACOTTA,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig

    data class MidnightViolet(
        val dualTintMetrics: Boolean = true,
        val midnightAccent: MidnightAccent = MidnightAccent.VIOLET,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig
}

object ThemeGaugeSummaryFormatter {
    fun format(config: ThemePackConfig): Pair<String, String> {
        val themeLine = when (config) {
            is ThemePackConfig.Basic ->
                "Basic · ${config.appearanceMode.label} · ${config.accent.label}"
            is ThemePackConfig.MidnightViolet ->
                "Midnight Violet · ${config.midnightAccent.label} · ${if (config.dualTintMetrics) "Dua warna" else "Satu warna"}"
        }
        val gaugeLine = "${config.gaugeModel.label} · ${config.chargingAnimation.label}"
        return themeLine to gaugeLine
    }
}
