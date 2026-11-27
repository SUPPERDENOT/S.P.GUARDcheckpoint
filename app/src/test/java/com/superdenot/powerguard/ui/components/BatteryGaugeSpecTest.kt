package com.superdenot.powerguard.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.superdenot.powerguard.ui.theme.BatterySemanticColors
import com.superdenot.powerguard.ui.CHARGING_SOURCE_CAPABILITY_LABEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

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
    fun batteryGaugeAccentUsesThemeAccentDuringChargingAndIdle() {
        assertEquals(
            Color.Green,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = true)
        )
        assertEquals(
            Color.Blue,
            resolveBatteryGaugeColor(palette, isCriticalThermal = false, isWarm = false, isCharging = false)
        )
        assertEquals(Color.Blue, resolveBatteryGaugeAccent(palette.normal, isCharging = true))
        assertEquals(Color.Blue, resolveBatteryGaugeAccent(palette.normal, isCharging = false))
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
        assertEquals(0.038f, FRONT_WAVE_AMPLITUDE_RATIO, 0f)
        assertEquals(5.2f, MAX_FRONT_WAVE_AMPLITUDE_DP, 0f)
        assertEquals(135f, WAVE_REAR_PHASE_OFFSET_DEGREES, 0f)
        assertEquals(2.3561945f, WAVE_REAR_PHASE_OFFSET, 0.000001f)
        assertEquals(-1f, WAVE_REAR_DIRECTION, 0f)
        assertEquals(3_000, WAVE_CYCLE_DURATION_MILLIS)
        assertEquals(2.6f, BACK_WAVE_ELEVATION_DP, 0f)
        assertEquals(0.82f, BACK_WAVE_AMPLITUDE_RATIO, 0f)
        assertEquals(0.75f, WAVE_REAR_CREST_STROKE_DP, 0f)
        assertEquals(0.25f, WAVE_REAR_CREST_ALPHA, 0f)
        assertEquals(0.30f, WAVE_REAR_FILL_ALPHA, 0f)
        assertEquals(0.95f, WAVE_FRONT_SURFACE_ALPHA, 0f)
        assertEquals(0.76f, WAVE_FRONT_BOTTOM_ALPHA, 0f)
        assertEquals(2f, WAVE_SAMPLE_STEP_DP, 0f)
        assertEquals(110f, WAVE_COMET_TAIL_DEGREES, 0f)
        assertEquals(6f, WAVE_COMET_HALO_STROKE_DP, 0f)
        assertEquals(3f, WAVE_COMET_TAIL_STROKE_DP, 0f)
        assertEquals(3.5f, WAVE_COMET_HEAD_RADIUS_DP, 0f)
        assertEquals(6f, WAVE_COMET_GLOW_RADIUS_DP, 0f)
        assertEquals(1.2f, WAVE_MENISCUS_STROKE_DP, 0f)
        assertEquals(7, WAVE_BUBBLE_COUNT)
        assertEquals(110f, WAVE_COMET_TAIL_DEGREES, 0f)
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
                    animationStyle = ChargingArcAnimation.STATIC
                )
            )
        }
    }

    @Test
    fun deltaGaugePhaseAccumulatesWhileAnimationIsEnabled() {
        val firstFrame = advanceGaugePhase(
            currentPhase = 0f,
            deltaMs = 32L,
            cycleDurationMillis = 2_400,
            shouldAnimate = true
        )
        val secondFrame = advanceGaugePhase(
            currentPhase = firstFrame,
            deltaMs = 32L,
            cycleDurationMillis = 2_400,
            shouldAnimate = true
        )

        assertEquals(32f / 2_400f, firstFrame, 0.000001f)
        assertEquals(64f / 2_400f, secondFrame, 0.000001f)
    }

    @Test
    fun deltaGaugePhaseWrapsAndBoundsLongFrameDeltas() {
        assertEquals(
            (0.99f + 64f / 2_400f) % 1f,
            advanceGaugePhase(0.99f, 900L, 2_400, shouldAnimate = true),
            0.000001f
        )
        assertEquals(
            0f,
            advanceGaugePhase(0f, -20L, 2_400, shouldAnimate = true),
            0f
        )
    }

    @Test
    fun deltaGaugePhaseResetsToZeroWhenAnimationIsDisabled() {
        assertEquals(
            0f,
            advanceGaugePhase(0.72f, 48L, 2_400, shouldAnimate = false),
            0f
        )
    }

    @Test
    fun chargingScreenUsesConciseSourceCapabilityLabel() {
        assertEquals("Kemampuan sumber", CHARGING_SOURCE_CAPABILITY_LABEL)
    }

    @Test
    fun waveSmoothstepEnvelopeIsFlatAtZeroAndFullAndFullyDevelopedInTheMiddle() {
        assertEquals(0f, waveSurfaceEnvelope(-0.1f), 0f)
        assertEquals(0f, waveSurfaceEnvelope(0f), 0f)
        assertEquals(0f, waveSurfaceEnvelope(0.02f), 0f)
        assertEquals(0.5f, waveSurfaceEnvelope(0.07f), 0.000001f)
        assertEquals(1f, waveSurfaceEnvelope(0.12f), 0f)
        assertEquals(1f, waveSurfaceEnvelope(0.5f), 0f)
        assertEquals(1f, waveSurfaceEnvelope(0.88f), 0f)
        assertEquals(0.5f, waveSurfaceEnvelope(0.93f), 0.000001f)
        assertEquals(0f, waveSurfaceEnvelope(0.98f), 0f)
        assertEquals(0f, waveSurfaceEnvelope(1f), 0f)
        assertEquals(0f, backWaveElevationOffsetDp(0f), 0f)
        assertEquals(2.6f, backWaveElevationOffsetDp(0.5f), 0f)
        assertEquals(0f, backWaveElevationOffsetDp(1f), 0f)
    }

    @Test
    fun frontWaveAmplitudeUsesDiameterRatioAndFivePointTwoDpMaximum() {
        assertEquals(3.8f, waveFrontAmplitudePx(100f, 5.2f), 0.000001f)
        assertEquals(5.2f, waveFrontAmplitudePx(200f, 5.2f), 0f)
        assertEquals(0f, waveFrontAmplitudePx(-10f, 4f), 0f)
        assertEquals(0f, waveFrontAmplitudePx(100f, -5.2f), 0f)
    }

    @Test
    fun harmonicSurfaceIsNormalizedAndSeamlessAtZeroAndThreeHundredSixtyDegrees() {
        val phases = listOf(0f, 0.7f, Math.PI.toFloat(), 4.8f)
        phases.forEach { phase ->
            val fullTurn = (2f * Math.PI).toFloat()
            assertEquals(waveHarmonicOffset(0f, phase), waveHarmonicOffset(fullTurn, phase), 0.000001f)
            assertEquals(
                waveHarmonicOffset(0f, waveRearPhase(phase)),
                waveHarmonicOffset(fullTurn, waveRearPhase(phase)),
                0.000001f
            )
        }
        for (step in 0..720) {
            val angle = (2f * Math.PI.toFloat()) * step / 720f
            assertTrue(kotlin.math.abs(waveHarmonicOffset(angle, 0.73f)) <= 1.000001f)
        }
    }

    @Test
    fun backWavePhaseIsOppositeToFrontAndContinuousAcrossTheFullCycle() {
        assertEquals(-1f, WAVE_REAR_DIRECTION, 0f)
        assertEquals(135f, WAVE_REAR_PHASE_OFFSET_DEGREES, 0f)
        assertEquals(2.3561945f, WAVE_REAR_PHASE_OFFSET, 0.000001f)
        val start = waveRearPhase(0f)
        val middle = waveRearPhase(Math.PI.toFloat())
        val end = waveRearPhase((2f * Math.PI).toFloat())
        assertEquals(WAVE_REAR_PHASE_OFFSET, start, 0f)
        assertEquals(Math.toRadians(-45.0).toFloat(), middle, 0.000001f)
        assertEquals(sin(start), sin(end), 0.000001f)
        assertEquals(cos(start), cos(end), 0.000001f)
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
    fun waveRendererUsesSemiTransparentAccentRearAndCounterDirectionFrontAndMeniscus() {
        val accent = Color(0xFF12B886)
        val layers = waveLayerColors(accent)
        val rearAtZero = waveRearPhase(0f)
        val rearAtWrap = waveRearPhase((2f * Math.PI).toFloat())
        assertEquals(-1f, WAVE_REAR_DIRECTION, 0f)
        assertEquals(sin(rearAtZero), sin(rearAtWrap), 0.000001f)
        assertEquals(cos(rearAtZero), cos(rearAtWrap), 0.000001f)
        assertEquals(135f, WAVE_REAR_PHASE_OFFSET_DEGREES, 0f)
        assertEquals(0.78f, BACK_WAVE_AMPLITUDE_RATIO, 0f)
        assertEquals(0.30f, WAVE_REAR_FILL_ALPHA, 0f)
        assertEquals(0.25f, WAVE_REAR_CREST_ALPHA, 0f)
        assertEquals(0.95f, WAVE_FRONT_SURFACE_ALPHA, 0f)
        assertEquals(0.76f, WAVE_FRONT_BOTTOM_ALPHA, 0f)
        assertEquals(accent.copy(alpha = 0.30f), layers.rear)
        assertEquals(accent.copy(alpha = 0.25f), layers.rearCrest)
        assertEquals(accent.copy(alpha = 0.95f), layers.frontSurface)
        assertEquals(accent.copy(alpha = 0.76f), layers.frontBottom)
        assertEquals(lerp(accent, Color.White, 0.32f).copy(alpha = 0.95f), layers.frontHighlight)
        assertEquals(waveMeniscusColor(accent), layers.meniscus)
        assertEquals(0.30f, layers.rear.alpha, 0.003f)
        assertEquals(0.25f, layers.rearCrest.alpha, 0.001f)
        assertEquals(1.2f, WAVE_MENISCUS_STROKE_DP, 0f)
        assertEquals(50, waveSampleCount(100f, 2f))
        assertEquals(2f, WAVE_SAMPLE_STEP_DP, 0f)
    }

    @Test
    fun meniscusColorAdaptsToDarkAndBrightAccentLuminance() {
        val darkAccent = Color(0xFF252923)
        val brightAccent = Color(0xFFF8F8F8)
        assertEquals(lerp(darkAccent, Color.White, 0.18f).copy(alpha = 0.90f), waveMeniscusColor(darkAccent))
        assertEquals(brightAccent.copy(alpha = 1f), waveMeniscusColor(brightAccent))
        assertEquals(0.90f, waveMeniscusColor(darkAccent).alpha, 0.003f)
        assertEquals(1f, waveMeniscusColor(brightAccent).alpha, 0f)
    }

    @Test
    fun waveCometUsesOuterGlassOrbitAndUnclippedThreeLayerGeometry() {
        assertFalse(WAVE_COMET_STYLE.clipToLiquid)
        assertEquals(110f, WAVE_COMET_STYLE.tailSweepDegrees, 0f)
        assertEquals(6f, WAVE_COMET_STYLE.haloStrokeDp, 0f)
        assertEquals(3f, WAVE_COMET_STYLE.tailStrokeDp, 0f)
        assertEquals(3.5f, WAVE_COMET_STYLE.headRadiusDp, 0f)
        assertEquals(6f, WAVE_COMET_STYLE.glowRadiusDp, 0f)
        assertEquals(49.25f, waveCometOrbitRadius(50f, 1.5f), 0f)
    }

    @Test
    fun capsuleSegmentProfileUsesSixRisingBubblesAndStaticKeepsThemDisabled() {
        val segmented = capsuleAnimationFeatures(ChargingArcAnimation.SEGMENTED_FLOW)
        val static = capsuleAnimationFeatures(ChargingArcAnimation.STATIC)
        assertEquals(6, CAPSULE_BUBBLE_COUNT)
        assertTrue(segmented.risingBubbles)
        assertTrue(segmented.animatedSurface)
        assertFalse(static.risingBubbles)
        assertFalse(static.animatedSurface)
        assertTrue(capsuleAnimationFeatures(ChargingArcAnimation.ENERGY_SWEEP).upwardSweep)
        assertTrue(capsuleAnimationFeatures(ChargingArcAnimation.COMET_HEAD).verticalComet)

        val first = capsuleBubbleSpec(0, 0.2f)
        val later = capsuleBubbleSpec(0, 0.4f)
        assertTrue(later.riseFraction > first.riseFraction)
        assertTrue((0 until CAPSULE_BUBBLE_COUNT).all { index ->
            val bubble = capsuleBubbleSpec(index, 0.35f)
            bubble.radiusDp in CAPSULE_BUBBLE_MIN_RADIUS_DP..CAPSULE_BUBBLE_MAX_RADIUS_DP &&
                bubble.alpha in CAPSULE_BUBBLE_MIN_ALPHA..CAPSULE_BUBBLE_MAX_ALPHA
        })
        assertEquals(1f, capsuleBubbleSurfaceFade(0.72f), 0f)
        assertEquals(0f, capsuleBubbleSurfaceFade(1f), 0f)
        assertEquals(0f, capsuleBubbleRippleProgress(0.72f), 0f)
        assertEquals(1f, capsuleBubbleRippleProgress(1f), 0f)
    }

    @Test
    fun forceAnimateKeepsSweepSegmentsAndCometRunningForEveryThermalLabel() {
        val thermalLabels = listOf("Normal", "Hangat", "Tinggi")
        thermalLabels.forEach { thermal ->
            listOf(ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.SEGMENTED_FLOW, ChargingArcAnimation.COMET_HEAD)
                .forEach { animation ->
                    assertTrue("$thermal / ${animation.name}",
                        shouldAnimateBatteryGauge(isCharging = false, animationStyle = animation, forceAnimate = true))
                }
        }
        assertFalse(shouldAnimateBatteryGauge(isCharging = false, animationStyle = ChargingArcAnimation.STATIC, forceAnimate = true))
    }

    @Test
    fun chargingKeepsSweepSegmentsAndCometRunningForEveryThermalLabel() {
        val thermalLabels = listOf("Normal", "Hangat", "Tinggi")
        thermalLabels.forEach { thermal ->
            listOf(ChargingArcAnimation.ENERGY_SWEEP, ChargingArcAnimation.SEGMENTED_FLOW, ChargingArcAnimation.COMET_HEAD)
                .forEach { animation ->
                    assertTrue("$thermal / ${animation.name}",
                        shouldAnimateBatteryGauge(isCharging = true, animationStyle = animation))
                }
        }
        assertFalse(shouldAnimateBatteryGauge(isCharging = true, animationStyle = ChargingArcAnimation.STATIC))
        assertFalse(shouldAnimateBatteryGauge(isCharging = false, animationStyle = ChargingArcAnimation.COMET_HEAD))
    }
}
