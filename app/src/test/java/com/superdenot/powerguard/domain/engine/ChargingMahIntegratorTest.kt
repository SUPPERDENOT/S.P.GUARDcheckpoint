package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingMahIntegratorTest {

    @Test
    fun integratesUsingActualElapsedMilliseconds() {
        val integrator = ChargingMahIntegrator()

        assertEquals(0.0, integrator.addSample(1_800, 10_000L), 0.000001)
        assertEquals(0.5, integrator.addSample(1_800, 11_000L), 0.000001)
        assertEquals(0.75, integrator.addSample(1_800, 12_500L), 0.000001)
    }

    @Test
    fun usesTrapezoidalIntegrationWhenCurrentChangesBetweenSamples() {
        val integrator = ChargingMahIntegrator()

        integrator.addSample(1_800, 1_000L)
        assertEquals(0.75, integrator.addSample(3_600, 2_000L), 0.000001)
        assertEquals(1.25, integrator.addSample(3_600, 3_000L), 0.000001)
    }

    @Test
    fun ignoresNonPositiveCurrentWithoutSubtractingDeliveredCharge() {
        val integrator = ChargingMahIntegrator()

        integrator.addSample(1_500, 1_000L)
        assertEquals(0.0, integrator.addSample(-1_500, 2_000L), 0.000001)
        assertEquals(0.0, integrator.addSample(0, 3_000L), 0.000001)
        assertEquals(0.5, integrator.addSample(1_800, 4_000L), 0.000001)
    }

    @Test
    fun ignoresNonMonotonicAndExcessivelyLongSampleGaps() {
        val integrator = ChargingMahIntegrator()

        integrator.addSample(2_000, 10_000L)
        assertEquals(0.0, integrator.addSample(2_000, 9_000L), 0.000001)
        assertEquals(0.0, integrator.addSample(2_000, 15_001L), 0.000001)
        assertEquals(2_000.0 / 3_600.0, integrator.addSample(2_000, 16_001L), 0.000001)
    }

    @Test
    fun resetMakesNextSampleANewBaseline() {
        val integrator = ChargingMahIntegrator()

        integrator.addSample(1_800, 1_000L)
        integrator.addSample(1_800, 2_000L)
        integrator.reset()

        assertEquals(0.0, integrator.addSample(1_800, 100_000L), 0.000001)
        assertEquals(0.5, integrator.addSample(1_800, 101_000L), 0.000001)
    }
}
