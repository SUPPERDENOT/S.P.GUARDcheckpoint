package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHealthEstimatorTest {

    @Test
    fun estimatesCapacityAndHealthForValidSession() {
        val result = BatteryHealthEstimator.estimate(2_000.0, 20, 60, 5_000)

        assertNotNull(result)
        assertEquals(5_000, result!!.estimatedCapacityMah)
        assertEquals(100f, result.healthPercentage, 0.001f)
    }

    @Test
    fun rejectsChargeWindowBelowFortyPercent() {
        assertNull(BatteryHealthEstimator.estimate(2_000.0, 20, 59, 5_000))
    }

    @Test
    fun rejectsInvalidLevelsAndNonFiniteChargeValues() {
        assertNull(BatteryHealthEstimator.estimate(Double.NaN, 20, 70, 5_000))
        assertNull(BatteryHealthEstimator.estimate(Double.POSITIVE_INFINITY, 20, 70, 5_000))
        assertNull(BatteryHealthEstimator.estimate(2_000.0, -1, 70, 5_000))
        assertNull(BatteryHealthEstimator.estimate(2_000.0, 70, 20, 5_000))
    }

    @Test
    fun rejectsInvalidDesignOrEstimatedCapacity() {
        assertNull(BatteryHealthEstimator.estimate(2_000.0, 20, 70, 999))
        assertNull(BatteryHealthEstimator.estimate(2_000.0, 20, 70, 20_001))
        assertNull(BatteryHealthEstimator.estimate(400.0, 20, 70, 5_000))
    }

    @Test
    fun rejectsHealthOutsidePlausibleRangeInsteadOfClamping() {
        assertNull(BatteryHealthEstimator.estimate(4_000.0, 20, 70, 5_000))
    }

    @Test
    fun validatesStoredSnapshotAgainstItsDesignCapacity() {
        assertTrue(BatteryHealthEstimator.isValidStoredSnapshot(4_500, 5_000, 90f))
        assertFalse(BatteryHealthEstimator.isValidStoredSnapshot(4_500, 5_000, 80f))
        assertFalse(BatteryHealthEstimator.isValidStoredSnapshot(4_500, 0, 90f))
    }
}
