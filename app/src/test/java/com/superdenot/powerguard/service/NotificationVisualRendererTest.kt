package com.superdenot.powerguard.service

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.R
import com.superdenot.powerguard.domain.model.LiveBatteryState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class NotificationVisualRendererTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        NotificationVisualRenderer.clearCacheForTesting()
    }

    @After
    fun tearDown() {
        NotificationVisualRenderer.clearCacheForTesting()
    }

    @Test
    fun resolveMonitorVisualSpec_coversAllBatteryStatesAndDrawables() {
        val standby = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = false, chargeSource = "Discharging", tempCelsius = 31.0f)
        )
        assertEquals(R.drawable.ic_status_standby, standby.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_STANDBY_SLATE, standby.accentColor)

        val acNormal = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = false, tempCelsius = 32.0f)
        )
        assertEquals(R.drawable.ic_status_ac, acNormal.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, acNormal.accentColor)

        val turboFast = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = true, tempCelsius = 34.5f)
        )
        assertEquals(R.drawable.ic_status_turbo_ring, turboFast.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_CYAN_FAST_CHARGE, turboFast.accentColor)

        val usb = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "USB Port", isFastCharging = false, tempCelsius = 30.5f)
        )
        assertEquals(R.drawable.ic_status_usb, usb.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, usb.accentColor)

        val wireless = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "Wireless", isFastCharging = false, tempCelsius = 33.0f)
        )
        assertEquals(R.drawable.ic_status_wireless, wireless.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, wireless.accentColor)

        val genericNormal = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "Unknown", isFastCharging = false, tempCelsius = 32.0f)
        )
        assertEquals(R.drawable.ic_status_temp_normal, genericNormal.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, genericNormal.accentColor)

        val warm = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = true, tempCelsius = 38.2f)
        )
        assertEquals(R.drawable.ic_status_temp_warm, warm.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_AMBER_THERMAL, warm.accentColor)

        val hot = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = true, tempCelsius = 42.0f)
        )
        assertEquals(R.drawable.ic_status_temp_hot, hot.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, hot.accentColor)

        val activeAnomaly = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(
                isCharging = true,
                chargeSource = "AC Adapter",
                isFastCharging = false,
                tempCelsius = 34.0f,
                anomalyAlert = "Arus pengisian terlalu rendah"
            )
        )
        assertEquals(R.drawable.ic_status_anomaly, activeAnomaly.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_AMBER_THERMAL, activeAnomaly.accentColor)

        val recoveryHold = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = false, chargeSource = "Discharging", tempCelsius = 33.0f),
            isRecoveryHold = true
        )
        assertEquals(R.drawable.ic_status_standby, recoveryHold.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_AMBER_THERMAL, recoveryHold.accentColor)
    }

    @Test
    fun bitmapCache_reusesExactBitmapInstanceAcrossRepeatedUpdatesWithoutReallocation() {
        val stateTick1 = LiveBatteryState(
            level = 54,
            currentMa = 2450,
            voltageMv = 4150,
            powerWatt = 10.17f,
            tempCelsius = 33.4f,
            isCharging = true,
            chargeSource = "AC Adapter",
            isFastCharging = true
        )
        val stateTick2 = stateTick1.copy(
            level = 55,
            currentMa = 2390,
            voltageMv = 4170,
            powerWatt = 9.96f,
            tempCelsius = 33.8f
        )

        val spec1 = NotificationVisualRenderer.resolveMonitorVisualSpec(stateTick1)
        val spec2 = NotificationVisualRenderer.resolveMonitorVisualSpec(stateTick2)
        assertEquals(spec1.cacheKey, spec2.cacheKey)

        val bmp1 = NotificationVisualRenderer.getOrRenderMonitorLargeIcon(context, spec1, stateTick1.isFastCharging)
        assertEquals(1, NotificationVisualRenderer.cacheMisses())
        assertEquals(0, NotificationVisualRenderer.cacheHits())
        assertEquals(1, NotificationVisualRenderer.cacheSize())
        assertFalse(bmp1.isRecycled)
        assertEquals(Bitmap.Config.ARGB_8888, bmp1.config)
        assertTrue(bmp1.width >= 64 && bmp1.height >= 64)

        repeat(20) {
            val cached = NotificationVisualRenderer.getOrRenderMonitorLargeIcon(context, spec2, stateTick2.isFastCharging)
            assertSame("Expected cached Bitmap instance without repeated allocation", bmp1, cached)
        }
        assertEquals(1, NotificationVisualRenderer.cacheMisses())
        assertEquals(20, NotificationVisualRenderer.cacheHits())
        assertEquals(1, NotificationVisualRenderer.cacheSize())

        val hotSpec = NotificationVisualRenderer.resolveMonitorVisualSpec(stateTick2.copy(tempCelsius = 42.5f))
        val hotBmp = NotificationVisualRenderer.getOrRenderMonitorLargeIcon(context, hotSpec, isFastCharging = false)
        assertNotSame(bmp1, hotBmp)
        assertEquals(2, NotificationVisualRenderer.cacheMisses())
        assertEquals(2, NotificationVisualRenderer.cacheSize())
    }

    @Test
    fun renderLargeIcon_producesFullColorNonMonochromeArtwork() {
        val fastSpec = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = true, tempCelsius = 32.0f)
        )
        val bitmap = NotificationVisualRenderer.getOrRenderMonitorLargeIcon(context, fastSpec, isFastCharging = true)

        val distinctColors = HashSet<Int>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val px = bitmap.getPixel(x, y)
                if (Color.alpha(px) > 0) {
                    distinctColors.add(px and 0x00FFFFFF)
                }
            }
        }
        assertTrue("Expected multi-color rendered badge with gradient and accent rings", distinctColors.size > 10)
    }

    @Test
    fun anomalyVisualSpec_usesSuperdGuardIconAndSeverityColors() {
        val thermalAnomaly = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Suhu baterai tinggi (43.0°C). Sistem memangkas arus pengisian."
        )
        assertEquals(R.drawable.ic_status_anomaly, thermalAnomaly.smallIconResId)
        assertNotEquals(android.R.drawable.stat_notify_error, thermalAnomaly.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, thermalAnomaly.accentColor)

        val slowChargeAnomaly = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Arus pengisian lambat terdeteksi pada adaptor AC."
        )
        assertEquals(R.drawable.ic_status_anomaly, slowChargeAnomaly.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_AMBER_THERMAL, slowChargeAnomaly.accentColor)

        val anomalyBitmap = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, thermalAnomaly)
        assertNotNull(anomalyBitmap)
        assertFalse(anomalyBitmap.isRecycled)
    }

    @Test
    @Config(sdk = [26])
    fun serviceNotifications_onApi26_attachMonochromeSmallIconDynamicAccentColorAndColoredLargeIcon() {
        val controller = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val service = controller.get()
        try {
            val monitorNotification: Notification =
                service.buildMonitorNotificationForTesting("+1850 mA (7.6W) • 33.0°C")
            assertNotNull(monitorNotification)
            assertEquals("SUPERD GUARD", monitorNotification.extras.getCharSequence(Notification.EXTRA_TITLE))
            assertNotEquals(0, monitorNotification.icon)
            assertNotEquals(0, monitorNotification.color)
            assertNotNull(NotificationCompat.getLargeIcon(monitorNotification))

            val anomalyNotification: Notification =
                service.buildAnomalyAlertNotificationForTesting("Suhu baterai tinggi (42.5°C).")
            assertNotNull(anomalyNotification)
            assertEquals("SUPERD GUARD • Peringatan Daya", anomalyNotification.extras.getCharSequence(Notification.EXTRA_TITLE))
            assertEquals(R.drawable.ic_status_anomaly, anomalyNotification.icon)
            assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, anomalyNotification.color)
            assertNotNull(NotificationCompat.getLargeIcon(anomalyNotification))
        } finally {
            controller.destroy()
        }
    }
    @Test
    fun anomalySeverity_usesDetectorCodesForAllFiveAnomalyTypes() {
        val cases = listOf(
            "FAKE_FAST_CHARGE_OSCILLATION" to NotificationVisualRenderer.AnomalySeverity.CRITICAL,
            "THERMAL_THROTTLING" to NotificationVisualRenderer.AnomalySeverity.CRITICAL,
            "NET_DRAIN_CHARGING" to NotificationVisualRenderer.AnomalySeverity.CRITICAL,
            "FAKE_FAST_CHARGING" to NotificationVisualRenderer.AnomalySeverity.WARNING,
            "SLOW_CHARGING" to NotificationVisualRenderer.AnomalySeverity.WARNING
        )
        cases.forEach { (code, expected) ->
            val spec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
                alertMessage = "Anomali Pengisian: arus masuk hanya -120 mA",
                anomalyCode = code
            )
            assertEquals("Severity for $code", expected, spec.severity)
            assertEquals(
                if (expected == NotificationVisualRenderer.AnomalySeverity.CRITICAL) NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY else NotificationVisualRenderer.COLOR_AMBER_THERMAL,
                spec.accentColor
            )
        }
    }

    @Test
    fun bitmapCache_isBoundedAndEvictionDoesNotRecycleIssuedBitmaps() {
        val base = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", tempCelsius = 32.0f)
        )
        val first = NotificationVisualRenderer.getOrRenderMonitorLargeIcon(
            context, base.copy(cacheKey = "eviction-0")
        )
        repeat(NotificationVisualRenderer.MAX_CACHE_ENTRIES + 4) { index ->
            NotificationVisualRenderer.getOrRenderMonitorLargeIcon(
                context, base.copy(cacheKey = "eviction-$index")
            )
        }
        assertTrue(NotificationVisualRenderer.cacheSize() <= NotificationVisualRenderer.MAX_CACHE_ENTRIES)
        assertFalse("An evicted bitmap may still be referenced by NotificationManager", first.isRecycled)
    }

    @Test
    fun bitmapCache_concurrentSameKeyRequestsReturnSingleSharedBitmap() {
        val state = LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", isFastCharging = true, tempCelsius = 33.0f)
        val spec = NotificationVisualRenderer.resolveMonitorVisualSpec(state)
        val workers = 16
        val ready = java.util.concurrent.CountDownLatch(workers)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(workers)
        try {
            val futures = (0 until workers).map {
                executor.submit<Bitmap> {
                    ready.countDown()
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    NotificationVisualRenderer.getOrRenderMonitorLargeIcon(context, spec, true)
                }
            }
            assertTrue("Workers did not become ready", ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
            start.countDown()
            val bitmaps = futures.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            val first = bitmaps.first()
            assertTrue(bitmaps.all { it === first })
            assertEquals(1, NotificationVisualRenderer.cacheMisses())
            assertEquals(workers - 1, NotificationVisualRenderer.cacheHits())
            assertEquals(1, NotificationVisualRenderer.cacheSize())
            assertFalse(first.isRecycled)
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

}