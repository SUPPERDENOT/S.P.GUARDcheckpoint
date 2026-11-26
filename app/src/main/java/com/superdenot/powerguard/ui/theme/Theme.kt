package com.superdenot.powerguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
        primary = Color(0xFF9D7BFF),
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
        primary = Color(0xFFF472B6),
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
        primary = Color(0xFF38BDF8),
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
        primary = Color(0xFF34D399),
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

internal fun midnightColorScheme(accent: MidnightAccent) = darkColorScheme(
    surfaceContainerLowest = midnightPalette(accent).lowest,
    surfaceContainerLow = midnightPalette(accent).low,
    surfaceContainer = midnightPalette(accent).container,
    surfaceContainerHigh = midnightPalette(accent).high,
    surfaceContainerHighest = midnightPalette(accent).highest,
    primary = midnightPalette(accent).primary,
    onPrimary = midnightPalette(accent).onPrimary,
    primaryContainer = midnightPalette(accent).primary.copy(alpha = 0.24f),
    onPrimaryContainer = MidnightOnSurface,
    secondary = EmeraldCharge,
    onSecondary = Color.White,
    tertiary = AmberThermal,
    error = CrimsonAnomaly,
    background = midnightPalette(accent).background,
    onBackground = MidnightOnSurface,
    surface = midnightPalette(accent).surface,
    onSurface = MidnightOnSurface,
    surfaceVariant = midnightPalette(accent).surface,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = midnightPalette(accent).outlineVariant,
    outlineVariant = midnightPalette(accent).outlineVariant
)
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
        is ThemePackConfig.MidnightViolet -> true
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
    }

    val metricTints = when (config) {
        is ThemePackConfig.MidnightViolet -> BatteryMetricTints(
            enabled = config.dualTintMetrics,
            cold = if (config.dualTintMetrics) MidnightColdMetric else Color.Transparent,
            warm = if (config.dualTintMetrics) MidnightWarmMetric else Color.Transparent
        )
        is ThemePackConfig.Basic -> BatteryMetricTints()
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
