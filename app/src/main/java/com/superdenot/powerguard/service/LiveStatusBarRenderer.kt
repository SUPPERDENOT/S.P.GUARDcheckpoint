package com.superdenot.powerguard.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Renders a compact, monochrome glyph suitable for a notification small icon.
 *
 * This class deliberately accepts an already validated display string. Sensor availability
 * belongs to the data/service layer; a missing reading must be passed as null rather than
 * converted to a fabricated zero value.
 */
internal object LiveStatusBarRenderer {

    enum class Parameter {
        TEMPERATURE,
        VOLTAGE,
        CURRENT,
        POWER,
        BATTERY_PERCENTAGE,
        CHARGING_STATUS
    }

    /**
     * Formats the exact LiveBatteryState instance used by the app UI.
     * Unavailable readings are represented as N/A; valid zero readings remain visible.
     */
    fun format(state: com.superdenot.powerguard.domain.model.LiveBatteryState, parameter: Parameter): String {
        return when (parameter) {
            Parameter.TEMPERATURE -> if (state.isTemperatureAvailable) "${state.tempCelsius.roundToInt()}°" else "N/A"
            Parameter.VOLTAGE -> if (state.isVoltageAvailable) String.format(Locale.US, "%.1fV", state.voltageMv / 1000f) else "N/A"
            Parameter.CURRENT -> if (state.isCurrentAvailable) String.format(Locale.US, "%.1fA", state.currentMa / 1000f) else "N/A"
            Parameter.POWER -> if (state.isPowerAvailable) String.format(Locale.US, "%.1fW", state.powerWatt) else "N/A"
            Parameter.BATTERY_PERCENTAGE -> if (state.isLevelAvailable) "${state.level}%" else "N/A"
            Parameter.CHARGING_STATUS -> if (state.isChargingStatusAvailable) {
                if (state.isCharging) "CHG" else "DIS"
            } else "N/A"
        }
    }

    const val ICON_WIDTH = 112
    const val ICON_HEIGHT = 64
    const val MAX_CACHE_ENTRIES = 32
    private const val HORIZONTAL_PADDING = 5f
    private const val VERTICAL_PADDING = 4f
    private const val MAX_TEXT_LENGTH = 8
    private const val BASE_TEXT_SIZE = 42f
    private const val MIN_TEXT_SIZE = 12f

    private val cacheLock = Any()
    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    /**
     * Returns null for unavailable/blank input. The caller must not replace unavailable sensor
     * readings with zero; zero is a valid reading for several battery measurements.
     */
    fun render(displayText: String?): Bitmap? {
        val text = displayText?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_TEXT_LENGTH && it.none { char -> char.code < 0x20 || char.code in 0x7f..0x9f } }
            ?: return null

        return synchronized(cacheLock) {
            bitmapCache[text] ?: renderBitmap(text).also { bitmapCache[text] = it }
        }
    }

    private fun renderBitmap(text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = BASE_TEXT_SIZE
        }

        val maxTextWidth = ICON_WIDTH - (HORIZONTAL_PADDING * 2f)
        val measuredWidth = paint.measureText(text)
        if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
            paint.textSize = (BASE_TEXT_SIZE * maxTextWidth / measuredWidth).coerceAtLeast(MIN_TEXT_SIZE)
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val baseline = (ICON_HEIGHT / 2f) - (paint.ascent() + paint.descent()) / 2f
        val availableHeight = ICON_HEIGHT - (VERTICAL_PADDING * 2f)
        if (bounds.height() > availableHeight) {
            paint.textSize = (paint.textSize * availableHeight / bounds.height()).coerceAtLeast(MIN_TEXT_SIZE)
        }

        val adjustedBaseline = (ICON_HEIGHT / 2f) - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, ICON_WIDTH / 2f, adjustedBaseline.takeIf { it.isFinite() } ?: baseline, paint)
        return bitmap
    }

    internal fun clearCacheForTesting() {
        synchronized(cacheLock) { bitmapCache.clear() }
    }

    internal fun cacheSizeForTesting(): Int =
        synchronized(cacheLock) { bitmapCache.size }
}
