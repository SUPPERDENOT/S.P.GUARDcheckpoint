package com.superdenot.powerguard.service

/**
 * Serial transition gate for foreground notification ID 1001.
 * It suppresses duplicate power-state edges and coalesces rapid opposite-state
 * bounce for 250 ms. The existing render gate remains responsible for stable
 * metric payload refreshes and its body throttle.
 */
internal class MonitorNotificationPostGate(
    private val transitionCooldownMs: Long = DEFAULT_TRANSITION_COOLDOWN_MS
) {
    init {
        require(transitionCooldownMs >= 0L) { "Transition cooldown must not be negative" }
    }

    private var lastCriticalStateSignature: String? = null
    private var lastTransitionElapsedMs: Long? = null
    private var pendingBounceSignature: String? = null

    @Synchronized
    fun shouldPost(
        payloadSignature: String,
        criticalStateSignature: String,
        nowElapsedMs: Long,
        snapshotSequence: Long = 0L,
        latestSequence: Long = snapshotSequence
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
            return false
        }

        if (previousTransition != null &&
            nowElapsedMs - previousTransition < transitionCooldownMs
        ) {
            pendingBounceSignature = criticalStateSignature
            return false
        }

        lastCriticalStateSignature = criticalStateSignature
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
        lastTransitionElapsedMs = null
        pendingBounceSignature = null
    }

    private companion object {
        const val DEFAULT_TRANSITION_COOLDOWN_MS = 250L
    }
}
