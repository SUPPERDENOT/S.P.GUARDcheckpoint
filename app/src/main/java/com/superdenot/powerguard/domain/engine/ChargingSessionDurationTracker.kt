package com.superdenot.powerguard.domain.engine

/**
 * Mengukur durasi sesi memakai jam monotonic agar perubahan jam sistem dan jeda loop
 * tidak membuat durasi bertambah berdasarkan jumlah iterasi.
 */
class ChargingSessionDurationTracker {
    private var baselineElapsedRealtimeMs: Long? = null
    private var baselineDurationSec: Long = 0L
    private var lastReportedDurationSec: Long = 0L

    fun startOrResume(elapsedRealtimeMs: Long, persistedDurationSec: Long) {
        baselineElapsedRealtimeMs = elapsedRealtimeMs.coerceAtLeast(0L)
        baselineDurationSec = persistedDurationSec.coerceAtLeast(0L)
        lastReportedDurationSec = baselineDurationSec
    }

    fun elapsedSeconds(elapsedRealtimeMs: Long): Long {
        val baseline = baselineElapsedRealtimeMs ?: return lastReportedDurationSec
        val deltaMs = (elapsedRealtimeMs - baseline).coerceAtLeast(0L)
        val calculated = baselineDurationSec + deltaMs / 1_000L
        lastReportedDurationSec = maxOf(lastReportedDurationSec, calculated)
        return lastReportedDurationSec
    }

    fun reset() {
        baselineElapsedRealtimeMs = null
        baselineDurationSec = 0L
        lastReportedDurationSec = 0L
    }
}
