package com.superdenot.powerguard.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MonitorNotificationPostGateTest {
    @Test
    fun sameChargingStateSuppressesDuplicatePayloadEvenWhenContentChanges() {
        val gate = MonitorNotificationPostGate()
        assertTrue(gate.shouldPost("service-start", "connected", 1_000L))
        assertFalse(gate.shouldPost("periodic-copy-change", "connected", 8_000L))
    }

    @Test
    fun eachStablePowerTransitionCanPostAfterCooldown() {
        val gate = MonitorNotificationPostGate()
        assertTrue(gate.shouldPost("plugged", "connected", 1_000L))
        assertFalse(gate.shouldPost("plugged-again", "connected", 1_050L))
        assertTrue(gate.shouldPost("unplugged", "disconnected", 1_300L))
        assertFalse(gate.shouldPost("unplugged-again", "disconnected", 1_350L))
        assertTrue(gate.shouldPost("reconnected", "connected", 1_600L))
    }

    @Test
    fun rapidOppositeStateBounceIsCoalescedUntilCooldownExpires() {
        val gate = MonitorNotificationPostGate()
        assertTrue(gate.shouldPost("plugged", "connected", 1_000L))
        assertFalse(gate.shouldPost("unplugged-bounce", "disconnected", 1_100L))
        assertTrue(gate.isBounceCoalescing(1_100L))
        assertFalse(gate.shouldPost("replug-bounce", "connected", 1_150L))
        assertTrue(gate.isBounceCoalescing(1_150L))
        assertTrue(gate.shouldPost("stable-unplugged", "disconnected", 1_300L))
        assertFalse(gate.isBounceCoalescing(1_300L))
    }

    @Test
    fun staleSnapshotSequenceIsRejectedBeforePosting() {
        val gate = MonitorNotificationPostGate()
        assertFalse(gate.shouldPost(
            payloadSignature = "stale-snapshot",
            criticalStateSignature = "connected",
            nowElapsedMs = 1_000L,
            snapshotSequence = 4L,
            latestSequence = 5L
        ))
        assertTrue(gate.shouldPost(
            payloadSignature = "latest-snapshot",
            criticalStateSignature = "connected",
            nowElapsedMs = 1_001L,
            snapshotSequence = 5L,
            latestSequence = 5L
        ))
    }

    @Test
    fun concurrentDuplicateTransitionCanOnlyPassGateOnce() {
        val gate = MonitorNotificationPostGate()
        val workers = 16
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        val results = Collections.synchronizedList(mutableListOf<Boolean>())
        val threads = (0 until workers).map {
            Thread {
                ready.countDown()
                start.await()
                results += gate.shouldPost("same-transition", "connected", 2_000L)
            }.apply { start() }
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        start.countDown()
        threads.forEach { it.join(5_000L) }
        assertEquals(1, results.count { it })
    }

    @Test
    fun resetAllowsCurrentStateToBePostedAgain() {
        val gate = MonitorNotificationPostGate()
        assertTrue(gate.shouldPost("payload-A", "connected", 1_000L))
        gate.reset()
        assertTrue(gate.shouldPost("payload-A", "connected", 1_001L))
    }

    @Test
    fun monotonicClockRollbackResetsTransitionMemory() {
        val gate = MonitorNotificationPostGate()
        assertTrue(gate.shouldPost("payload-A", "connected", 2_000L))
        assertTrue(gate.shouldPost("payload-B", "connected", 1_000L))
    }
}
