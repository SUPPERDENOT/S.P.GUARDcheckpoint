package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnomalyEvaluatorTest {
    private fun session(durationMs: Long = 120_000L, startLevel: Int = 20, endLevel: Int = 21,
        mah: Double = 100.0, temperature: Float = 0f, hasTemperature: Boolean = false,
        reason: String? = null, isAnomaly: Boolean = false) = ChargingSessionEntity(
        startTime = 1_000L, endTime = 1_000L + durationMs, startLevel = startLevel, endLevel = endLevel,
        totalMahDelivered = mah, maxTempCelsius = temperature, chargerType = "AC Adapter",
        hasTemperatureData = hasTemperature, anomalyReason = reason, isAnomaly = isAnomaly
    )

    @Test fun mahPerPercentUsesStrictGreaterThan120Threshold() {
        assertTrue(SessionAnomaly.CHARGE_SPIKE in SessionAnomalyEvaluator.evaluate(session(mah = 120.1), 21, 121_000L))
        assertTrue(SessionAnomaly.CHARGE_SPIKE !in SessionAnomalyEvaluator.evaluate(session(mah = 120.0), 21, 121_000L))
    }
    @Test fun zeroGainMahUsesStrictGreaterThan250Threshold() {
        assertTrue(SessionAnomaly.CHARGE_SPIKE !in SessionAnomalyEvaluator.evaluate(session(startLevel = 20, endLevel = 20, mah = 250.0), 20, 121_000L))
        assertTrue(SessionAnomaly.CHARGE_SPIKE in SessionAnomalyEvaluator.evaluate(session(startLevel = 20, endLevel = 20, mah = 250.1), 20, 121_000L))
    }
    @Test fun temperatureAt40CIsFlaggedWhenAvailable() {
        assertTrue(SessionAnomaly.HIGH_TEMPERATURE in SessionAnomalyEvaluator.evaluate(session(temperature = 40f, hasTemperature = true), 21, 121_000L))
        assertTrue(SessionAnomaly.HIGH_TEMPERATURE !in SessionAnomalyEvaluator.evaluate(session(), 21, 121_000L))
    }
    @Test fun zeroGainNeedsAtLeast120Seconds() {
        assertTrue(SessionAnomaly.ZERO_PERCENT_GAIN !in SessionAnomalyEvaluator.evaluate(session(durationMs = 119_000L, startLevel = 20, endLevel = 20), 20, 120_000L))
        assertTrue(SessionAnomaly.ZERO_PERCENT_GAIN in SessionAnomalyEvaluator.evaluate(session(startLevel = 20, endLevel = 20), 20, 121_000L))
    }
    @Test fun missingEndLevelDoesNotBecomeAFalseZeroGainAnomaly() {
        val unavailable = session().copy(endLevel = null)
        assertTrue(SessionAnomalyEvaluator.evaluate(unavailable, null, 121_000L).isEmpty())
    }
    @Test fun recoveryMarkerDoesNotChangeEngineAnomalyFlag() {
        val recovered = session(reason = SessionAnomalyEvaluator.RECOVERY_MARKER, isAnomaly = false)
        assertEquals(listOf(SessionAnomaly.RECOVERY), SessionAnomalyEvaluator.evaluate(recovered, 21, 121_000L))
        assertTrue(!recovered.isAnomaly)
    }
    @Test fun genericRecordedBadgeIsHiddenWhenSpecificAnomalyExists() {
        val result = SessionAnomalyEvaluator.evaluate(
            session(mah = 300.0, isAnomaly = true), 21, 121_000L
        )
        assertTrue(SessionAnomaly.CHARGE_SPIKE in result)
        assertTrue(SessionAnomaly.RECORDED !in result)
    }

    @Test fun genericRecordedBadgeRemainsWhenNoSpecificAnomalyExists() {
        assertEquals(
            listOf(SessionAnomaly.RECORDED),
            SessionAnomalyEvaluator.evaluate(session(reason = "SLOW_CHARGING", isAnomaly = true), 21, 121_000L)
        )
    }
}