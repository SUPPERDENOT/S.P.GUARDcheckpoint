package com.superdenot.powerguard.domain.analytics

object SessionMicroFilter {
    const val MICRO_SESSION_LIMIT_SECONDS = 60L
    const val MAX_ZERO_DELTA_CHARGE_MAH = 0.5

    fun shouldHide(
        durationSeconds: Long,
        deltaSoc: Int,
        chargedMah: Double
    ): Boolean {
        return durationSeconds < MICRO_SESSION_LIMIT_SECONDS ||
            (deltaSoc == 0 && chargedMah.isFinite() && chargedMah <= MAX_ZERO_DELTA_CHARGE_MAH)
    }
}
