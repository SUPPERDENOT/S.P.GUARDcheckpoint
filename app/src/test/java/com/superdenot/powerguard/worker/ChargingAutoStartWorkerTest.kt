package com.superdenot.powerguard.worker

import com.superdenot.powerguard.service.AutoStartRequestResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingAutoStartWorkerTest {

    @Test
    fun acceptedOrAlreadyActiveRequestsCompleteCurrentWork() {
        assertEquals(
            ChargingAutoStartWorkOutcome.COMPLETE,
            AutoStartRequestResult.REQUESTED.toChargingAutoStartWorkOutcome()
        )
        assertEquals(
            ChargingAutoStartWorkOutcome.COMPLETE,
            AutoStartRequestResult.ALREADY_IN_FLIGHT.toChargingAutoStartWorkOutcome()
        )
        assertEquals(
            ChargingAutoStartWorkOutcome.COMPLETE,
            AutoStartRequestResult.SERVICE_ALREADY_ACTIVE.toChargingAutoStartWorkOutcome()
        )
    }

    @Test
    fun disabledOrNotChargingRequestsCompleteCurrentWork() {
        assertEquals(
            ChargingAutoStartWorkOutcome.COMPLETE,
            AutoStartRequestResult.MONITORING_DISABLED.toChargingAutoStartWorkOutcome()
        )
        assertEquals(
            ChargingAutoStartWorkOutcome.COMPLETE,
            AutoStartRequestResult.NOT_CHARGING.toChargingAutoStartWorkOutcome()
        )
    }


    @Test
    fun oneTimeWorkIsArmedOnlyForConfirmedUnpluggedState() {
        assertEquals(true, shouldArmChargingOneTimeWork(0))
        assertEquals(false, shouldArmChargingOneTimeWork(null))
        assertEquals(false, shouldArmChargingOneTimeWork(-1))
        assertEquals(false, shouldArmChargingOneTimeWork(1))
        assertEquals(false, shouldArmChargingOneTimeWork(2))
    }

    @Test
    fun platformDeferralOrFailureRequestsRetry() {
        assertEquals(
            ChargingAutoStartWorkOutcome.RETRY,
            AutoStartRequestResult.DEFERRED_BY_PLATFORM.toChargingAutoStartWorkOutcome()
        )
        assertEquals(
            ChargingAutoStartWorkOutcome.RETRY,
            AutoStartRequestResult.FAILED.toChargingAutoStartWorkOutcome()
        )
    }
}
