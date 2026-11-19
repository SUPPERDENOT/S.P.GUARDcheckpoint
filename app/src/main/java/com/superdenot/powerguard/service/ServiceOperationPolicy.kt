package com.superdenot.powerguard.service

/** Runtime mode derived from the two user toggles and the data-durability guard. */
internal enum class ServiceOperationMode {
    STOPPED,
    HUD_ONLY,
    MONITORING_STANDBY,
    MONITORING_ACTIVE,
    RECOVERY_HOLD
}

internal object ServiceOperationPolicy {
    fun shouldKeepServiceAlive(
        isMonitoringEnabled: Boolean,
        isNotifBarEnabled: Boolean,
        isRecoveryHoldRequired: Boolean
    ): Boolean = isMonitoringEnabled || isNotifBarEnabled || isRecoveryHoldRequired

    fun resolveMode(
        isMonitoringEnabled: Boolean,
        isNotifBarEnabled: Boolean,
        isRecoveryHoldRequired: Boolean,
        isCharging: Boolean = true
    ): ServiceOperationMode = when {
        isRecoveryHoldRequired -> ServiceOperationMode.RECOVERY_HOLD
        isMonitoringEnabled && isCharging -> ServiceOperationMode.MONITORING_ACTIVE
        isMonitoringEnabled && isNotifBarEnabled -> ServiceOperationMode.HUD_ONLY
        isMonitoringEnabled -> ServiceOperationMode.MONITORING_STANDBY
        isNotifBarEnabled -> ServiceOperationMode.HUD_ONLY
        else -> ServiceOperationMode.STOPPED
    }

    fun mayRecordRoom(
        mode: ServiceOperationMode,
        isCharging: Boolean,
        hasActiveSession: Boolean
    ): Boolean = mode == ServiceOperationMode.MONITORING_ACTIVE && isCharging && hasActiveSession

    fun shouldFinalizeSessionOnMonitoringDisable(
        wasMonitoringEnabled: Boolean,
        isMonitoringEnabled: Boolean,
        hasActiveSession: Boolean
    ): Boolean = wasMonitoringEnabled && !isMonitoringEnabled && hasActiveSession
}

/**
 * Suppresses identical foreground-notification renders and throttles body-only changes.
 * Icon changes are never delayed; settings changes can force an immediate refresh.
 */
internal class PersistentNotificationRenderGate(
    private val bodyThrottleMs: Long = 4_000L
) {
    data class Decision(
        val shouldNotify: Boolean,
        val body: String,
        val signature: String
    )

    private var lastRenderSignature: String? = null
    private var lastBody: String? = null
    private var lastBodyUpdateElapsedRealtime: Long = Long.MIN_VALUE

    fun evaluate(
        iconKey: String,
        title: String,
        requestedBody: String,
        nowElapsedRealtime: Long,
        force: Boolean = false
    ): Decision {
        val bodyChanged = requestedBody != lastBody
        val bodyIsThrottled = bodyChanged &&
            lastBody != null &&
            nowElapsedRealtime - lastBodyUpdateElapsedRealtime < bodyThrottleMs
        val effectiveBody = if (bodyIsThrottled) lastBody!! else requestedBody
        val signature = "$iconKey|$title|$effectiveBody"
        val shouldNotify = force || signature != lastRenderSignature

        if (shouldNotify) {
            lastRenderSignature = signature
            if (effectiveBody != lastBody) {
                lastBody = effectiveBody
                lastBodyUpdateElapsedRealtime = nowElapsedRealtime
            }
        }
        return Decision(shouldNotify, effectiveBody, signature)
    }

    fun reset() {
        lastRenderSignature = null
        lastBody = null
        lastBodyUpdateElapsedRealtime = Long.MIN_VALUE
    }

    internal fun lastRenderSignatureForTesting(): String? = lastRenderSignature
}
