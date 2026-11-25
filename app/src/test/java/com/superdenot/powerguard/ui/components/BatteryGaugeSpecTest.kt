package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class BatteryGaugeSpecTest {
    private val palette = BatterySemanticColors(
        normal = Color.Blue,
        charging = Color.Green,
        warm = Color.Yellow,
        criticalThermal = Color.Red
    )

    @Test
    fun criticalThermalAlwaysWinsOverWarmAndCharging() {
        assertEquals(
            Color.Red,
            resolveBatteryGaugeColor(
                semanticColors = palette,
                isCriticalThermal = true,
                isWarm = true,
                isCharging = true
            )
        )
    }

    @Test
    fun warmWinsOverCharging() {
        assertEquals(
            Color.Yellow,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = true, isCharging = true)
        )
    }

    @Test
    fun normalChargingUsesGreenAndNormalIdleUsesThemeAccent() {
        assertEquals(
            Color.Green,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = true)
        )
        assertEquals(
            Color.Blue,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = false)
        )
    }

    @Test
    fun segmentedGaugeUsesTwentyFourClockwiseBlocksAcrossZeroToOneHundred() {
        assertEquals(24, SEGMENTED_GAUGE_BLOCK_COUNT)
        assertEquals(0, segmentedGaugeLitCount(0))
        assertEquals(1, segmentedGaugeLitCount(5))
        assertEquals(12, segmentedGaugeLitCount(50))
        assertEquals(24, segmentedGaugeLitCount(99))
        assertEquals(24, segmentedGaugeLitCount(100))
        assertEquals(0, segmentedGaugeLitCount(-20))
        assertEquals(24, segmentedGaugeLitCount(140))
        (0..100).forEach { level ->
            assertEquals("level=$level", (level * 24 / 100f).roundToInt(), segmentedGaugeLitCount(level))
        }
    }

    @Test
    fun capsuleSweepAndCometMoveFromBottomToTop() {
        val top = 10f
        val bottom = 90f
        assertEquals(bottom, capsuleCometY(top, bottom, 0f), 0f)
        assertEquals(50f, capsuleCometY(top, bottom, 0.5f), 0f)
        assertEquals(top, capsuleCometY(top, bottom, 1f), 0f)
    }

    @Test
    fun segmentedBlocksStartAtTwelveAndAdvanceClockwise() {
        assertEquals(-90f, segmentedGaugeStartAngle(0), 0f)
        assertEquals(-75f, segmentedGaugeStartAngle(1), 0f)
        assertEquals(255f, segmentedGaugeStartAngle(23), 0f)
        assertTrue(segmentedGaugeTrailAlpha(3, headIndex = 3, activeCount = 8) >
            segmentedGaugeTrailAlpha(2, headIndex = 3, activeCount = 8))
    }

    @Test
    fun waveLiquidLevelTracksBatteryFractionForChargingAndIdleAcrossEveryAnimation() {
        listOf(false, true).forEach { isCharging ->
            ChargingArcAnimation.values().forEach { animation ->
                assertEquals("charging=$isCharging animation=$animation", 0.69f,
                    waveLiquidFillLevel(0.69f, isCharging, animation), 0f)
            }
        }
        assertEquals(0f, waveLiquidFillLevel(-0.2f, false, ChargingArcAnimation.STATIC), 0f)
        assertEquals(1f, waveLiquidFillLevel(1.2f, true, ChargingArcAnimation.COMET_HEAD), 0f)
        listOf(false, true).forEach { isCharging ->
            ChargingArcAnimation.values().forEach { animation ->
                assertEquals(0f, waveLiquidFillLevel(0f, isCharging, animation), 0f)
                assertEquals(1f, waveLiquidFillLevel(1f, isCharging, animation), 0f)
            }
        }
    }

    @Test
    fun waveRendererUsesTwoOffsetLayersAndBrightFrontFill() {
        assertEquals(1.15f, WAVE_REAR_PHASE_OFFSET, 0f)
        assertEquals(0.68f, WAVE_REAR_AMPLITUDE_SCALE, 0f)
        assertTrue(WAVE_REAR_FILL_ALPHA < WAVE_FRONT_FILL_ALPHA)
        assertEquals(0.82f, WAVE_FRONT_FILL_ALPHA, 0f)
    }

    @Test
    fun forceAnimateEnablesPreviewWithoutChargingButStillHonorsVisibilityAndStaticMode() {
        assertTrue(
            shouldAnimateBatteryGauge(
                isCharging = false,
                isVisibleOnScreen = true,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.ENERGY_SWEEP,
                forceAnimate = true
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = false,
                isVisibleOnScreen = true,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.STATIC,
                forceAnimate = true
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = false,
                isVisibleOnScreen = false,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.COMET_HEAD,
                forceAnimate = true
            )
        )
    }

    @Test
    fun chargingAnimationOnlyRunsWhenVisibleAndThermallyNormal() {
        assertTrue(
            shouldAnimateBatteryGauge(
                isCharging = true,
                isVisibleOnScreen = true,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.ENERGY_SWEEP
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = true,
                isVisibleOnScreen = true,
                isCriticalThermal = true,
                isWarm = false,
                animation = ChargingArcAnimation.COMET_HEAD
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = true,
                isVisibleOnScreen = true,
                isCriticalThermal = false,
                isWarm = true,
                animation = ChargingArcAnimation.SEGMENTED_FLOW
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = true,
                isVisibleOnScreen = false,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.ENERGY_SWEEP
            )
        )
        assertFalse(
            shouldAnimateBatteryGauge(
                isCharging = true,
                isVisibleOnScreen = true,
                isCriticalThermal = false,
                isWarm = false,
                animation = ChargingArcAnimation.STATIC
            )
        )
    }
}
