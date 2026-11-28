package com.superdenot.powerguard.ui.analytics

/**
 * Monotonic tile-launch request state. A request is pending only until the
 * matching screen acknowledges it; stale acknowledgements cannot consume a newer request.
 */
internal data class PopupDetectorOpenTrigger(
    val lastIssuedRequestId: Int = 0,
    val consumedRequestId: Int = 0
) {
    val pendingRequestId: Int
        get() = lastIssuedRequestId.takeIf { it > consumedRequestId } ?: 0

    fun issue(): PopupDetectorOpenTrigger =
        copy(lastIssuedRequestId = lastIssuedRequestId + 1)

    fun consume(requestId: Int): PopupDetectorOpenTrigger =
        if (requestId != 0 && requestId == pendingRequestId) {
            copy(consumedRequestId = requestId)
        } else {
            this
        }
}
