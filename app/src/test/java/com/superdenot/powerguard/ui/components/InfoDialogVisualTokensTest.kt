package com.superdenot.powerguard.ui.components

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class InfoDialogVisualTokensTest {

    @Test
    fun popupSurfaceUsesTheActiveLightColorSchemeTokens() {
        val container = Color(0xFFFFFCF6)
        val outline = Color(0xFF77736B)
        val scheme = lightColorScheme().copy(
            surfaceContainerHigh = container,
            outlineVariant = outline
        )

        assertEquals(
            InfoDialogSurfaceColors(containerColor = container, borderColor = outline),
            infoDialogSurfaceColors(scheme)
        )
    }

    @Test
    fun popupSurfaceUsesTheActiveDarkColorSchemeTokens() {
        val container = Color(0xFF29272F)
        val outline = Color(0xFF77717F)
        val scheme = darkColorScheme().copy(
            surfaceContainerHigh = container,
            outlineVariant = outline
        )

        assertEquals(
            InfoDialogSurfaceColors(containerColor = container, borderColor = outline),
            infoDialogSurfaceColors(scheme)
        )
    }

    @Test
    fun infoBadgeKeepsTheSpecifiedCircleAndTouchTargetGeometry() {
        assertEquals(18f, INFO_BADGE_DIAMETER_DP, 0f)
        assertEquals(12f, INFO_BADGE_ICON_DIAMETER_DP, 0f)
        assertEquals(24f, INFO_BADGE_TOUCH_TARGET_DP, 0f)
        assertEquals(3f, INFO_BADGE_GAP_DP, 0f)
    }
}
