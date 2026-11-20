package com.superdenot.powerguard.service

/**
 * Serial gate for foreground notification ID 1001.
 *
 * It suppresses duplicate payloads, coalesces rapid power-state bounce for 250 ms,
 * and rejects snapshots superseded while they are being built. Normal metric refreshes
 * remain possible after a stable state; they do not extend the transition cooldown.
 */
internal class MonitorNotificationPostGate(
    private val transitionCooldownMs: Long = DEFAULT_TRANSITION_COOLDOWN_MS
) {
    init {
        require(transitionCooldownMs >= 0L) { "Transition cooldown must not be negative" }
    }

    private var lastCriticalStateSignature: String? = null
    private var lastPayloadSignature: String? = null
    private var lastTransitionElapsedMs: Long? = null
    private var pendingBounceSignature: String? = null

    @Synchronized
    fun shouldPost(
        payloadSignature: String,
        criticalStateSignature: String,
        nowElapsedMs: Long,
        snapshotSequence: Long = 0L,
        latestSequence: Long = snapshotSequence,
        allowPayloadRefresh: Boolean = false
    ): Boolean {
        require(payloadSignature.isNotBlank()) { "Payload signature must not be blank" }
        require(nowElapsedMs >= 0L) { "Elapsed realtime must not be negative" }
        if (snapshotSequence < latestSequence) return false

        var previousTransition = lastTransitionElapsedMs
        if (previousTransition != null && nowElapsedMs < previousTransition) {
            resetInternal()
            previousTransition = null
        }

        if (criticalStateSignature == lastCriticalStateSignature) {
            if (pendingBounceSignature != null && previousTransition != null &&
                nowElapsedMs - previousTransition < transitionCooldownMs
            ) {
                return false
            }
            pendingBounceSignature = null
            if (allowPayloadRefresh && payloadSignature != lastPayloadSignature) {
                lastPayloadSignature = payloadSignature
                return true
            }
            return false
        }

        if (previousTransition != null &&
            nowElapsedMs - previousTransition < transitionCooldownMs
        ) {
            pendingBounceSignature = criticalStateSignature
            return false
        }

        lastCriticalStateSignature = criticalStateSignature
        lastPayloadSignature = payloadSignature
        lastTransitionElapsedMs = nowElapsedMs
        pendingBounceSignature = null
        return true
    }

    @Synchronized
    fun isBounceCoalescing(nowElapsedMs: Long): Boolean {
        val previous = lastTransitionElapsedMs ?: return false
        if (nowElapsedMs < previous) return false
        return pendingBounceSignature != null &&
            nowElapsedMs - previous < transitionCooldownMs
    }

    @Synchronized
    fun reset() = resetInternal()

    private fun resetInternal() {
        lastCriticalStateSignature = null
        lastPayloadSignature = null
        lastTransitionElapsedMs = null
        pendingBounceSignature = null
    }

    private companion object {
        const val DEFAULT_TRANSITION_COOLDOWN_MS = 250L
    }
}
