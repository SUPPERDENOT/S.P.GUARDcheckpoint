package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class BatteryHealthEstimatorTest {
    private fun session(
        start: Int = 20,
        end: Int = 40,
        mah: Double = 1_000.0,
        endTime: Long? = 600_000L
    ) = ChargingSessionEntity(
        startTime = 0L,
        endTime = endTime,
        startLevel = start,
        endLevel = end,
        totalMahDelivered = mah,
        chargerType = "AC Adapter"
    )

    @Test
    fun estimatesEffectiveCapacityAndSohFromValidCompletedSession() {
        val result = BatteryHealthEstimator.estimate(listOf(session()), 5_000)
        assertNotNull(result)
        assertEquals(5_000, result!!.effectiveCapacityMah)
        assertEquals(100f, result.healthPercentage, 0.001f)
        assertEquals(1, result.validSessionCount)
    }

    @Test
    fun acceptsExactlyFifteenPercentSocGain() {
        val result = BatteryHealthEstimator.estimate(listOf(session(start = 20, end = 35, mah = 750.0)))
        assertNotNull(result)
        assertEquals(5_000, result!!.effectiveCapacityMah)
    }

    @Test
    fun rejectsSocGainBelowFifteenPercentToAvoidBiasedEstimate() {
        assertNull(BatteryHealthEstimator.estimate(listOf(session(start = 20, end = 34, mah = 700.0))))
    }

    @Test
    fun ignoresOpenOrInvalidSessions() {
        val result = BatteryHealthEstimator.estimate(
            listOf(
                session(endTime = null),
                session(start = 80, end = 20),
                session(mah = Double.NaN),
                session(endTime = 0L)
            )
        )
        assertNull(result)
    }

    @Test
    fun usesMedianAcrossValidSessionsToReduceOutlierInfluence() {
        val result = BatteryHealthEstimator.estimate(
            sessions = listOf(
                session(mah = 1_000.0),
                session(mah = 1_100.0),
                session(mah = 2_000.0)
            ),
            designCapacityMah = 5_500
        )
        assertNotNull(result)
        assertEquals(5_500, result!!.effectiveCapacityMah)
        assertEquals(3, result.validSessionCount)
    }

    @Test
    fun rejectsSessionsShorterThanOneMinute() {
        assertNull(BatteryHealthEstimator.estimate(listOf(session(endTime = 59_000L))))
    }

    @Test
    fun rejectsInvalidDesignCapacity() {
        assertNull(BatteryHealthEstimator.estimate(listOf(session()), 999))
    }
}
