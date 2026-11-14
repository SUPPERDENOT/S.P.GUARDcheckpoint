package com.superdenot.powerguard.domain.engine

/**
 * Mengintegrasikan arus charging menjadi mAh berdasarkan waktu monotonic aktual.
 *
 * Sampel pertama hanya menetapkan baseline. Gap yang terlalu panjang dibuang agar
 * arus lama tidak diekstrapolasi saat loop/service sempat tertahan.
 */
class ChargingMahIntegrator(
    private val maxSampleGapMs: Long = DEFAULT_MAX_SAMPLE_GAP_MS
) {
    private var lastSampleElapsedRealtimeMs: Long? = null

    init {
        require(maxSampleGapMs > 0L)
    }

    fun addSample(currentMa: Int, elapsedRealtimeMs: Long): Double {
        val previous = lastSampleElapsedRealtimeMs
        lastSampleElapsedRealtimeMs = elapsedRealtimeMs

        if (previous == null || elapsedRealtimeMs <= previous) return 0.0
        val deltaMs = elapsedRealtimeMs - previous
        if (deltaMs > maxSampleGapMs || currentMa <= 0) return 0.0

        return currentMa.toDouble() * deltaMs.toDouble() / MILLIS_PER_HOUR
    }

    fun reset() {
        lastSampleElapsedRealtimeMs = null
    }

    companion object {
        const val DEFAULT_MAX_SAMPLE_GAP_MS = 5_000L
        private const val MILLIS_PER_HOUR = 3_600_000.0
    }
}
