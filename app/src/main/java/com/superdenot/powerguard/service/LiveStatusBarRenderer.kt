package com.superdenot.powerguard.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.superdenot.powerguard.domain.model.StatusBarParameter
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Renders a compact, monochrome glyph suitable for a notification small icon.
 *
 * Dimensions and typography are density-scaled, following the proven Wattz status-icon
 * approach. SystemUI still controls the final status-bar slot size.
 */
internal object LiveStatusBarRenderer {

    fun format(state: com.superdenot.powerguard.domain.model.LiveBatteryState, parameter: StatusBarParameter): String {
        return when (parameter) {
            StatusBarParameter.TEMPERATURE -> if (state.isTemperatureAvailable) "${state.tempCelsius.roundToInt()}°" else "N/A"
            StatusBarParameter.VOLTAGE -> if (state.isVoltageAvailable) String.format(Locale.US, "%.1fV", state.voltageMv / 1000f) else "N/A"
            StatusBarParameter.CURRENT -> if (state.isCurrentAvailable) String.format(Locale.US, "%.1fA", state.currentMa / 1000f) else "N/A"
            StatusBarParameter.POWER -> if (state.isPowerAvailable) "${state.powerWatt.roundToInt()}W" else "N/A"
            StatusBarParameter.BATTERY_PERCENTAGE -> if (state.isLevelAvailable) "${state.level}%" else "N/A"
            StatusBarParameter.CHARGING_STATUS -> if (state.isChargingStatusAvailable) {
                if (state.isCharging) "CHG" else "DIS"
            } else "N/A"
        }
    }

    const val ICON_SIZE_DP = 48
    const val MAX_CACHE_ENTRIES = 32
    private const val HORIZONTAL_PADDING_DP = 3f
    private const val TARGET_TEXT_WIDTH_DP = 40f
    private const val BASE_TEXT_SIZE_DP = 28f
    private const val MIN_TEXT_SIZE_DP = 10f
    private const val MAX_TEXT_LENGTH = 8

    private data class CacheKey(val text: String, val densityBits: Int)

    private val cacheLock = Any()
    private val bitmapCache = object : LinkedHashMap<CacheKey, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Bitmap>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /**
     * Returns null for unavailable/blank input. Density is supplied by the Android display
     * metrics so the bitmap's physical dimensions and text scale together on high-density screens.
     */
    fun render(displayText: String?, density: Float = 1f): Bitmap? {
        val text = displayText?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_TEXT_LENGTH && it.none { char -> char.code < 0x20 || char.code in 0x7f..0x9f } }
            ?: return null
        val safeDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val key = CacheKey(text, safeDensity.toBits())

        return synchronized(cacheLock) {
            bitmapCache[key] ?: renderBitmap(text, safeDensity).also { bitmapCache[key] = it }
        }
    }

    private fun renderBitmap(text: String, density: Float): Bitmap {
        val sizePx = (ICON_SIZE_DP * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val horizontalPadding = HORIZONTAL_PADDING_DP * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = BASE_TEXT_SIZE_DP * density
        }

        val maxTextWidth = sizePx - (horizontalPadding * 2f)
        val measuredWidth = paint.measureText(text)
        if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
            paint.textSize = (paint.textSize * maxTextWidth / measuredWidth)
                .coerceAtLeast(MIN_TEXT_SIZE_DP * density)
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val verticalPadding = density
        val availableHeight = sizePx - (verticalPadding * 2f)
        if (bounds.height() > availableHeight && bounds.height() > 0) {
            paint.textSize = (paint.textSize * availableHeight / bounds.height())
                .coerceAtLeast(MIN_TEXT_SIZE_DP * density)
        }

        val baseline = (sizePx / 2f) - (paint.ascent() + paint.descent()) / 2f
        // Normalize short labels (e.g. CHG/DIS and compact power) to a similar visual width.
        // Long readings still shrink by font size first, preserving the full value.
        val targetWidth = minOf(TARGET_TEXT_WIDTH_DP * density, maxTextWidth)
        val finalWidth = paint.measureText(text)
        val horizontalScale = if (finalWidth > 0f) {
            (targetWidth / finalWidth).coerceIn(1f, 1.35f)
        } else {
            1f
        }
        canvas.save()
        canvas.scale(horizontalScale, 1f, sizePx / 2f, baseline)
        canvas.drawText(text, sizePx / 2f, baseline, paint)
        canvas.restore()
        return bitmap
    }

    internal fun clearCacheForTesting() {
        synchronized(cacheLock) { bitmapCache.clear() }
    }

    internal fun cacheSizeForTesting(): Int =
        synchronized(cacheLock) { bitmapCache.size }
}
