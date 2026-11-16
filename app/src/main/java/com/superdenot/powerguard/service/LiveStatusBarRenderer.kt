package com.superdenot.powerguard.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.util.LinkedHashMap

/**
 * Renders a compact, monochrome glyph suitable for a notification small icon.
 *
 * This class deliberately accepts an already validated display string. Sensor availability
 * belongs to the data/service layer; a missing reading must be passed as null rather than
 * converted to a fabricated zero value.
 */
internal object LiveStatusBarRenderer {

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
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_TEXT_LENGTH && it.none(Char::isISOControl) }
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
