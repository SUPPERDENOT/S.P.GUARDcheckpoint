package com.superdenot.powerguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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
private data class MidnightPalette(val background: Color, val surface: Color, val outline: Color, val primary: Color)

private val MidnightVioletBackground = Color(0xFF0B091E)
private val MidnightVioletSurface = Color(0xFF151233)
private val MidnightVioletOutline = Color(0xFF2E2866)
val MidnightVioletAccent = Color(0xFF7C3AED)
private val MidnightPinkBackground = Color(0xFF160A14)
private val MidnightPinkSurface = Color(0xFF241120)
private val MidnightPinkOutline = Color(0xFF4A1D3E)
val MidnightPinkAccent = Color(0xFFEC4899)
private val MidnightCyanBackground = Color(0xFF071318)
private val MidnightCyanSurface = Color(0xFF0E2129)
private val MidnightCyanOutline = Color(0xFF1B4352)
val MidnightCyanAccent = Color(0xFF06B6D4)
private val MidnightGreenBackground = Color(0xFF07140F)
private val MidnightGreenSurface = Color(0xFF0E221B)
private val MidnightGreenOutline = Color(0xFF1B4536)
val MidnightGreenAccent = Color(0xFF10B981)

private fun midnightPalette(accent: MidnightAccent): MidnightPalette = when (accent) {
    MidnightAccent.VIOLET -> MidnightPalette(MidnightVioletBackground, MidnightVioletSurface, MidnightVioletOutline, MidnightVioletAccent)
    MidnightAccent.PINK -> MidnightPalette(MidnightPinkBackground, MidnightPinkSurface, MidnightPinkOutline, MidnightPinkAccent)
    MidnightAccent.CYAN -> MidnightPalette(MidnightCyanBackground, MidnightCyanSurface, MidnightCyanOutline, MidnightCyanAccent)
    MidnightAccent.GREEN -> MidnightPalette(MidnightGreenBackground, MidnightGreenSurface, MidnightGreenOutline, MidnightGreenAccent)
}
private val MidnightOnSurface = Color(0xFFF8FAFC)
private val MidnightOnSurfaceVariant = Color(0xFFA5A1C2)
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

internal fun midnightSurfaceContainerPalette(accent: MidnightAccent): SurfaceContainerPalette =
    when (accent) {
        MidnightAccent.VIOLET -> SurfaceContainerPalette(
            Color(0xFF0A0819), Color(0xFF100E27), Color(0xFF171433), Color(0xFF1F1A42), Color(0xFF292354)
        )
        MidnightAccent.PINK -> SurfaceContainerPalette(
            Color(0xFF140810), Color(0xFF1D0D19), Color(0xFF281321), Color(0xFF341A2C), Color(0xFF402237)
        )
        MidnightAccent.CYAN -> SurfaceContainerPalette(
            Color(0xFF061116), Color(0xFF0A1A20), Color(0xFF10252D), Color(0xFF17323C), Color(0xFF1F404B)
        )
        MidnightAccent.GREEN -> SurfaceContainerPalette(
            Color(0xFF06120C), Color(0xFF0B1C13), Color(0xFF11291D), Color(0xFF183728), Color(0xFF204634)
        )
    }
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
        is ThemePackConfig.MidnightViolet -> {
            val palette = midnightPalette(config.midnightAccent)
            val containers = midnightSurfaceContainerPalette(config.midnightAccent)
            darkColorScheme(
                surfaceContainerLowest = containers.lowest,
                surfaceContainerLow = containers.low,
                surfaceContainer = containers.container,
                surfaceContainerHigh = containers.high,
                surfaceContainerHighest = containers.highest,
                primary = palette.primary,
                onPrimary = if (palette.primary.luminance() > 0.175f) palette.background else Color.White,
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
                outline = palette.outline,
                outlineVariant = palette.outline
            )
        }
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
