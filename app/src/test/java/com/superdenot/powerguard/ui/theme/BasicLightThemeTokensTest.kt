package com.superdenot.powerguard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicLightThemeTokensTest {
    @Test fun lightOutlineHasClearSeparationFromPaperSurface() {
        assertTrue(contrastRatio(BasicLightOutlineVariant, Color(0xFFFFFCF6)) >= 3.0)
    }
    @Test fun secondaryTextRemainsReadableOnMineralSubCards() {
        assertTrue(contrastRatio(BasicLightOnSurfaceVariant, BasicLightSubSurface) >= 4.5)
    }
    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter.toDouble() + 0.05) / (darker.toDouble() + 0.05)
    }
}
