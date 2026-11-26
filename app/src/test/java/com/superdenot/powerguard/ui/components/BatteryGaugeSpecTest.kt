package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import com.superdenot.powerguard.ui.CHARGING_SOURCE_CAPABILITY_LABEL
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
    fun capsuleInnerContourIsConcentricAndKeepsUniformOuterStroke() {
        assertEquals(1.8f, CAPSULE_OUTER_STROKE_DP, 0f)
        assertEquals(1.5f, CAPSULE_INNER_GAP_DP, 0f)
        val geometry = capsuleInnerGeometry(
            outerCornerRadiusPx = 24f,
            strokeWidthPx = 1.8f,
            innerGapPx = 1.5f,
            minimumCornerRadiusPx = 4f
        )
        assertEquals(3.3f, geometry.insetPx, 0.0001f)
        assertEquals(20.7f, geometry.cornerRadiusPx, 0.0001f)

        val constrained = capsuleInnerGeometry(
            outerCornerRadiusPx = 3f,
            strokeWidthPx = 1.8f,
            innerGapPx = 1.5f,
            minimumCornerRadiusPx = 4f
        )
        assertEquals(4f, constrained.cornerRadiusPx, 0f)
    }

    @Test
    fun segmentedBlocksStartAtSixAndAdvanceClockwiseUpTheLeftSide() {
        assertEquals(90f, SEGMENTED_GAUGE_START_ANGLE_DEGREES, 0f)
        assertEquals(90f, segmentedGaugeStartAngle(0), 0f)
        assertEquals(105f, segmentedGaugeStartAngle(1), 0f)
        assertEquals(180f, segmentedGaugeStartAngle(6), 0f)
        assertEquals(270f, segmentedGaugeStartAngle(12), 0f)
        assertEquals(435f, segmentedGaugeStartAngle(23), 0f)
        assertEquals(12, segmentedGaugeLitCount(50))
        assertEquals(16, segmentedGaugeLitCount(65))
        assertTrue(segmentedGaugeTrailAlpha(3, headIndex = 3, activeCount = 8) >
            segmentedGaugeTrailAlpha(2, headIndex = 3, activeCount = 8))
    }

    @Test
    fun capsuleCometUsesAnUpwardVerticalBeamThatStaysInsideTheLiquidColumn() {
        val top = 12f
        val bottom = 92f
        val beamLength = 28f
        val atBottom = capsuleCometY(top, bottom, 0f)
        val midway = capsuleCometY(top, bottom, 0.5f)
        val atTop = capsuleCometY(top, bottom, 1f)
        assertEquals(bottom, atBottom, 0f)
        assertEquals(52f, midway, 0f)
        assertEquals(top, atTop, 0f)
        assertEquals(bottom, capsuleCometBeamTailBottom(atBottom, bottom, beamLength), 0f)
        assertEquals(80f, capsuleCometBeamTailBottom(midway, bottom, beamLength), 0f)
        assertEquals(40f, capsuleCometBeamTailBottom(atTop, bottom, beamLength), 0f)
    }

    @Test
    fun waveAnimationModesHaveDistinctVisualFeatures() {
        val sweep = waveAnimationFeatures(ChargingArcAnimation.ENERGY_SWEEP)
        val bubbles = waveAnimationFeatures(ChargingArcAnimation.SEGMENTED_FLOW)
        val comet = waveAnimationFeatures(ChargingArcAnimation.COMET_HEAD)
        val static = waveAnimationFeatures(ChargingArcAnimation.STATIC)

        assertTrue(sweep.animatedSurface)
        assertTrue(sweep.upwardSweep)
        assertFalse(sweep.risingBubbles)
        assertFalse(sweep.outerComet)

        assertTrue(bubbles.animatedSurface)
        assertFalse(bubbles.upwardSweep)
        assertTrue(bubbles.risingBubbles)
        assertFalse(bubbles.outerComet)

        assertTrue(comet.animatedSurface)
        assertFalse(comet.upwardSweep)
        assertFalse(comet.risingBubbles)
        assertTrue(comet.outerComet)

        assertFalse(static.animatedSurface)
        assertFalse(static.upwardSweep)
        assertFalse(static.risingBubbles)
        assertFalse(static.outerComet)
    }

    @Test
    fun waveAmplitudeAndMotionGeometryMatchTheFourModeDesign() {
        assertEquals(6.5f, WAVE_AMPLITUDE_DP, 0f)
        assertEquals(1.6f, WAVE_CYCLES, 0f)
        assertEquals(115f, WAVE_REAR_PHASE_OFFSET_DEGREES, 0f)
        assertEquals(2.0071286f, WAVE_REAR_PHASE_OFFSET, 0.000001f)
        assertEquals(2f, WAVE_MENISCUS_STROKE_DP, 0f)
        assertEquals(7, WAVE_BUBBLE_COUNT)
        assertEquals(100f, WAVE_COMET_TAIL_DEGREES, 0f)
        assertTrue(WAVE_REAR_FILL_ALPHA < WAVE_FRONT_FILL_ALPHA)
    }

    @Test
    fun capsuleGlossIsDisabledOnlyForStaticMode() {
        assertFalse(capsuleGlossEnabled(ChargingArcAnimation.STATIC))
        ChargingArcAnimation.values()
            .filter { it != ChargingArcAnimation.STATIC }
            .forEach { assertTrue(capsuleGlossEnabled(it)) }
    }

    @Test
    fun staticModeDisablesAnimationForEveryGaugeModel() {
        BatteryGaugeModel.values().forEach { model ->
            assertFalse(
                "Statis must disable effects for ${model.label}",
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

    @Test
    fun frameClockFallbackOnlyRunsWhenPlatformAnimatorsAreDisabledAndEffectsAreRequested() {
        assertTrue(shouldUseGaugeFrameClockFallback(animatorsEnabled = false, animateEffects = true))
        assertFalse(shouldUseGaugeFrameClockFallback(animatorsEnabled = true, animateEffects = true))
        assertFalse(shouldUseGaugeFrameClockFallback(animatorsEnabled = false, animateEffects = false))
    }

    @Test
    fun chargingScreenUsesConciseSourceCapabilityLabel() {
        assertEquals("Kemampuan sumber", CHARGING_SOURCE_CAPABILITY_LABEL)
    }

    @Test
    fun waveLiquidLevelTracksBatteryFractionForChargingIdleAndExactBoundariesAcrossEveryAnimation() {
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
        assertEquals(2.0071286f, WAVE_REAR_PHASE_OFFSET, 0.000001f)
        assertEquals(0.76f, WAVE_REAR_AMPLITUDE_SCALE, 0f)
        assertTrue(WAVE_REAR_FILL_ALPHA < WAVE_FRONT_FILL_ALPHA)
        assertEquals(0.88f, WAVE_FRONT_FILL_ALPHA, 0f)
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
