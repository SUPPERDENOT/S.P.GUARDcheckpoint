package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class PopupDetectorOpenTriggerTest {
    @Test
    fun consumedTileRequestCannotBeDeliveredAgain() {
        val issued = PopupDetectorOpenTrigger().issue()
        val requestId = issued.pendingRequestId

        assertEquals(1, requestId)
        val consumed = issued.consume(requestId)

        assertEquals(0, consumed.pendingRequestId)
        assertEquals(consumed, consumed.consume(requestId))
        assertEquals(0, consumed.consume(requestId).pendingRequestId)

        val nextLaunch = consumed.issue()
        assertEquals(2, nextLaunch.pendingRequestId)
    }

    @Test
    fun staleAcknowledgementCannotConsumeANewerTileRequest() {
        val firstLaunch = PopupDetectorOpenTrigger().issue()
        val secondLaunch = firstLaunch.issue()

        val afterStaleAcknowledgement = secondLaunch.consume(firstLaunch.pendingRequestId)

        assertEquals(2, afterStaleAcknowledgement.pendingRequestId)
        assertEquals(2, afterStaleAcknowledgement.consume(2).consumedRequestId)
        assertEquals(0, afterStaleAcknowledgement.consume(2).pendingRequestId)
    }
}
