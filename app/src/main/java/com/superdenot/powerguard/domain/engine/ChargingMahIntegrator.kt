package com.superdenot.powerguard.domain.engine

/**
 * Mengintegrasikan arus charging menjadi mAh berdasarkan waktu monotonic aktual.
 * Menggunakan integrasi trapezoidal agar perubahan arus antar-sampel tidak
 * diasumsikan langsung konstan pada nilai sampel terbaru.
 *
 * Sampel pertama hanya menetapkan baseline. Gap terlalu panjang dibuang agar
 * arus lama tidak diekstrapolasi saat loop/service sempat tertahan.
 */
class ChargingMahIntegrator(
    private val maxSampleGapMs: Long = DEFAULT_MAX_SAMPLE_GAP_MS
) {
    private var lastSampleElapsedRealtimeMs: Long? = null
    private var lastPositiveCurrentMa: Int = 0

    init {
        require(maxSampleGapMs > 0L)
    }

    fun addSample(currentMa: Int, elapsedRealtimeMs: Long): Double {
        val previousTime = lastSampleElapsedRealtimeMs
        if (previousTime == null) {
            lastSampleElapsedRealtimeMs = elapsedRealtimeMs
            lastPositiveCurrentMa = currentMa.coerceAtLeast(0)
            return 0.0
        }

        if (elapsedRealtimeMs <= previousTime) return 0.0

        val deltaMs = elapsedRealtimeMs - previousTime
        val positiveCurrentMa = currentMa.coerceAtLeast(0)
        lastSampleElapsedRealtimeMs = elapsedRealtimeMs

        if (deltaMs > maxSampleGapMs) {
            lastPositiveCurrentMa = positiveCurrentMa
            return 0.0
        }

        val averageCurrentMa = (lastPositiveCurrentMa.toDouble() + positiveCurrentMa.toDouble()) / 2.0
        lastPositiveCurrentMa = positiveCurrentMa
        return averageCurrentMa * deltaMs.toDouble() / MILLIS_PER_HOUR
    }

    fun reset() {
        lastSampleElapsedRealtimeMs = null
        lastPositiveCurrentMa = 0
    }

    companion object {
        const val DEFAULT_MAX_SAMPLE_GAP_MS = 5_000L
        private const val MILLIS_PER_HOUR = 3_600_000.0
    }
}
