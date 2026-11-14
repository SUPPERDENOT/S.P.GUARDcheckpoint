package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryCurrentNormalizerTest {

    @Test
    fun chargingCurrentIsPositiveRegardlessOfVendorRawSign() {
        assertEquals(1_800, BatteryCurrentNormalizer.normalize(1_800, true))
        assertEquals(1_800, BatteryCurrentNormalizer.normalize(-1_800, true))
        assertEquals(1_800, BatteryCurrentNormalizer.normalize(1_800_000, true))
        assertEquals(1_800, BatteryCurrentNormalizer.normalize(-1_800_000, true))
    }

    @Test
    fun nonChargingCurrentIsNegativeRegardlessOfVendorRawSign() {
        assertEquals(-1_800, BatteryCurrentNormalizer.normalize(1_800, false))
        assertEquals(-1_800, BatteryCurrentNormalizer.normalize(-1_800, false))
    }

    @Test
    fun zeroAndMinimumIntegerAreHandledSafely() {
        assertEquals(0, BatteryCurrentNormalizer.normalize(0, true))
        assertEquals(0, BatteryCurrentNormalizer.normalize(Int.MIN_VALUE, true))
        assertEquals(0, BatteryCurrentNormalizer.normalize(Int.MIN_VALUE, false))
    }
}
