package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryCurrentNormalizerTest {
    @Test
    fun preservesPositiveAndNegativeBatteryCurrentWhileExternalPowerIsConnected() {
        assertEquals(1_800, BatteryCurrentNormalizer.normalize(1_800_000, true))
        assertEquals(-1_800, BatteryCurrentNormalizer.normalize(-1_800_000, true))
        assertEquals(-1, BatteryCurrentNormalizer.normalize(-1_999, true))
    }

    @Test
    fun normalizesDischargingDirectionRegardlessOfVendorRawSign() {
        assertEquals(-1_800, BatteryCurrentNormalizer.normalize(1_800_000, false))
        assertEquals(-1_800, BatteryCurrentNormalizer.normalize(-1_800_000, false))
    }

    @Test
    fun zeroAndMinimumIntegerAreHandledSafely() {
        assertEquals(0, BatteryCurrentNormalizer.normalize(0, true))
        assertEquals(0, BatteryCurrentNormalizer.normalize(Int.MIN_VALUE, true))
        assertEquals(0, BatteryCurrentNormalizer.normalize(Int.MIN_VALUE, false))
    }
}
