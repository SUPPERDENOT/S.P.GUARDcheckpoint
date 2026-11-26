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

enum class AmoledPureAccent(val key: String, val label: String) {
    AMBER_GOLD("amber_gold", "Amber Emas"),
    CLEAN_WHITE("clean_white", "Putih Bersih");

    companion object {
        fun fromKey(raw: String?): AmoledPureAccent =
            values().firstOrNull { it.key == raw } ?: AMBER_GOLD
    }
}

enum class EvCockpitAccent(val key: String, val label: String) {
    ELECTRIC_SKY("electric_sky", "Electric Sky"),
    VOLT_LIME("volt_lime", "Volt Lime");

    companion object {
        fun fromKey(raw: String?): EvCockpitAccent =
            values().firstOrNull { it.key == raw } ?: ELECTRIC_SKY
    }
}

enum class NordicFrostAccent(val key: String, val label: String) {
    COBALT_STEEL("cobalt_steel", "Cobalt Steel"),
    TEAL("teal", "Teal");

    companion object {
        fun fromKey(raw: String?): NordicFrostAccent =
            values().firstOrNull { it.key == raw } ?: COBALT_STEEL
    }
}

enum class MonochromeVariant(val key: String, val label: String) {
    DARK_CHARCOAL("dark_charcoal", "Arang Gelap"),
    LIGHT_PAPER("light_paper", "Kertas Terang");

    companion object {
        fun fromKey(raw: String?): MonochromeVariant =
            values().firstOrNull { it.key == raw } ?: DARK_CHARCOAL
    }
}

enum class ThemePackKind {
    BASIC,
    MIDNIGHT,
    AMOLED_PURE,
    EV_COCKPIT,
    NORDIC_FROST,
    MONOCHROME
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

    data class AmoledPure(
        val accent: AmoledPureAccent = AmoledPureAccent.AMBER_GOLD,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig

    data class EvCockpit(
        val accent: EvCockpitAccent = EvCockpitAccent.ELECTRIC_SKY,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig

    data class NordicFrost(
        val accent: NordicFrostAccent = NordicFrostAccent.COBALT_STEEL,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig

    data class Monochrome(
        val variant: MonochromeVariant = MonochromeVariant.DARK_CHARCOAL,
        override val gaugeModel: BatteryGaugeModel = BatteryGaugeModel.ARC,
        override val chargingAnimation: ChargingArcAnimation = ChargingArcAnimation.ENERGY_SWEEP
    ) : ThemePackConfig
}

/** Switches package while retaining the user's independent gauge and animation selections. */
fun ThemePackConfig.withPack(kind: ThemePackKind): ThemePackConfig {
    val gauge = gaugeModel
    val animation = chargingAnimation
    return when (kind) {
        ThemePackKind.BASIC -> if (this is ThemePackConfig.Basic) this else ThemePackConfig.Basic(
            gaugeModel = gauge, chargingAnimation = animation
        )
        ThemePackKind.MIDNIGHT -> if (this is ThemePackConfig.MidnightViolet) this else ThemePackConfig.MidnightViolet(
            gaugeModel = gauge, chargingAnimation = animation
        )
        ThemePackKind.AMOLED_PURE -> if (this is ThemePackConfig.AmoledPure) this else ThemePackConfig.AmoledPure(
            gaugeModel = gauge, chargingAnimation = animation
        )
        ThemePackKind.EV_COCKPIT -> if (this is ThemePackConfig.EvCockpit) this else ThemePackConfig.EvCockpit(
            gaugeModel = gauge, chargingAnimation = animation
        )
        ThemePackKind.NORDIC_FROST -> if (this is ThemePackConfig.NordicFrost) this else ThemePackConfig.NordicFrost(
            gaugeModel = gauge, chargingAnimation = animation
        )
        ThemePackKind.MONOCHROME -> if (this is ThemePackConfig.Monochrome) this else ThemePackConfig.Monochrome(
            gaugeModel = gauge, chargingAnimation = animation
        )
    }
}

fun themePreviewConfig(config: ThemePackConfig): ThemePackConfig = when (config) {
    is ThemePackConfig.Basic -> config.copy()
    is ThemePackConfig.MidnightViolet -> config.copy()
    is ThemePackConfig.AmoledPure -> config.copy()
    is ThemePackConfig.EvCockpit -> config.copy()
    is ThemePackConfig.NordicFrost -> config.copy()
    is ThemePackConfig.Monochrome -> config.copy()
}

fun hasThemeGaugeUnsavedChanges(draft: ThemePackConfig, saved: ThemePackConfig): Boolean = draft != saved

object ThemeGaugeSummaryFormatter {
    fun format(config: ThemePackConfig): Pair<String, String> {
        val themeLine = when (config) {
            is ThemePackConfig.Basic ->
                "Basic · ${config.appearanceMode.label} · ${config.accent.label}"
            is ThemePackConfig.MidnightViolet ->
                "Midnight · ${config.midnightAccent.label} · ${if (config.dualTintMetrics) "Dua warna" else "Satu warna"}"
            is ThemePackConfig.AmoledPure ->
                "AMOLED Pure · ${config.accent.label}"
            is ThemePackConfig.EvCockpit ->
                "EV Cockpit · ${config.accent.label}"
            is ThemePackConfig.NordicFrost ->
                "Nordic Frost · ${config.accent.label}"
            is ThemePackConfig.Monochrome ->
                "Monochrome · ${config.variant.label}"
        }
        val gaugeLine = "${config.gaugeModel.label} · ${config.chargingAnimation.label}"
        return themeLine to gaugeLine
    }
}
