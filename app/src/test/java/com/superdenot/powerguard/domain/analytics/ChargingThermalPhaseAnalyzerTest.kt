package com.superdenot.powerguard.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingThermalPhaseAnalyzerTest {
    @Test fun mapsSampleDurationIntoThermalZones() {
        val samples = buildList {
            repeat(18) { index -> add(sample(index * 60_000L, 37f, 1_800, 3_700)) }
            repeat(4) { index -> add(sample((18 + index) * 60_000L, 39f, 1_800, 3_720)) }
            repeat(5) { index -> add(sample((22 + index) * 60_000L, 40f, 1_800, 3_740)) }
        }
        val summary = ChargingThermalPhaseAnalyzer.analyze(samples)
        assertEquals(18, summary.safeMinutes)
        assertEquals(4, summary.warmMinutes)
        assertEquals(4, summary.highMinutes)
    }

    @Test fun identifiesCcLikePatternWithoutClaimingProtocol() {
        val samples = (0 until 8).map { index ->
            sample(index * 30_000L, 35f, 2_000 + (index % 2) * 20, 3_700 + index * 15)
        }
        assertEquals(ChargingPhaseIndicator.CC, ChargingThermalPhaseAnalyzer.analyze(samples).phase)
    }

    @Test fun identifiesCvLikePatternWhenVoltageIsStableAndCurrentTapers() {
        val samples = (0 until 8).map { index ->
            sample(index * 30_000L, 39f, 2_000 - index * 180, 4_350 + (index % 2) * 5)
        }
        assertEquals(ChargingPhaseIndicator.CV, ChargingThermalPhaseAnalyzer.analyze(samples).phase)
    }

    @Test fun insufficientOrAmbiguousSamplesDoNotGuessPhase() {
        assertEquals(
            ChargingPhaseIndicator.UNKNOWN,
            ChargingThermalPhaseAnalyzer.analyze(listOf(sample(0L, 35f, 1_000, 3_700))).phase
        )
    }

    private fun sample(time: Long, temp: Float, current: Int, voltage: Int) =
        ChargingThermalSample(time, temp, true, current, true, voltage, true, true, true)
}
