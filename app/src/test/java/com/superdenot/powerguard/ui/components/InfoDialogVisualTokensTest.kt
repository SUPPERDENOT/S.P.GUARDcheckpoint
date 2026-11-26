package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.MidnightAccent
import com.superdenot.powerguard.ui.theme.basicSurfaceContainerPalette
import com.superdenot.powerguard.ui.theme.midnightSurfaceContainerPalette
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
    fun everyMidnightAccentDefinesDistinctThemeTonedSurfaceContainers() {
        val materialDefaultDarkHigh = Color(0xFF2B2930)
        MidnightAccent.values().forEach { accent ->
            val palette = midnightSurfaceContainerPalette(accent)
            val colors = listOf(
                palette.lowest,
                palette.low,
                palette.container,
                palette.high,
                palette.highest
            )
            assertEquals("accent=$accent", 5, colors.toSet().size)
            assertNotEquals("accent=$accent", materialDefaultDarkHigh, palette.high)
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
        assertEquals(10f, INFO_DIALOG_HEADER_BOTTOM_PADDING_DP, 0f)
        assertEquals(8f, INFO_DIALOG_SCROLL_VERTICAL_PADDING_DP, 0f)
        assertEquals(16f, INFO_DIALOG_FOOTER_HORIZONTAL_PADDING_DP, 0f)
        assertEquals(8f, INFO_DIALOG_FOOTER_TOP_PADDING_DP, 0f)
        assertEquals(12f, INFO_DIALOG_FOOTER_BOTTOM_PADDING_DP, 0f)
        assertEquals(36f, INFO_DIALOG_BUTTON_HEIGHT_DP, 0f)
    }
}
