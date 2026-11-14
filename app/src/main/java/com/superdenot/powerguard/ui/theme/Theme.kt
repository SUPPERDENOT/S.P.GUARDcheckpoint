package com.superdenot.powerguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldCharge = Color(0xFF00E676)
val CyanFastCharge = Color(0xFF00E5FF)
val AmberThermal = Color(0xFFFFAB00)
val CrimsonAnomaly = Color(0xFFFF5252)
val SlateSurface = Color(0xFF12161F)
val SlateCard = Color(0xFF1B2230)

private val PowerGuardDarkScheme = darkColorScheme(
    primary = EmeraldCharge,
    secondary = CyanFastCharge,
    tertiary = AmberThermal,
    error = CrimsonAnomaly,
    background = SlateSurface,
    surface = SlateSurface,
    surfaceVariant = SlateCard,
    onPrimary = Color(0xFF00210E),
    onBackground = Color(0xFFECEFF4),
    onSurface = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

@Composable
fun PowerGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PowerGuardDarkScheme,
        content = content
    )
}
