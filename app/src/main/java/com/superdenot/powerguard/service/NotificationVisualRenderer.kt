package com.superdenot.powerguard.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.superdenot.powerguard.R
import com.superdenot.powerguard.domain.model.LiveBatteryState
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * Renderer visual notifikasi P1 untuk SUPERD GUARD.
 *
 * Memisahkan tiga lapisan visual notifikasi:
 * 1. Small icon monokrom status bar berdasarkan kondisi daya/suhu baterai.
 * 2. Warna aksen dinamis melalui NotificationCompat.Builder.setColor().
 * 3. Large icon berwarna penuh untuk panel notifikasi dengan bounded LRU bitmap cache
 *    agar tidak terjadi alokasi Bitmap berulang setiap siklus pembaruan 3 detik.
 *
 * Kompatibel penuh dengan Android API 26+.
 */
internal object NotificationVisualRenderer {

    @ColorInt const val COLOR_EMERALD_CHARGE: Int = 0xFF00E676.toInt()
    @ColorInt const val COLOR_CYAN_ACCENT: Int = 0xFF00E5FF.toInt()
    @ColorInt const val COLOR_AMBER_THERMAL: Int = 0xFFFFAB00.toInt()
    @ColorInt const val COLOR_CRIMSON_ANOMALY: Int = 0xFFFF5252.toInt()
    @ColorInt const val COLOR_STANDBY_SLATE: Int = 0xFF78909C.toInt()

    @ColorInt private const val BADGE_SURFACE_CENTER: Int = 0xFF222B3C.toInt()
    @ColorInt private const val BADGE_SURFACE_EDGE: Int = 0xFF10141D.toInt()
    @ColorInt private const val ANOMALY_SURFACE_CENTER: Int = 0xFF311822.toInt()
    @ColorInt private const val ANOMALY_SURFACE_EDGE: Int = 0xFF140B10.toInt()

    private const val TEMP_WARM_THRESHOLD_C = 37.0f
    private const val TEMP_HOT_THRESHOLD_C = 41.0f

    internal const val MAX_CACHE_ENTRIES = 16
    private const val MIN_LARGE_ICON_PX = 64
    private const val MAX_LARGE_ICON_PX = 128
    private const val TARGET_LARGE_ICON_DP = 48f

    data class MonitorVisualSpec(
        @DrawableRes val smallIconResId: Int,
        @ColorInt val accentColor: Int,
        val subText: String,
        val cacheKey: String
    )

    enum class AnomalySeverity { WARNING, CRITICAL }

    data class AnomalyVisualSpec(
        @DrawableRes val smallIconResId: Int,
        @ColorInt val accentColor: Int,
        val subText: String,
        val cacheKey: String,
        val severity: AnomalySeverity
    )

    private val cacheLock = Any()
    private val bitmapCache = LruCache<String, Bitmap>(MAX_CACHE_ENTRIES)
    private val cacheHitsCounter = AtomicInteger(0)
    private val cacheMissesCounter = AtomicInteger(0)

    fun resolveMonitorContentTitle(state: LiveBatteryState): String {
        if (!state.isCharging) return "Pemantauan Baterai Aktif"
        val level = if (state.isLevelAvailable) "${state.level}%" else "—"
        return "Mengisi Daya • $level"
    }

    fun resolveMonitorVisualSpec(
        state: LiveBatteryState,
        isRecoveryHold: Boolean = false
    ): MonitorVisualSpec {
        if (isRecoveryHold) {
            val iconRes = if (state.isCharging) {
                resolveChargingDrawable(state)
            } else {
                R.drawable.ic_status_standby
            }
            return MonitorVisualSpec(
                smallIconResId = iconRes,
                accentColor = COLOR_AMBER_THERMAL,
                subText = "SUPERD GUARD • RECOVERY",
                cacheKey = "monitor:$iconRes:$COLOR_AMBER_THERMAL:false:true"
            )
        }

        if (!state.isCharging) {
            return MonitorVisualSpec(
                smallIconResId = R.drawable.ic_status_standby,
                accentColor = COLOR_STANDBY_SLATE,
                subText = "SUPERD GUARD • STANDBY",
                cacheKey = "monitor:${R.drawable.ic_status_standby}:$COLOR_STANDBY_SLATE:false:false"
            )
        }

        if (state.tempCelsius >= TEMP_HOT_THRESHOLD_C) {
            return MonitorVisualSpec(
                smallIconResId = R.drawable.ic_status_temp_hot,
                accentColor = COLOR_CRIMSON_ANOMALY,
                subText = "SUPERD GUARD • THERMAL HIGH",
                cacheKey = "monitor:${R.drawable.ic_status_temp_hot}:$COLOR_CRIMSON_ANOMALY:false:false"
            )
        }

        if (!state.anomalyAlert.isNullOrBlank()) {
            val anomalySpec = resolveAnomalyVisualSpec(state.anomalyAlert)
            return MonitorVisualSpec(
                smallIconResId = anomalySpec.smallIconResId,
                accentColor = anomalySpec.accentColor,
                subText = anomalySpec.subText,
                cacheKey = "monitor:${anomalySpec.cacheKey}"
            )
        }

        if (state.tempCelsius >= TEMP_WARM_THRESHOLD_C) {
            return MonitorVisualSpec(
                smallIconResId = R.drawable.ic_status_temp_warm,
                accentColor = COLOR_AMBER_THERMAL,
                subText = "SUPERD GUARD • WARM",
                cacheKey = "monitor:${R.drawable.ic_status_temp_warm}:$COLOR_AMBER_THERMAL:false:false"
            )
        }

        val smallIconRes = resolveChargingDrawable(state)
        val accentColor = COLOR_EMERALD_CHARGE
        val subText = when (state.chargeSource) {
            "USB Port" -> "SUPERD GUARD • USB"
            "Wireless" -> "SUPERD GUARD • WIRELESS"
            "AC Adapter" -> "SUPERD GUARD • AC CHARGE"
            else -> "SUPERD GUARD • CHARGING"
        }

        return MonitorVisualSpec(
            smallIconResId = smallIconRes,
            accentColor = accentColor,
            subText = subText,
            cacheKey = "monitor:$smallIconRes:$accentColor:false"
        )
    }

    fun resolveAnomalyVisualSpec(
        alertMessage: String,
        anomalyCode: String? = null
    ): AnomalyVisualSpec {
        val normalized = alertMessage.lowercase(Locale.US)
        val severity = when (anomalyCode) {
            "THERMAL_THROTTLING",
            "NET_DRAIN_CHARGING" -> AnomalySeverity.CRITICAL
            "SLOW_CHARGING" -> AnomalySeverity.WARNING
            else -> {
                val criticalByMessage =
                    normalized.contains("suhu baterai tinggi") ||
                    normalized.contains("memangkas arus") ||
                    normalized.contains("thermal") ||
                    normalized.contains("berkedip tidak stabil") ||
                    normalized.contains("berubah positif/negatif") ||
                    normalized.contains("oscillation") ||
                    Regex("""arus masuk hanya -\d+""").containsMatchIn(normalized) ||
                    normalized.contains("net_drain") ||
                    normalized.contains("terkuras") ||
                    normalized.contains("drain")
                if (criticalByMessage) AnomalySeverity.CRITICAL else AnomalySeverity.WARNING
            }
        }
        val accentColor = if (severity == AnomalySeverity.CRITICAL) COLOR_CRIMSON_ANOMALY else COLOR_AMBER_THERMAL
        val subText = if (severity == AnomalySeverity.CRITICAL) "SUPERD GUARD • CRITICAL" else "SUPERD GUARD • WARNING"
        return AnomalyVisualSpec(
            smallIconResId = R.drawable.ic_status_anomaly,
            accentColor = accentColor,
            subText = subText,
            cacheKey = "anomaly:${R.drawable.ic_status_anomaly}:$accentColor",
            severity = severity
        )
    }

    @DrawableRes
    private fun resolveChargingDrawable(state: LiveBatteryState): Int {
        if (state.tempCelsius >= TEMP_HOT_THRESHOLD_C) return R.drawable.ic_status_temp_hot
        if (state.tempCelsius >= TEMP_WARM_THRESHOLD_C) return R.drawable.ic_status_temp_warm

        return when (state.chargeSource) {
            "USB Port" -> R.drawable.ic_status_usb
            "AC Adapter" -> R.drawable.ic_status_ac
            "Wireless" -> R.drawable.ic_status_wireless
            else -> R.drawable.ic_status_temp_normal
        }
    }

    fun getOrRenderMonitorLargeIcon(
        context: Context,
        spec: MonitorVisualSpec,
    ): Bitmap {
        return getOrCreateCachedBitmap(context, spec.cacheKey) { appContext, sizePx ->
            renderBadgeBitmap(
                context = appContext,
                sizePx = sizePx,
                iconResId = spec.smallIconResId,
                accentColor = spec.accentColor,
                secondaryArcColor = null,
                centerSurfaceColor = BADGE_SURFACE_CENTER,
                edgeSurfaceColor = BADGE_SURFACE_EDGE
            )
        }
    }

    fun getOrRenderAnomalyLargeIcon(
        context: Context,
        spec: AnomalyVisualSpec
    ): Bitmap {
        return getOrCreateCachedBitmap(context, spec.cacheKey) { appContext, sizePx ->
            renderBadgeBitmap(
                context = appContext,
                sizePx = sizePx,
                iconResId = spec.smallIconResId,
                accentColor = spec.accentColor,
                secondaryArcColor = COLOR_CYAN_ACCENT,
                centerSurfaceColor = ANOMALY_SURFACE_CENTER,
                edgeSurfaceColor = ANOMALY_SURFACE_EDGE
            )
        }
    }

    private fun getOrCreateCachedBitmap(
        context: Context,
        baseKey: String,
        render: (Context, Int) -> Bitmap
    ): Bitmap {
        val appContext = context.applicationContext ?: context
        val sizePx = resolveLargeIconSizePx(appContext)
        val fullKey = "$baseKey:$sizePx"

        // Lock mencakup cache lookup dan render agar permintaan paralel untuk key
        // yang sama tidak membuat Bitmap duplikat. Ukuran bitmap kecil (64–128 px).
        // Jangan recycle saat eviction: NotificationManager mungkin masih memegangnya.
        synchronized(cacheLock) {
            bitmapCache.get(fullKey)?.let { cached ->
                if (!cached.isRecycled) {
                    cacheHitsCounter.incrementAndGet()
                    return cached
                }
            }
            val rendered = render(appContext, sizePx)
            bitmapCache.put(fullKey, rendered)
            cacheMissesCounter.incrementAndGet()
            return rendered
        }
    }

    private fun resolveLargeIconSizePx(context: Context): Int {
        val density = context.resources?.displayMetrics?.density ?: 1f
        return (TARGET_LARGE_ICON_DP * density)
            .roundToInt()
            .coerceIn(MIN_LARGE_ICON_PX, MAX_LARGE_ICON_PX)
    }

    private fun renderBadgeBitmap(
        context: Context,
        sizePx: Int,
        @DrawableRes iconResId: Int,
        @ColorInt accentColor: Int,
        @ColorInt secondaryArcColor: Int?,
        @ColorInt centerSurfaceColor: Int,
        @ColorInt edgeSurfaceColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx * 0.5f
        val cy = sizePx * 0.5f
        val outerRadius = sizePx * 0.46f

        // 1. Dark precision obsidian-slate backdrop disc
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                cx,
                cy,
                outerRadius,
                centerSurfaceColor,
                edgeSurfaceColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, outerRadius, bgPaint)

        // 2. Ambient inner glow halo matching dynamic accent color
        val haloRadius = outerRadius - sizePx * 0.045f
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.065f
            color = ColorUtils.setAlphaComponent(accentColor, 54)
        }
        canvas.drawCircle(cx, cy, haloRadius, haloPaint)

        // 3. Crisp outer accent border ring
        val borderStroke = sizePx * 0.036f
        val borderRadius = outerRadius - borderStroke * 0.5f
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderStroke
            color = ColorUtils.setAlphaComponent(accentColor, 232)
        }
        canvas.drawCircle(cx, cy, borderRadius, borderPaint)

        // 4. Optional secondary status arc
        if (secondaryArcColor != null) {
            val arcInset = sizePx * 0.095f
            val arcBounds = RectF(arcInset, arcInset, sizePx - arcInset, sizePx - arcInset)
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = sizePx * 0.03f
                color = ColorUtils.setAlphaComponent(secondaryArcColor, 210)
            }
            canvas.drawArc(arcBounds, -55f, 110f, false, arcPaint)
        }

        // 5. Full-color multi-tone vector icon centered in the badge (no monochrome tint override)
        val iconPadding = (sizePx * 0.22f).roundToInt()
        val drawable = runCatching { ContextCompat.getDrawable(context, iconResId)?.mutate() }.getOrNull()
        if (drawable != null) {
            drawable.setBounds(
                iconPadding,
                iconPadding,
                sizePx - iconPadding,
                sizePx - iconPadding
            )
            drawable.draw(canvas)
        } else {
            drawFallbackBolt(canvas, sizePx, accentColor)
        }

        return bitmap
    }

    private fun drawFallbackBolt(canvas: Canvas, sizePx: Int, @ColorInt color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val s = sizePx.toFloat()
        val bolt = Path().apply {
            moveTo(s * 0.55f, s * 0.22f)
            lineTo(s * 0.34f, s * 0.53f)
            lineTo(s * 0.49f, s * 0.53f)
            lineTo(s * 0.45f, s * 0.78f)
            lineTo(s * 0.66f, s * 0.47f)
            lineTo(s * 0.51f, s * 0.47f)
            close()
        }
        canvas.drawPath(bolt, paint)
    }

    internal fun cacheHits(): Int = cacheHitsCounter.get()

    internal fun cacheMisses(): Int = cacheMissesCounter.get()

    internal fun cacheSize(): Int = synchronized(cacheLock) { bitmapCache.size() }

    internal fun clearCacheForTesting() {
        synchronized(cacheLock) {
            bitmapCache.evictAll()
        }
        cacheHitsCounter.set(0)
        cacheMissesCounter.set(0)
    }
}