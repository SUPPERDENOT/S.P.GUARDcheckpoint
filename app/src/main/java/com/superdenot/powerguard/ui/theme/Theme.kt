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

private val Paper = Color(0xFFFFFCF6)
private val SecondaryText = Color(0xFF62665F)
private val Border = Color(0xFFD8D1C5)
private val TerracottaAccent = Color(0xFFD96B2B)
private val EmeraldAccent = Color(0xFF39765B)
private val CobaltAccent = Color(0xFF315D9B)
fun basicAccentSwatches(): List<Color> = listOf(TerracottaAccent, EmeraldAccent, CobaltAccent)
private val DarkBackground = Color(0xFF171916)
private val DarkSurface = Color(0xFF222720)
private val DarkText = Color(0xFFF4F0E7)
private val DarkMuted = Color(0xFFB5B9B0)
private val MidnightBackground = Color(0xFF0B091E)
private val MidnightSurface = Color(0xFF151233)
private val MidnightOutline = Color(0xFF2E2866)
val MidnightVioletAccent = Color(0xFF7C3AED)
private val MidnightPrimary = MidnightVioletAccent
private val MidnightOnSurface = Color(0xFFF8FAFC)
private val MidnightOnSurfaceVariant = Color(0xFFA5A1C2)
private val MidnightColdMetric = Color(0xFF102B3A)
private val MidnightWarmMetric = Color(0xFF2D1F27)
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
            if (isDark) {
                darkColorScheme(
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
                    surfaceVariant = SlateCard,
                    onSurfaceVariant = SecondaryText,
                    outline = Border,
                    outlineVariant = Border.copy(alpha = 0.7f)
                )
            }
        }
        is ThemePackConfig.MidnightViolet -> darkColorScheme(
            primary = MidnightPrimary,
            onPrimary = Color.White,
            secondary = EmeraldCharge,
            onSecondary = Color.White,
            tertiary = AmberThermal,
            error = CrimsonAnomaly,
            background = MidnightBackground,
            onBackground = MidnightOnSurface,
            surface = MidnightSurface,
            onSurface = MidnightOnSurface,
            surfaceVariant = MidnightSurface,
            onSurfaceVariant = MidnightOnSurfaceVariant,
            outline = MidnightOutline,
            outlineVariant = MidnightOutline
        )
    }

    val metricTints = when (config) {
        is ThemePackConfig.MidnightViolet -> BatteryMetricTints(
            enabled = config.dualTintMetrics,
            cold = MidnightColdMetric,
            warm = MidnightWarmMetric
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
