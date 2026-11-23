package com.superdenot.powerguard.service
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.domain.model.StatusBarParameter
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
internal data class StatusBarReading(val valueText: String, val unitText: String) {
    val displayText: String get() = valueText + unitText
}
/** Dedicated Status Bar Indicator renderer; Monitoring Notification keeps its own drawable. */
internal object LiveStatusBarRenderer {
    fun format(state: LiveBatteryState, parameter: StatusBarParameter): StatusBarReading =
        when (parameter) {
            StatusBarParameter.POWER -> reading(state.isPowerAvailable, state.powerWatt.toDouble(), "W")
            StatusBarParameter.CURRENT -> {
                val ma = state.currentMa
                if (abs(ma.toLong()) >= 1000L) reading(state.isCurrentAvailable, ma.toDouble() / 1000.0, "A")
                else reading(state.isCurrentAvailable, ma.toDouble(), "mA")
            }
            StatusBarParameter.VOLTAGE -> reading(state.isVoltageAvailable, state.voltageMv.toDouble() / 1000.0, "V")
            StatusBarParameter.TEMPERATURE -> reading(state.isTemperatureAvailable, state.tempCelsius.toDouble(), "°C")
            StatusBarParameter.BATTERY_PERCENTAGE -> StatusBarReading(if (state.isLevelAvailable) formatAdaptive(state.level.toDouble()) else "—", "%")
            StatusBarParameter.CHARGING_STATUS -> StatusBarReading("—", "")
        }
    private fun reading(available: Boolean, value: Double, unit: String): StatusBarReading =
        if (available && value.isFinite()) StatusBarReading(formatAdaptive(value), unit) else StatusBarReading("—", unit)
    internal fun formatAdaptive(value: Double): String {
        if (!value.isFinite()) return "—"
        return if (abs(value) < 10.0) String.format(Locale.US, "%.1f", value)
        else String.format(Locale.US, "%.0f", value)
    }
    const val ICON_SIZE_DP = 48
    const val MAX_CACHE_ENTRIES = 32
    private const val VALUE_TEXT_SIZE_DP = 22f
    private const val UNIT_TEXT_SIZE_DP = 11f
    private const val VALUE_REGION_RATIO = 0.62f
    private const val UNIT_REGION_RATIO = 0.35f
    private const val MAX_VALUE_LENGTH = 8
    private const val MAX_UNIT_LENGTH = 4
    private data class CacheKey(val parameter: StatusBarParameter, val valueText: String, val unitText: String, val densityBits: Int)
    private val cacheLock = Any()
    private val bitmapCache = object : LinkedHashMap<CacheKey, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Bitmap>?): Boolean = size > MAX_CACHE_ENTRIES
    }
    fun render(parameter: StatusBarParameter, reading: StatusBarReading, density: Float = 1f): Bitmap? {
        val value = reading.valueText.trim().takeIf { it.isNotEmpty() && it.length <= MAX_VALUE_LENGTH && safe(it) } ?: return null
        val unit = reading.unitText.trim().takeIf { it.isNotEmpty() && it.length <= MAX_UNIT_LENGTH && safe(it) } ?: return null
        val d = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val key = CacheKey(parameter, value, unit, d.toBits())
        return synchronized(cacheLock) { bitmapCache[key] ?: renderBitmap(value, unit, d).also { bitmapCache[key] = it } }
    }
    private fun safe(text: String) = text.none { it.code < 0x20 || it.code in 0x7f..0x9f }
    private fun renderBitmap(value: String, unit: String, density: Float): Bitmap {
        val size = (ICON_SIZE_DP * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val width = size - 2f * density
        drawLine(canvas, value, paint(VALUE_TEXT_SIZE_DP * density), center, size * (VALUE_REGION_RATIO / 2f), width)
        drawLine(canvas, unit, paint(UNIT_TEXT_SIZE_DP * density), center, size * (VALUE_REGION_RATIO + UNIT_REGION_RATIO / 2f), width)
        return bitmap
    }
    private fun paint(size: Float) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = size
    }
    private fun drawLine(canvas: Canvas, text: String, p: Paint, x: Float, y: Float, maxWidth: Float) {
        val fm = p.fontMetrics
        val baseline = y - (fm.ascent + fm.descent) / 2f
        val measured = p.measureText(text)
        val scale = if (measured > maxWidth && measured > 0f) maxWidth / measured else 1f
        canvas.save(); canvas.scale(scale, 1f, x, y); canvas.drawText(text, x, baseline, p); canvas.restore()
    }
    internal fun clearCacheForTesting() { synchronized(cacheLock) { bitmapCache.clear() } }
    internal fun cacheSizeForTesting(): Int = synchronized(cacheLock) { bitmapCache.size }
}
