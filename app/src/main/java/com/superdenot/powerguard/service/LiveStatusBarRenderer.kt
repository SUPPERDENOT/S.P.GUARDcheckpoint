package com.superdenot.powerguard.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.domain.model.StatusBarParameter
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

internal data class StatusBarReading(val valueText: String, val unitText: String) {
    val displayText: String get() = valueText + unitText
}

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
            StatusBarParameter.BATTERY_PERCENTAGE ->
                StatusBarReading(if (state.isLevelAvailable) formatAdaptive(state.level.toDouble()) else "—", "%")
            StatusBarParameter.RESOURCE_SOURCE -> StatusBarReading(formatResourceSource(state), "")
            StatusBarParameter.CHARGING_STATUS -> StatusBarReading("—", "")
        }

    internal fun formatResourceSource(state: LiveBatteryState): String {
        if (!state.isChargeSourceAvailable) return "—"
        if (!state.isPowerConnected) return "BAT"
        return when (state.chargeSource) {
            "AC Adapter" -> "AC"
            "USB Port" -> "USB"
            "Wireless" -> "WL"
            else -> "—"
        }
    }

    private fun reading(available: Boolean, value: Double, unit: String): StatusBarReading =
        if (available && value.isFinite()) StatusBarReading(formatAdaptive(value), unit)
        else StatusBarReading("—", unit)

    internal fun formatAdaptive(value: Double): String {
        if (!value.isFinite()) return "—"
        val roundedTenth = if (abs(value) < 10.0) (value * 10.0).roundToInt() / 10.0 else value
        return if (abs(roundedTenth) < 10.0) String.format(Locale.US, "%.1f", roundedTenth)
        else String.format(Locale.US, "%.0f", value)
    }

    const val ICON_SIZE_DP = 24
    const val MAX_CACHE_ENTRIES = 32
    const val MAX_VALUE_LENGTH = 4
    private const val MAX_SOURCE_TEXT_LENGTH = 3
    private const val MAX_UNIT_LENGTH = 4
    private const val HORIZONTAL_INSET_DP = 0.5f
    private const val VERTICAL_INSET_DP = 0.5f
    internal const val ROW_GAP_DP = 0.75f
    internal const val VALUE_HEIGHT_SHARE = 0.58f
    internal const val UNIT_HEIGHT_SHARE = 0.42f
    internal const val UNIT_MAX_WIDTH_RATIO = 0.88f
    private val GLYPH_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.BOLD)

    private data class CacheKey(
        val parameter: StatusBarParameter,
        val valueText: String,
        val unitText: String,
        val densityBits: Int
    )

    private val cacheLock = Any()
    private val bitmapCache = object : LinkedHashMap<CacheKey, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Bitmap>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    fun render(parameter: StatusBarParameter, reading: StatusBarReading, density: Float = 1f): Bitmap? {
        val isResourceSource = parameter == StatusBarParameter.RESOURCE_SOURCE
        val value = reading.valueText.trim().takeIf {
            it.isNotEmpty() &&
                it.length <= (if (isResourceSource) MAX_SOURCE_TEXT_LENGTH else MAX_VALUE_LENGTH) &&
                safe(it)
        } ?: return null
        val unit = reading.unitText.trim()
        if (isResourceSource) {
            if (unit.isNotEmpty()) return null
        } else if (unit.isEmpty() || unit.length > MAX_UNIT_LENGTH || !safe(unit)) {
            return null
        }
        val d = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val key = CacheKey(parameter, value, unit, d.toBits())
        return synchronized(cacheLock) {
            bitmapCache[key] ?: renderBitmap(parameter, value, unit, d).also { bitmapCache[key] = it }
        }
    }

    private fun safe(text: String) = text.none { it.code < 0x20 || it.code in 0x7f..0x9f }

    private fun renderBitmap(parameter: StatusBarParameter, value: String, unit: String, density: Float): Bitmap {
        val size = (ICON_SIZE_DP * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val horizontalInset = HORIZONTAL_INSET_DP * density
        val verticalInset = VERTICAL_INSET_DP * density
        val maxWidth = (size - 2f * horizontalInset).coerceAtLeast(1f)

        if (parameter == StatusBarParameter.RESOURCE_SOURCE) {
            val maxHeight = (size - 2f * verticalInset).coerceAtLeast(1f)
            val paint = fittedPaint(value, maxHeight, maxWidth)
            val bounds = textBounds(value, paint)
            val baseline = size / 2f - (bounds.top + bounds.bottom) / 2f
            canvas.drawText(value, size / 2f, baseline, paint)
            return bitmap
        }

        val rowGap = ROW_GAP_DP * density
        val contentHeight = (size - 2f * verticalInset - rowGap).coerceAtLeast(1f)
        val valueMaxHeight = contentHeight * VALUE_HEIGHT_SHARE
        val unitMaxHeight = contentHeight * UNIT_HEIGHT_SHARE
        val unitMaxWidth = min(maxWidth, size * UNIT_MAX_WIDTH_RATIO)
        val valuePaint = fittedPaint(value, valueMaxHeight, maxWidth)
        val unitPaint = fittedPaint(unit, unitMaxHeight, unitMaxWidth)
        val valueBounds = textBounds(value, valuePaint)
        val unitBounds = textBounds(unit, unitPaint)
        val valueBaseline = verticalInset - valueBounds.top
        val unitBaseline = size - verticalInset - unitBounds.bottom
        canvas.drawText(value, size / 2f, valueBaseline, valuePaint)
        canvas.drawText(unit, size / 2f, unitBaseline, unitPaint)
        return bitmap
    }

    private fun fittedPaint(text: String, maxHeight: Float, maxWidth: Float): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = GLYPH_TYPEFACE
            textSize = 100f
        }
        val bounds = textBounds(text, paint)
        val measuredWidth = paint.measureText(text)
        val heightScale = if (bounds.height() > 0) maxHeight / bounds.height() else 1f
        val widthScale = if (measuredWidth > 0f) maxWidth / measuredWidth else 1f
        paint.textSize = 100f * min(heightScale, widthScale)
        return paint
    }

    private fun textBounds(text: String, paint: Paint): Rect =
        Rect().also { paint.getTextBounds(text, 0, text.length, it) }

    internal fun clearCacheForTesting() {
        synchronized(cacheLock) { bitmapCache.clear() }
    }

    internal fun cacheSizeForTesting(): Int = synchronized(cacheLock) { bitmapCache.size }
}
