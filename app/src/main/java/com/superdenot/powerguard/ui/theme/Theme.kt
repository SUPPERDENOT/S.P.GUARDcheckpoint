package com.superdenot.powerguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Industrial Instrument palette: warm paper, graphite, and restrained safety orange.
val EmeraldCharge = Color(0xFF39765B)
val CyanFastCharge = Color(0xFFD96B2B)
val AmberThermal = Color(0xFFA87524)
val CrimsonAnomaly = Color(0xFFB8473D)
val SlateSurface = Color(0xFFF4F0E7)
val SlateCard = Color(0xFFE9E3D8)

private val Paper = Color(0xFFFFFCF6)
private val Graphite = Color(0xFF252923)
private val SecondaryText = Color(0xFF62665F)
private val Border = Color(0xFFD8D1C5)

private val PowerGuardLightScheme = lightColorScheme(
    primary = CyanFastCharge,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5DFD1),
    onPrimaryContainer = Color(0xFF51250F),
    secondary = EmeraldCharge,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9DF),
    onSecondaryContainer = Color(0xFF173D2B),
    tertiary = AmberThermal,
    onTertiary = Color.White,
    error = CrimsonAnomaly,
    onError = Color.White,
    errorContainer = Color(0xFFF5DEDA),
    onErrorContainer = Color(0xFF5B211B),
    background = SlateSurface,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SecondaryText,
    outline = Border,
    outlineVariant = Border.copy(alpha = 0.7f)
)

@Composable
fun PowerGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PowerGuardLightScheme,
        content = content
    )
}
