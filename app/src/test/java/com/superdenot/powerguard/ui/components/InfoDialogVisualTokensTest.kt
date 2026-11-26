package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.MidnightAccent
import com.superdenot.powerguard.ui.theme.basicSurfaceContainerPalette
import com.superdenot.powerguard.ui.theme.midnightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoDialogVisualTokensTest {

    @Test
    fun everyBasicAccentDefinesDistinctLightAndDarkSurfaceContainers() {
        val materialDefaultDarkHigh = Color(0xFF2B2930)
        BasicAccent.values().forEach { accent ->
            listOf(false, true).forEach { isDark ->
                val palette = basicSurfaceContainerPalette(accent, isDark)
                val colors = listOf(
                    palette.lowest,
                    palette.low,
                    palette.container,
                    palette.high,
                    palette.highest
                )
                assertEquals("accent=$accent dark=$isDark", 5, colors.toSet().size)
                assertNotEquals("accent=$accent dark=$isDark", materialDefaultDarkHigh, palette.high)
                assertTrue(
                    "accent=$accent dark=$isDark must not fall back to neutral Material 3 gray",
                    palette.high != materialDefaultDarkHigh
                )
            }
        }
    }

    @Test
    fun everyMidnightAccentMatchesTheEyeComfortTokenSpecification() {
        val expected = mapOf(
            MidnightAccent.VIOLET to MidnightExpected(
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
            ),
            MidnightAccent.PINK to MidnightExpected(
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
            ),
            MidnightAccent.CYAN to MidnightExpected(
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
            ),
            MidnightAccent.GREEN to MidnightExpected(
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
        )

        expected.forEach { (accent, tokens) ->
            val scheme = midnightColorScheme(accent)
            assertEquals("$accent background", tokens.background, scheme.background)
            assertEquals("$accent surface", tokens.surface, scheme.surface)
            assertEquals("$accent surfaceContainerLowest", tokens.lowest, scheme.surfaceContainerLowest)
            assertEquals("$accent surfaceContainerLow", tokens.low, scheme.surfaceContainerLow)
            assertEquals("$accent surfaceContainer", tokens.container, scheme.surfaceContainer)
            assertEquals("$accent surfaceContainerHigh", tokens.high, scheme.surfaceContainerHigh)
            assertEquals("$accent surfaceContainerHighest", tokens.highest, scheme.surfaceContainerHighest)
            assertEquals("$accent outlineVariant", tokens.outlineVariant, scheme.outlineVariant)
            assertEquals("$accent primary", tokens.primary, scheme.primary)
            assertEquals("$accent onPrimary", tokens.onPrimary, scheme.onPrimary)
            assertEquals("$accent onBackground", Color(0xFFE6EAF2), scheme.onBackground)
            assertEquals("$accent onSurface", Color(0xFFE6EAF2), scheme.onSurface)
            assertEquals("$accent onSurfaceVariant", Color(0xFF9AA4B8), scheme.onSurfaceVariant)

            assertContrastAtLeast("$accent onPrimary / primary", scheme.onPrimary, scheme.primary, 4.5)
            assertContrastAtLeast("$accent onSurface / surface", scheme.onSurface, scheme.surface, 4.5)
            assertContrastAtLeast("$accent onSurface / background", scheme.onSurface, scheme.background, 4.5)
            assertContrastAtLeast("$accent onSurfaceVariant / surface", scheme.onSurfaceVariant, scheme.surface, 4.5)
        }
    }

    @Test
    fun everyMidnightAccentHasFiveDistinctSurfaceContainerLevels() {
        MidnightAccent.values().forEach { accent ->
            val scheme = midnightColorScheme(accent)
            val colors = listOf(
                scheme.surfaceContainerLowest,
                scheme.surfaceContainerLow,
                scheme.surfaceContainer,
                scheme.surfaceContainerHigh,
                scheme.surfaceContainerHighest
            )
            assertEquals("$accent", 5, colors.toSet().size)
            assertNotEquals("$accent", Color(0xFF2B2930), scheme.surfaceContainerHigh)
        }
    }

    @Test
    fun infoBadgeUsesFlatCompactGeometry() {
        assertEquals(16f, INFO_BADGE_DIAMETER_DP, 0f)
        assertEquals(11f, INFO_BADGE_ICON_DIAMETER_DP, 0f)
        assertEquals(48f, INFO_BADGE_TOUCH_TARGET_DP, 0f)
        assertEquals(4f, INFO_BADGE_GAP_DP, 0f)
    }

    @Test
    fun infoDialogHeaderScrollAndFooterUseSlimLayoutTokens() {
        assertEquals(18f, INFO_DIALOG_HEADER_TITLE_SP, 0f)
        assertEquals(2f, INFO_DIALOG_HEADER_GAP_DP, 0f)
        assertEquals(16f, INFO_DIALOG_HEADER_HORIZONTAL_PADDING_DP, 0f)
        assertEquals(14f, INFO_DIALOG_HEADER_TOP_PADDING_DP, 0f)
        assertEquals(8f, INFO_DIALOG_HEADER_BOTTOM_PADDING_DP, 0f)
        assertEquals(16f, INFO_DIALOG_SCROLL_HORIZONTAL_PADDING_DP, 0f)
        assertEquals(4f, INFO_DIALOG_SCROLL_TOP_PADDING_DP, 0f)
        assertEquals(12f, INFO_DIALOG_SCROLL_BOTTOM_PADDING_DP, 0f)
        assertEquals(16f, INFO_DIALOG_FOOTER_HORIZONTAL_PADDING_DP, 0f)
        assertEquals(8f, INFO_DIALOG_FOOTER_TOP_PADDING_DP, 0f)
        assertEquals(12f, INFO_DIALOG_FOOTER_BOTTOM_PADDING_DP, 0f)
        assertEquals(36f, INFO_DIALOG_BUTTON_HEIGHT_DP, 0f)
    }

    @Test
    fun bothInfoDialogsShareSolidSurfaceAndCompactFooterTokens() {
        assertEquals(20f, INFO_DIALOG_SURFACE_CORNER_RADIUS_DP, 0f)
        assertEquals(1f, INFO_DIALOG_SURFACE_BORDER_DP, 0f)
        assertEquals(0f, INFO_DIALOG_SURFACE_TONAL_ELEVATION_DP, 0f)
        assertEquals(36f, INFO_DIALOG_BUTTON_HEIGHT_DP, 0f)
    }

    private fun assertContrastAtLeast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Double
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$label contrast ratio $ratio is below $minimum", ratio >= minimum)
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

    private data class MidnightExpected(
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
}
