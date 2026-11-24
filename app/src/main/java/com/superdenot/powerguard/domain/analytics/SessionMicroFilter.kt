package com.superdenot.powerguard.domain.analytics

object SessionMicroFilter {
    const val MICRO_SESSION_LIMIT_SECONDS = 60L

    fun shouldHide(
        durationSeconds: Long,
        @Suppress("UNUSED_PARAMETER") deltaSoc: Int,
        @Suppress("UNUSED_PARAMETER") chargedMah: Double
    ): Boolean = durationSeconds < MICRO_SESSION_LIMIT_SECONDS
}