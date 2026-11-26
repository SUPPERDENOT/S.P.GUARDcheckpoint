package com.superdenot.powerguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// All explicit color literals live in this theme package.
val EmeraldCharge = Color(0xFF39765B)
val CyanFastCharge = Color(0xFFD96B2B)
val AmberThermal = Color(0xFFA87524)
val CrimsonAnomaly = Color(0xFFB8473D)
val SlateSurface = Color(0xFFF4F0E7)
val SlateCard = Color(0xFFE9E3D8)
val InstrumentGraphite = Color(0xFF252923)
val StatusReadyGreen = Color(0xFF22C55E)
val StatusErrorRed = Color(0xFFEF4444)
val GaugePercentageOutline = Color(0xF0000000)

private val Paper = Color(0xFFFFFCF6)
private val SecondaryText = Color(0xFF62665F)
internal val BasicLightOutline = Color(0xFF928B80)
internal val BasicLightOutlineVariant = Color(0xFF8F887D)
internal val BasicLightOnSurfaceVariant = SecondaryText
internal val BasicLightSubSurface = SlateCard
private val TerracottaAccent = Color(0xFFD96B2B)
private val EmeraldAccent = Color(0xFF39765B)
private val CobaltAccent = Color(0xFF315D9B)
fun basicAccentSwatches(): List<Color> = listOf(TerracottaAccent, EmeraldAccent, CobaltAccent)
private val DarkBackground = Color(0xFF171916)
private val DarkSurface = Color(0xFF222720)
private val DarkText = Color(0xFFF4F0E7)
private val DarkMuted = Color(0xFFB5B9B0)
private data class MidnightPalette(
    val background: Color,
    val surface: Color,
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color
)

private val MidnightOnSurface = Color(0xFFE6EAF2)
private val MidnightOnSurfaceVariant = Color(0xFF9AA4B8)
val MidnightVioletAccent = Color(0xFF9D7BFF)
val MidnightPinkAccent = Color(0xFFF472B6)
val MidnightCyanAccent = Color(0xFF38BDF8)
val MidnightGreenAccent = Color(0xFF34D399)

private fun midnightPalette(accent: MidnightAccent): MidnightPalette = when (accent) {
    MidnightAccent.VIOLET -> MidnightPalette(
        background = Color(0xFF0E0F1A),
        surface = Color(0xFF161826),
        lowest = Color(0xFF0A0B14),
        low = Color(0xFF121420),
        container = Color(0xFF161826),
        high = Color(0xFF1D2032),
        highest = Color(0xFF24283E),
        outlineVariant = Color(0xFF2B2F47),
        primary = MidnightVioletAccent,
        onPrimary = Color(0xFF130D26)
    )
    MidnightAccent.PINK -> MidnightPalette(
        background = Color(0xFF140F14),
        surface = Color(0xFF1E171E),
        lowest = Color(0xFF0F0B0F),
        low = Color(0xFF191319),
        container = Color(0xFF1E171E),
        high = Color(0xFF271E27),
        highest = Color(0xFF312631),
        outlineVariant = Color(0xFF3A2C39),
        primary = MidnightPinkAccent,
        onPrimary = Color(0xFF240B18)
    )
    MidnightAccent.CYAN -> MidnightPalette(
        background = Color(0xFF0C1318),
        surface = Color(0xFF131E26),
        lowest = Color(0xFF080E12),
        low = Color(0xFF0F181F),
        container = Color(0xFF131E26),
        high = Color(0xFF192731),
        highest = Color(0xFF20313E),
        outlineVariant = Color(0xFF263947),
        primary = MidnightCyanAccent,
        onPrimary = Color(0xFF071B26)
    )
    MidnightAccent.GREEN -> MidnightPalette(
        background = Color(0xFF0C1412),
        surface = Color(0xFF131F1C),
        lowest = Color(0xFF080E0D),
        low = Color(0xFF0F1917),
        container = Color(0xFF131F1C),
        high = Color(0xFF192925),
        highest = Color(0xFF20342F),
        outlineVariant = Color(0xFF263B35),
        primary = MidnightGreenAccent,
        onPrimary = Color(0xFF062017)
    )
}
private val MidnightColdMetric = Color(0xFF102B3A)
private val MidnightWarmMetric = Color(0xFF2D1F27)

internal data class SurfaceContainerPalette(
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color
)

internal fun basicSurfaceContainerPalette(
    accent: BasicAccent,
    isDark: Boolean
): SurfaceContainerPalette = when (accent) {
    BasicAccent.TERRACOTTA -> if (isDark) SurfaceContainerPalette(
        Color(0xFF17130F), Color(0xFF211A15), Color(0xFF2B211B), Color(0xFF352922), Color(0xFF40332B)
    ) else SurfaceContainerPalette(
        Color(0xFFFFFDF9), Color(0xFFFBF5EE), Color(0xFFF4EDE4), Color(0xFFEDE4D9), Color(0xFFE5DACE)
    )
    BasicAccent.EMERALD -> if (isDark) SurfaceContainerPalette(
        Color(0xFF101710), Color(0xFF19221A), Color(0xFF222D23), Color(0xFF2C382D), Color(0xFF374437)
    ) else SurfaceContainerPalette(
        Color(0xFFFBFEFA), Color(0xFFF3F8F0), Color(0xFFEAF1E6), Color(0xFFE1E9DC), Color(0xFFD8E2D2)
    )
    BasicAccent.COBALT -> if (isDark) SurfaceContainerPalette(
        Color(0xFF111720), Color(0xFF1A2430), Color(0xFF243040), Color(0xFF2E3C4E), Color(0xFF39495D)
    ) else SurfaceContainerPalette(
        Color(0xFFFBFCFF), Color(0xFFF2F5FA), Color(0xFFE9EEF6), Color(0xFFE0E7F1), Color(0xFFD7E0EC)
    )
}

internal fun midnightSurfaceContainerPalette(accent: MidnightAccent): SurfaceContainerPalette {
    val palette = midnightPalette(accent)
    return SurfaceContainerPalette(
        lowest = palette.lowest,
        low = palette.low,
        container = palette.container,
        high = palette.high,
        highest = palette.highest
    )
}

internal fun midnightColorScheme(accent: MidnightAccent) : androidx.compose.material3.ColorScheme {
    val palette = midnightPalette(accent)
    return darkColorScheme(
        surfaceContainerLowest = palette.lowest,
        surfaceContainerLow = palette.low,
        surfaceContainer = palette.container,
        surfaceContainerHigh = palette.high,
        surfaceContainerHighest = palette.highest,
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primary.copy(alpha = 0.24f),
        onPrimaryContainer = MidnightOnSurface,
        secondary = EmeraldCharge,
        onSecondary = Color.White,
        tertiary = AmberThermal,
        error = CrimsonAnomaly,
        background = palette.background,
        onBackground = MidnightOnSurface,
        surface = palette.surface,
        onSurface = MidnightOnSurface,
        surfaceVariant = palette.surface,
        onSurfaceVariant = MidnightOnSurfaceVariant,
        outline = palette.outlineVariant,
        outlineVariant = palette.outlineVariant
    )
}

private data class StandalonePalette(
    val background: Color,
    val surface: Color,
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

private fun standaloneDarkColorScheme(palette: StandalonePalette): ColorScheme = darkColorScheme(
    surfaceContainerLowest = palette.lowest,
    surfaceContainerLow = palette.low,
    surfaceContainer = palette.container,
    surfaceContainerHigh = palette.high,
    surfaceContainerHighest = palette.highest,
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primary.copy(alpha = 0.24f),
    onPrimaryContainer = palette.onSurface,
    secondary = EmeraldCharge,
    onSecondary = Color.White,
    tertiary = AmberThermal,
    onTertiary = Color.White,
    error = CrimsonAnomaly,
    onError = Color.White,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onSurface,
    surfaceVariant = palette.high,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.outlineVariant,
    outlineVariant = palette.outlineVariant
)

private fun standaloneLightColorScheme(palette: StandalonePalette): ColorScheme = lightColorScheme(
    surfaceContainerLowest = palette.lowest,
    surfaceContainerLow = palette.low,
    surfaceContainer = palette.container,
    surfaceContainerHigh = palette.high,
    surfaceContainerHighest = palette.highest,
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primary.copy(alpha = 0.14f),
    onPrimaryContainer = palette.onBackground,
    secondary = EmeraldCharge,
    onSecondary = Color.White,
    tertiary = AmberThermal,
    onTertiary = Color.White,
    error = CrimsonAnomaly,
    onError = Color.White,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onSurface,
    surfaceVariant = palette.high,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.outlineVariant,
    outlineVariant = palette.outlineVariant
)

internal fun amoledPureColorScheme(accent: AmoledPureAccent): ColorScheme {
    val primary = when (accent) {
        AmoledPureAccent.AMBER_GOLD -> Color(0xFFFBBF24)
        AmoledPureAccent.CLEAN_WHITE -> Color(0xFFF8FAFC)
    }
    val onPrimary = when (accent) {
        AmoledPureAccent.AMBER_GOLD -> Color(0xFF1A1000)
        AmoledPureAccent.CLEAN_WHITE -> Color(0xFF0D0E10)
    }
    return standaloneDarkColorScheme(StandalonePalette(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        lowest = Color(0xFF000000),
        low = Color(0xFF08090B),
        container = Color(0xFF0D0E10),
        high = Color(0xFF14161A),
        highest = Color(0xFF1C1F24),
        outlineVariant = Color(0xFF22252A),
        primary = primary,
        onPrimary = onPrimary,
        onBackground = Color(0xFFF2F4F8),
        onSurface = Color(0xFFF2F4F8),
        onSurfaceVariant = Color(0xFF9CA3AF)
    ))
}

internal fun evCockpitColorScheme(accent: EvCockpitAccent): ColorScheme {
    val primary = when (accent) {
        EvCockpitAccent.ELECTRIC_SKY -> Color(0xFF38BDF8)
        EvCockpitAccent.VOLT_LIME -> Color(0xFFA3E635)
    }
    val onPrimary = when (accent) {
        EvCockpitAccent.ELECTRIC_SKY -> Color(0xFF061A26)
        EvCockpitAccent.VOLT_LIME -> Color(0xFF132202)
    }
    return standaloneDarkColorScheme(StandalonePalette(
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        lowest = Color(0xFF090E1A),
        low = Color(0xFF131C31),
        container = Color(0xFF1E293B),
        high = Color(0xFF263449),
        highest = Color(0xFF304059),
        outlineVariant = Color(0xFF334155),
        primary = primary,
        onPrimary = onPrimary,
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF94A3B8)
    ))
}

internal fun nordicFrostColorScheme(accent: NordicFrostAccent): ColorScheme {
    val primary = when (accent) {
        NordicFrostAccent.COBALT_STEEL -> Color(0xFF2563EB)
        NordicFrostAccent.TEAL -> Color(0xFF0D9488)
    }
    return standaloneLightColorScheme(StandalonePalette(
        background = Color(0xFFF1F5F9),
        surface = Color(0xFFFFFFFF),
        lowest = Color(0xFFFFFFFF),
        low = Color(0xFFF8FAFC),
        container = Color(0xFFFFFFFF),
        high = Color(0xFFE8EEF5),
        highest = Color(0xFFDCE4EE),
        outlineVariant = Color(0xFFCBD5E1),
        primary = primary,
        onPrimary = Color.White,
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF475569)
    ))
}

internal fun monochromeColorScheme(variant: MonochromeVariant): ColorScheme = when (variant) {
    MonochromeVariant.DARK_CHARCOAL -> standaloneDarkColorScheme(StandalonePalette(
        background = Color(0xFF141414),
        surface = Color(0xFF1C1C1C),
        lowest = Color(0xFF0E0E0E),
        low = Color(0xFF181818),
        container = Color(0xFF1C1C1C),
        high = Color(0xFF252525),
        highest = Color(0xFF2E2E2E),
        outlineVariant = Color(0xFF383838),
        primary = Color(0xFFE5E5E5),
        onPrimary = Color(0xFF141414),
        onBackground = Color(0xFFF5F5F5),
        onSurface = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFFA3A3A3)
    ))
    MonochromeVariant.LIGHT_PAPER -> standaloneLightColorScheme(StandalonePalette(
        background = Color(0xFFF5F5F4),
        surface = Color(0xFFFFFFFF),
        lowest = Color(0xFFFFFFFF),
        low = Color(0xFFFAFAF9),
        container = Color(0xFFFFFFFF),
        high = Color(0xFFECECEB),
        highest = Color(0xFFE2E2E0),
        outlineVariant = Color(0xFFD6D3D1),
        primary = Color(0xFF1C1917),
        onPrimary = Color(0xFFFAFAF9),
        onBackground = Color(0xFF1C1917),
        onSurface = Color(0xFF1C1917),
        onSurfaceVariant = Color(0xFF57534E)
    ))
}

fun amoledPureAccentSwatches(): List<Color> = listOf(Color(0xFFFBBF24), Color(0xFFF8FAFC))
fun evCockpitAccentSwatches(): List<Color> = listOf(Color(0xFF38BDF8), Color(0xFFA3E635))
fun nordicFrostAccentSwatches(): List<Color> = listOf(Color(0xFF2563EB), Color(0xFF0D9488))
fun monochromeSwatches(): List<Color> = listOf(Color(0xFF141414), Color(0xFFF5F5F4))

private val BasicDarkSurfaceVariant = Color(0xFF2B3029)
private val BasicDarkOutline = Color(0xFF62665F)
private val BasicDarkOutlineVariant = Color(0xFF454B42)
private val TerracottaContainer = Color(0xFFF5DFD1)
private val TerracottaOnContainer = Color(0xFF51250F)
private val EmeraldContainer = Color(0xFFDCE9DF)
private val EmeraldOnContainer = Color(0xFF173D2B)
private val ErrorContainer = Color(0xFFF5DEDA)
private val ErrorOnContainer = Color(0xFF5B211B)

data class BatterySemanticColors(
    val normal: Color,
    val charging: Color,
    val warm: Color,
    val criticalThermal: Color
)

data class BatteryMetricTints(
    val enabled: Boolean = false,
    val cold: Color = Color.Transparent,
    val warm: Color = Color.Transparent
)

val LocalBatterySemanticColors = staticCompositionLocalOf {
    BatterySemanticColors(
        normal = TerracottaAccent,
        charging = EmeraldCharge,
        warm = AmberThermal,
        criticalThermal = CrimsonAnomaly
    )
}

val LocalBatteryMetricTints = staticCompositionLocalOf { BatteryMetricTints() }

fun basicAccentColor(accent: BasicAccent): Color = when (accent) {
    BasicAccent.TERRACOTTA -> TerracottaAccent
    BasicAccent.EMERALD -> EmeraldAccent
    BasicAccent.COBALT -> CobaltAccent
}

fun midnightAccentColor(accent: MidnightAccent): Color = when (accent) {
    MidnightAccent.VIOLET -> MidnightVioletAccent
    MidnightAccent.PINK -> MidnightPinkAccent
    MidnightAccent.CYAN -> MidnightCyanAccent
    MidnightAccent.GREEN -> MidnightGreenAccent
}

fun midnightAccentSwatches(): List<Color> = listOf(
    MidnightVioletAccent, MidnightPinkAccent, MidnightCyanAccent, MidnightGreenAccent
)

@Composable
fun PowerGuardTheme(
    config: ThemePackConfig = ThemePackConfig.Basic(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (config) {
        is ThemePackConfig.Basic -> when (config.appearanceMode) {
            BasicAppearanceMode.SYSTEM -> systemDark
            BasicAppearanceMode.LIGHT -> false
            BasicAppearanceMode.DARK -> true
        }
        is ThemePackConfig.MidnightViolet,
        is ThemePackConfig.AmoledPure,
        is ThemePackConfig.EvCockpit -> true
        is ThemePackConfig.NordicFrost -> false
        is ThemePackConfig.Monochrome -> config.variant == MonochromeVariant.DARK_CHARCOAL
    }

    val colorScheme = when (config) {
        is ThemePackConfig.Basic -> {
            val accent = basicAccentColor(config.accent)
            val containers = basicSurfaceContainerPalette(config.accent, isDark)
            if (isDark) {
                darkColorScheme(
                    surfaceContainerLowest = containers.lowest,
                    surfaceContainerLow = containers.low,
                    surfaceContainer = containers.container,
                    surfaceContainerHigh = containers.high,
                    surfaceContainerHighest = containers.highest,
                    primary = accent,
                    onPrimary = Color.White,
                    secondary = EmeraldCharge,
                    onSecondary = Color.White,
                    tertiary = AmberThermal,
                    error = CrimsonAnomaly,
                    background = DarkBackground,
                    onBackground = DarkText,
                    surface = DarkSurface,
                    onSurface = DarkText,
                    surfaceVariant = BasicDarkSurfaceVariant,
                    onSurfaceVariant = DarkMuted,
                    outline = BasicDarkOutline,
                    outlineVariant = BasicDarkOutlineVariant
                )
            } else {
                lightColorScheme(
                    surfaceContainerLowest = containers.lowest,
                    surfaceContainerLow = containers.low,
                    surfaceContainer = containers.container,
                    surfaceContainerHigh = containers.high,
                    surfaceContainerHighest = containers.highest,
                    primary = accent,
                    onPrimary = Color.White,
                    primaryContainer = accent.copy(alpha = 0.16f),
                    onPrimaryContainer = InstrumentGraphite,
                    secondary = EmeraldCharge,
                    onSecondary = Color.White,
                    secondaryContainer = EmeraldContainer,
                    onSecondaryContainer = EmeraldOnContainer,
                    tertiary = AmberThermal,
                    onTertiary = Color.White,
                    error = CrimsonAnomaly,
                    onError = Color.White,
                    errorContainer = ErrorContainer,
                    onErrorContainer = ErrorOnContainer,
                    background = SlateSurface,
                    onBackground = InstrumentGraphite,
                    surface = Paper,
                    onSurface = InstrumentGraphite,
                    surfaceVariant = BasicLightSubSurface,
                    onSurfaceVariant = BasicLightOnSurfaceVariant,
                    outline = BasicLightOutline,
                    outlineVariant = BasicLightOutlineVariant
                )
            }
        }
        is ThemePackConfig.MidnightViolet -> midnightColorScheme(config.midnightAccent)
        is ThemePackConfig.AmoledPure -> amoledPureColorScheme(config.accent)
        is ThemePackConfig.EvCockpit -> evCockpitColorScheme(config.accent)
        is ThemePackConfig.NordicFrost -> nordicFrostColorScheme(config.accent)
        is ThemePackConfig.Monochrome -> monochromeColorScheme(config.variant)
    }

    val metricTints = when (config) {
        is ThemePackConfig.MidnightViolet -> BatteryMetricTints(
            enabled = config.dualTintMetrics,
            cold = if (config.dualTintMetrics) MidnightColdMetric else Color.Transparent,
            warm = if (config.dualTintMetrics) MidnightWarmMetric else Color.Transparent
        )
        is ThemePackConfig.Basic,
        is ThemePackConfig.AmoledPure,
        is ThemePackConfig.EvCockpit,
        is ThemePackConfig.NordicFrost,
        is ThemePackConfig.Monochrome -> BatteryMetricTints()
    }

    CompositionLocalProvider(
        LocalBatterySemanticColors provides BatterySemanticColors(
            normal = colorScheme.primary,
            charging = EmeraldCharge,
            warm = AmberThermal,
            criticalThermal = CrimsonAnomaly
        ),
        LocalBatteryMetricTints provides metricTints
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
