package com.superdenot.powerguard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePackColorSchemeTest {
    @Test
    fun everyNewAccentHasAtLeastWcagAaPrimaryContrast() {
        val schemes = listOf(
            amoledPureColorScheme(AmoledPureAccent.AMBER_GOLD),
            amoledPureColorScheme(AmoledPureAccent.CLEAN_WHITE),
            evCockpitColorScheme(EvCockpitAccent.ELECTRIC_SKY),
            evCockpitColorScheme(EvCockpitAccent.VOLT_LIME),
            nordicFrostColorScheme(NordicFrostAccent.COBALT_STEEL),
            nordicFrostColorScheme(NordicFrostAccent.TEAL),
            monochromeColorScheme(isDark = false),
            monochromeColorScheme(isDark = true)
        )
        schemes.forEach { scheme ->
            assertTrue("primary/onPrimary contrast=${contrastRatio(scheme.primary, scheme.onPrimary)}",
                contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5)
        }
    }

    @Test
    fun everyNewSchemeDefinesFiveDistinctSurfaceContainerLevelsWithoutM3DefaultGrey() {
        val schemes = listOf(
            amoledPureColorScheme(AmoledPureAccent.AMBER_GOLD),
            amoledPureColorScheme(AmoledPureAccent.CLEAN_WHITE),
            evCockpitColorScheme(EvCockpitAccent.ELECTRIC_SKY),
            evCockpitColorScheme(EvCockpitAccent.VOLT_LIME),
            nordicFrostColorScheme(NordicFrostAccent.COBALT_STEEL),
            nordicFrostColorScheme(NordicFrostAccent.TEAL),
            monochromeColorScheme(isDark = false),
            monochromeColorScheme(isDark = true)
        )
        val materialDefault = Color(0xFF2B2930)
        schemes.forEach { scheme ->
            val containers = surfaceContainers(scheme)
            assertEquals(5, containers.distinct().size)
            assertFalse(containers.contains(materialDefault))
        }
    }

    @Test
    fun amoledPureUsesTrueBlackAndSpecifiedContainerRamp() {
        val scheme = amoledPureColorScheme(AmoledPureAccent.AMBER_GOLD)
        assertEquals(Color(0xFF000000), scheme.background)
        assertEquals(Color(0xFF000000), scheme.surface)
        assertEquals(
            listOf(
                Color(0xFF050505), Color(0xFF090909), Color(0xFF0D0D0D),
                Color(0xFF131313), Color(0xFF181818)
            ),
            surfaceContainers(scheme)
        )
        assertTrue(contrastRatio(scheme.outlineVariant, scheme.surface) >= 3.0)
    }

    @Test
    fun lightAndDarkMonochromeRemainGrayscaleAndHighContrast() {
        listOf(monochromeColorScheme(false), monochromeColorScheme(true)).forEach { scheme ->
            assertEquals(scheme.primary.red, scheme.primary.green, 0.0001f)
            assertEquals(scheme.primary.green, scheme.primary.blue, 0.0001f)
            assertEquals(scheme.onPrimary.red, scheme.onPrimary.green, 0.0001f)
            assertEquals(scheme.onPrimary.green, scheme.onPrimary.blue, 0.0001f)
        }
    }

    private fun surfaceContainers(scheme: ColorScheme) = listOf(
        scheme.surfaceContainerLowest,
        scheme.surfaceContainerLow,
        scheme.surfaceContainer,
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest
    )

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter.toDouble() + 0.05) / (darker.toDouble() + 0.05)
    }
}
