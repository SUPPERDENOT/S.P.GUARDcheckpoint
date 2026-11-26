package com.superdenot.powerguard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StandaloneThemeTokensTest {

    @Test
    fun amoledPureMatchesBothRequestedAccentPalettes() {
        assertScheme(
            amoledPureColorScheme(AmoledPureAccent.AMBER_GOLD),
            background = 0xFF000000, surface = 0xFF000000,
            lowest = 0xFF000000, low = 0xFF08090B, container = 0xFF0D0E10,
            high = 0xFF14161A, highest = 0xFF1C1F24, outline = 0xFF22252A,
            primary = 0xFFFBBF24, onPrimary = 0xFF1A1000,
            onBackground = 0xFFF2F4F8, onSurface = 0xFFF2F4F8, onSurfaceVariant = 0xFF9CA3AF
        )
        assertScheme(
            amoledPureColorScheme(AmoledPureAccent.CLEAN_WHITE),
            background = 0xFF000000, surface = 0xFF000000,
            lowest = 0xFF000000, low = 0xFF08090B, container = 0xFF0D0E10,
            high = 0xFF14161A, highest = 0xFF1C1F24, outline = 0xFF22252A,
            primary = 0xFFF8FAFC, onPrimary = 0xFF0D0E10,
            onBackground = 0xFFF2F4F8, onSurface = 0xFFF2F4F8, onSurfaceVariant = 0xFF9CA3AF
        )
    }

    @Test
    fun evCockpitMatchesBothRequestedAccentPalettes() {
        assertScheme(
            evCockpitColorScheme(EvCockpitAccent.ELECTRIC_SKY),
            background = 0xFF0F172A, surface = 0xFF1E293B,
            lowest = 0xFF090E1A, low = 0xFF131C31, container = 0xFF1E293B,
            high = 0xFF263449, highest = 0xFF304059, outline = 0xFF334155,
            primary = 0xFF38BDF8, onPrimary = 0xFF061A26,
            onBackground = 0xFFF1F5F9, onSurface = 0xFFF1F5F9, onSurfaceVariant = 0xFF94A3B8
        )
        assertScheme(
            evCockpitColorScheme(EvCockpitAccent.VOLT_LIME),
            background = 0xFF0F172A, surface = 0xFF1E293B,
            lowest = 0xFF090E1A, low = 0xFF131C31, container = 0xFF1E293B,
            high = 0xFF263449, highest = 0xFF304059, outline = 0xFF334155,
            primary = 0xFFA3E635, onPrimary = 0xFF132202,
            onBackground = 0xFFF1F5F9, onSurface = 0xFFF1F5F9, onSurfaceVariant = 0xFF94A3B8
        )
    }

    @Test
    fun nordicFrostMatchesBothRequestedAccentPalettes() {
        assertScheme(
            nordicFrostColorScheme(NordicFrostAccent.COBALT_STEEL),
            background = 0xFFF1F5F9, surface = 0xFFFFFFFF,
            lowest = 0xFFFFFFFF, low = 0xFFF8FAFC, container = 0xFFFFFFFF,
            high = 0xFFE8EEF5, highest = 0xFFDCE4EE, outline = 0xFFCBD5E1,
            primary = 0xFF2563EB, onPrimary = 0xFFFFFFFF,
            onBackground = 0xFF0F172A, onSurface = 0xFF0F172A, onSurfaceVariant = 0xFF475569
        )
        assertScheme(
            nordicFrostColorScheme(NordicFrostAccent.TEAL),
            background = 0xFFF1F5F9, surface = 0xFFFFFFFF,
            lowest = 0xFFFFFFFF, low = 0xFFF8FAFC, container = 0xFFFFFFFF,
            high = 0xFFE8EEF5, highest = 0xFFDCE4EE, outline = 0xFFCBD5E1,
            primary = 0xFF0D9488, onPrimary = 0xFFFFFFFF,
            onBackground = 0xFF0F172A, onSurface = 0xFF0F172A, onSurfaceVariant = 0xFF475569
        )
    }

    @Test
    fun monochromeMatchesDarkAndLightRequestedVariants() {
        assertScheme(
            monochromeColorScheme(MonochromeVariant.DARK_CHARCOAL),
            background = 0xFF141414, surface = 0xFF1C1C1C,
            lowest = 0xFF0E0E0E, low = 0xFF181818, container = 0xFF1C1C1C,
            high = 0xFF252525, highest = 0xFF2E2E2E, outline = 0xFF383838,
            primary = 0xFFE5E5E5, onPrimary = 0xFF141414,
            onBackground = 0xFFF5F5F5, onSurface = 0xFFF5F5F5, onSurfaceVariant = 0xFFA3A3A3
        )
        assertScheme(
            monochromeColorScheme(MonochromeVariant.LIGHT_PAPER),
            background = 0xFFF5F5F4, surface = 0xFFFFFFFF,
            lowest = 0xFFFFFFFF, low = 0xFFFAFAF9, container = 0xFFFFFFFF,
            high = 0xFFECECEB, highest = 0xFFE2E2E0, outline = 0xFFD6D3D1,
            primary = 0xFF1C1917, onPrimary = 0xFFFAFAF9,
            onBackground = 0xFF1C1917, onSurface = 0xFF1C1917, onSurfaceVariant = 0xFF57534E
        )
    }

    @Test
    fun allStandaloneSchemesKeepTextAndSurfaceContrastReadable() {
        val schemes = listOf(
            amoledPureColorScheme(AmoledPureAccent.AMBER_GOLD),
            amoledPureColorScheme(AmoledPureAccent.CLEAN_WHITE),
            evCockpitColorScheme(EvCockpitAccent.ELECTRIC_SKY),
            evCockpitColorScheme(EvCockpitAccent.VOLT_LIME),
            nordicFrostColorScheme(NordicFrostAccent.COBALT_STEEL),
            nordicFrostColorScheme(NordicFrostAccent.TEAL),
            monochromeColorScheme(MonochromeVariant.DARK_CHARCOAL),
            monochromeColorScheme(MonochromeVariant.LIGHT_PAPER)
        )
        schemes.forEach { scheme ->
            assertTrue("onSurface/background contrast", contrastRatio(scheme.onSurface, scheme.background) >= 4.5)
            assertTrue("onSurface/surface contrast", contrastRatio(scheme.onSurface, scheme.surface) >= 4.5)
            assertTrue("onSurfaceVariant/surface contrast", contrastRatio(scheme.onSurfaceVariant, scheme.surface) >= 4.5)
        }
        val tealButtonTextContrast = contrastRatio(
            nordicFrostColorScheme(NordicFrostAccent.TEAL).onPrimary,
            nordicFrostColorScheme(NordicFrostAccent.TEAL).primary
        )
        assertEquals(3.74, tealButtonTextContrast, 0.02)
    }

    private fun assertScheme(
        scheme: ColorScheme,
        background: Long,
        surface: Long,
        lowest: Long,
        low: Long,
        container: Long,
        high: Long,
        highest: Long,
        outline: Long,
        primary: Long,
        onPrimary: Long,
        onBackground: Long,
        onSurface: Long,
        onSurfaceVariant: Long
    ) {
        assertEquals(Color(background), scheme.background)
        assertEquals(Color(surface), scheme.surface)
        assertEquals(Color(lowest), scheme.surfaceContainerLowest)
        assertEquals(Color(low), scheme.surfaceContainerLow)
        assertEquals(Color(container), scheme.surfaceContainer)
        assertEquals(Color(high), scheme.surfaceContainerHigh)
        assertEquals(Color(highest), scheme.surfaceContainerHighest)
        assertEquals(Color(outline), scheme.outlineVariant)
        assertEquals(Color(primary), scheme.primary)
        assertEquals(Color(onPrimary), scheme.onPrimary)
        assertEquals(Color(onBackground), scheme.onBackground)
        assertEquals(Color(onSurface), scheme.onSurface)
        assertEquals(Color(onSurfaceVariant), scheme.onSurfaceVariant)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92
            else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }
}
