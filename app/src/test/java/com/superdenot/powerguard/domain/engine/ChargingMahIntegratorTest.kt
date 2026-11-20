package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingMahIntegratorTest {
    @Test fun integratesValidAdjacentSamples() {
        val integrator = ChargingMahIntegrator()
        assertEquals(0.0, integrator.addSample(1_000, 1_000), 0.0)
        assertEquals(1_000.0 / 3_600.0, integrator.addSample(1_000, 2_000), 0.000001)
    }

    @Test fun unavailableSampleBreaksIntegrationBaseline() {
        val integrator = ChargingMahIntegrator()
        integrator.addSample(2_000, 1_000)
        assertEquals(0.0, integrator.addSample(0, 2_000, false), 0.0)
        assertEquals(0.0, integrator.addSample(2_000, 10_000), 0.0)
        assertEquals(2_000.0 / 3_600.0, integrator.addSample(2_000, 11_000), 0.000001)
    }

    @Test fun longGapDoesNotIntegrateAcrossGap() {
        val integrator = ChargingMahIntegrator(1_000)
        integrator.addSample(1_000, 1_000)
        assertEquals(0.0, integrator.addSample(1_000, 3_000), 0.0)
        assertEquals(1_000.0 * 500 / 3_600_000.0,
            integrator.addSample(1_000, 3_500), 0.000001)
    }
}
