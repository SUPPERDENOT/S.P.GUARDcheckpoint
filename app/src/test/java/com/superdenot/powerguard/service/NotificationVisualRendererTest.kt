package com.superdenot.powerguard.service

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.R
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.domain.model.StatusBarParameter
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class NotificationVisualRendererTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context.applicationContext as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationVisualRenderer.clearCacheForTesting()
    }

    @After
    fun tearDown() {
        NotificationVisualRenderer.clearCacheForTesting()
    }

    @Test
    fun monitorContentTitleReflectsChargingLevelAndIdleStateWithoutDuplicateBrandTitle() {
        assertEquals(
            "Mengisi Daya • 56%",
            NotificationVisualRenderer.resolveMonitorContentTitle(
                LiveBatteryState(level = 56, isCharging = true)
            )
        )
        assertEquals(
            "Mengisi Daya • —",
            NotificationVisualRenderer.resolveMonitorContentTitle(
                LiveBatteryState(level = 0, isLevelAvailable = false, isCharging = true)
            )
        )
        assertEquals(
            "Mengisi Daya • 56% • Menghitung ETA...",
            NotificationVisualRenderer.resolveMonitorContentTitle(
                LiveBatteryState(level = 56, isCharging = true, isChargingEtaTracking = true)
            )
        )
        assertEquals(
            "Mengisi Daya • 56% • ±58 mnt",
            NotificationVisualRenderer.resolveMonitorContentTitle(
                LiveBatteryState(
                    level = 56,
                    isCharging = true,
                    isChargingEtaTracking = true,
                    chargingEtaMinutes = 58L
                )
            )
        )
        assertEquals(
            "Pemantauan Baterai Aktif",
            NotificationVisualRenderer.resolveMonitorContentTitle(
                LiveBatteryState(level = 56, isCharging = false)
            )
        )
    }

    @Test
    fun resolvedEtaIsSharedByForegroundNotificationTitleAndExpandedBody() {
        val state = LiveBatteryState(level = 79, isCharging = true,
            isChargingEtaTracking = true, chargingEtaMinutes = 63L)
        assertEquals("Mengisi Daya • 79% • ±1j 3m",
            NotificationVisualRenderer.resolveMonitorContentTitle(state))
        assertTrue(NotificationVisualRenderer.formatMonitorExpandedText(state)
            .endsWith("Estimasi penuh: 1 jam 3 menit"))
    }

    @Test
    fun statusBarParameterNotificationTextFormatsAllSixSelections() {
        val state = LiveBatteryState(
            level = 33,
            currentMa = -243,
            voltageMv = 3853,
            powerWatt = -3.0f,
            tempCelsius = 36.1f,
            isCharging = true,
            isPowerConnected = true,
            chargeSource = "AC Adapter",
            isChargeSourceAvailable = true
        )
        val expected = mapOf(
            StatusBarParameter.TEMPERATURE to "Suhu • 36.1°C",
            StatusBarParameter.VOLTAGE to "Tegangan • 3853 mV",
            StatusBarParameter.CURRENT to "Arus • -243 mA",
            StatusBarParameter.POWER to "Daya • -3.0 W",
            StatusBarParameter.BATTERY_PERCENTAGE to "Level Baterai • 33%",
            StatusBarParameter.RESOURCE_SOURCE to "Sumber daya • AC"
        )
        expected.forEach { (parameter, text) ->
            assertEquals(
                "Notification text for $parameter",
                text,
                NotificationVisualRenderer.formatStatusBarParameterText(state, parameter)
            )
        }
        assertEquals(
            "Sumber daya • USB",
            NotificationVisualRenderer.formatStatusBarParameterText(
                state.copy(chargeSource = "USB Port"),
                StatusBarParameter.RESOURCE_SOURCE
            )
        )
        assertEquals(
            "Status • Mengisi (AC)",
            NotificationVisualRenderer.formatStatusBarParameterText(state, StatusBarParameter.CHARGING_STATUS)
        )
        assertEquals(
            "Status • Tidak Mengisi",
            NotificationVisualRenderer.formatStatusBarParameterText(
                state.copy(isCharging = false),
                StatusBarParameter.CHARGING_STATUS
            )
        )
    }

    @Test
    fun statusBarParameterNotificationTextUsesUnavailableFallbackForEverySelection() {
        val unavailable = LiveBatteryState(
            isLevelAvailable = false,
            isCurrentAvailable = false,
            isVoltageAvailable = false,
            isTemperatureAvailable = false,
            isPowerAvailable = false,
            isChargingStatusAvailable = false
        )
        val expected = mapOf(
            StatusBarParameter.TEMPERATURE to "Suhu • —",
            StatusBarParameter.VOLTAGE to "Tegangan • —",
            StatusBarParameter.CURRENT to "Arus • —",
            StatusBarParameter.POWER to "Daya • —",
            StatusBarParameter.BATTERY_PERCENTAGE to "Level Baterai • —",
            StatusBarParameter.RESOURCE_SOURCE to "Sumber daya • —"
        )
        expected.forEach { (parameter, text) ->
            assertEquals(
                "Unavailable notification text for $parameter",
                text,
                NotificationVisualRenderer.formatStatusBarParameterText(unavailable, parameter)
            )
        }
        assertEquals(
            "Status • —",
            NotificationVisualRenderer.formatStatusBarParameterText(unavailable, StatusBarParameter.CHARGING_STATUS)
        )
    }
    @Test
    fun monitorNotificationBodyIsCompactAndKeepsSignedBatterySideReadings() {
        val state = LiveBatteryState(
            currentMa = -243,
            powerWatt = -0.9f,
            tempCelsius = 31.5f,
            sessionMahDelivered = 0.0
        )
        assertEquals(
            "-243 mA (-0.9W) • 31.5°C • +0.0 mAh",
            NotificationVisualRenderer.formatMonitorContentText(state)
        )
        assertEquals(
            "-243 mA (-0.9W) • 31.5°C • +0.0 mAh",
            NotificationVisualRenderer.formatMonitorContentText(
                state.copy(isCharging = true, isChargingEtaTracking = true, chargingEtaMinutes = 58L)
            )
        )
        assertEquals(
            "Level baterai: 0%\nArus baterai: -243 mA\nDaya baterai: -0.9 W\nTegangan baterai: 0 mV\nSuhu baterai: 31.5°C\nMuatan bersih: +0.0 mAh\nStatus pengisian: Tidak mengisi\nSumber daya: Discharging\nProtokol / kemampuan sumber: Tidak Terhubung",
            NotificationVisualRenderer.formatMonitorExpandedText(state)
        )
    }

    @Test
    fun resolveMonitorVisualSpec_coversAllBatteryStatesAndDrawables() {
        val standby = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = false, chargeSource = "Discharging", tempCelsius = 31.0f)
        )
        assertEquals(R.drawable.ic_status_standby, standby.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_STANDBY_SLATE, standby.accentColor)

        val acNormal = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", tempCelsius = 32.0f)
        )
        assertEquals(R.drawable.ic_status_ac, acNormal.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, acNormal.accentColor)

        val acWithoutProtocolEvidence = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", tempCelsius = 34.5f)
        )
        assertEquals(R.drawable.ic_status_ac, acWithoutProtocolEvidence.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, acWithoutProtocolEvidence.accentColor)
        assertFalse(acWithoutProtocolEvidence.subText.contains("FAST"))

        val usb = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "USB Port", tempCelsius = 30.5f)
        )
        assertEquals(R.drawable.ic_status_usb, usb.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, usb.accentColor)

        val wireless = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "Wireless", tempCelsius = 33.0f)
        )
        assertEquals(R.drawable.ic_status_wireless, wireless.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, wireless.accentColor)

        val genericNormal = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "Unknown", tempCelsius = 32.0f)
        )
        assertEquals(R.drawable.ic_status_temp_normal, genericNormal.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_EMERALD_CHARGE, genericNormal.accentColor)

        val warm = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", tempCelsius = 38.2f)
        )
        assertEquals(R.drawable.ic_status_temp_warm, warm.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_AMBER_THERMAL, warm.accentColor)

        val hot = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(isCharging = true, chargeSource = "AC Adapter", tempCelsius = 42.0f)
        )
        assertEquals(R.drawable.ic_status_temp_hot, hot.smallIconResId)
        assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, hot.accentColor)

        val activeAnomaly = NotificationVisualRenderer.resolveMonitorVisualSpec(
            LiveBatteryState(
                isCharging = true,
                chargeSource = "AC Adapter",
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
    fun bitmapCache_reusesExactBitmapInstanceForAnomalyNotification() {
        val spec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Thermal throttling",
            "THERMAL_THROTTLING"
        )
        val first = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, spec)
        assertEquals(1, NotificationVisualRenderer.cacheMisses())
        repeat(20) {
            val cached = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, spec)
            assertSame("Expected cached Bitmap instance without repeated allocation", first, cached)
        }
        assertEquals(1, NotificationVisualRenderer.cacheMisses())
        assertEquals(20, NotificationVisualRenderer.cacheHits())
        assertEquals(1, NotificationVisualRenderer.cacheSize())

        val otherSpec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Net drain",
            "NET_DRAIN_CHARGING"
        )
        assertNotEquals("Different anomaly codes must not share a cache identity", spec.cacheKey, otherSpec.cacheKey)
        val otherBitmap = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, otherSpec)
        assertNotSame(first, otherBitmap)
        assertEquals(2, NotificationVisualRenderer.cacheMisses())
        assertEquals(2, NotificationVisualRenderer.cacheSize())
    }

    @Test
    fun bitmapCache_keyIncludesEveryAnomalyVisualProperty() {
        val base = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Thermal throttling",
            "THERMAL_THROTTLING"
        )
        val original = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, base)
        val variants = listOf(
            base.copy(smallIconResId = R.drawable.ic_status_temp_hot),
            base.copy(accentColor = NotificationVisualRenderer.COLOR_AMBER_THERMAL),
            base.copy(subText = "SUPERD GUARD • WARNING"),
            base.copy(severity = NotificationVisualRenderer.AnomalySeverity.WARNING)
        )

        variants.forEach { variant ->
            val bitmap = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, variant)
            assertNotSame("Changed visual properties must not reuse the base bitmap", original, bitmap)
        }
        assertEquals(5, NotificationVisualRenderer.cacheMisses())
        assertEquals(5, NotificationVisualRenderer.cacheSize())
    }

    @Test
    fun renderLargeIcon_producesValidArgbBitmapForAnomalyNotification() {
        val spec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Thermal throttling",
            "THERMAL_THROTTLING"
        )
        val bitmap = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, spec)
        assertFalse(bitmap.isRecycled)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        assertTrue(bitmap.width in 64..128)
        assertEquals(bitmap.width, bitmap.height)
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
        val netDrainFallback = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Anomali Pengisian (AC Adapter): Arus masuk hanya -120 mA (-0.50 W)."
        )
        assertEquals(NotificationVisualRenderer.AnomalySeverity.CRITICAL, netDrainFallback.severity)
        assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, netDrainFallback.accentColor)

        val anomalyBitmap = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, thermalAnomaly)
        assertNotNull(anomalyBitmap)
        assertFalse(anomalyBitmap.isRecycled)
    }

    @Test
    @Config(sdk = [26])
    fun serviceNotifications_onApi26_keepSmallIconAndOmitMonitorLargeIcon() {
        val controller = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val service = controller.get()
        try {
            val warmupState = LiveBatteryState(
                level = 56,
                currentMa = -243,
                powerWatt = -0.9f,
                voltageMv = 4150,
                tempCelsius = 31.5f,
                sessionMahDelivered = 0.0,
                isCharging = true,
                isChargingEtaTracking = true,
                chargeSource = "USB Port",
                chargingProtocol = LiveBatteryState.PROTOCOL_NOT_EXPOSED
            )
            val monitorNotification: Notification =
                service.buildMonitorNotificationForTesting(
                    NotificationVisualRenderer.formatMonitorContentText(warmupState),
                    warmupState
                )
            assertNotNull(monitorNotification)
            assertEquals(
                "Mengisi Daya • 56% • Menghitung ETA...",
                monitorNotification.extras.getCharSequence(Notification.EXTRA_TITLE)
            )
            assertEquals(
                "-243 mA (-0.9W) • 31.5°C • +0.0 mAh",
                monitorNotification.extras.getCharSequence(Notification.EXTRA_TEXT)
            )
            assertEquals(
                "Level baterai: 56%\nArus baterai: -243 mA\nDaya baterai: -0.9 W\nTegangan baterai: 4150 mV\nSuhu baterai: 31.5°C\nMuatan bersih: +0.0 mAh\nStatus pengisian: Mengisi\nSumber daya: USB Port\nProtokol / kemampuan sumber: Tidak Diekspos Perangkat\nEstimasi penuh: Menghitung ETA...",
                monitorNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )

            val estimatedState = warmupState.copy(chargingEtaMinutes = 58L)
            val estimatedNotification = service.buildMonitorNotificationForTesting(
                NotificationVisualRenderer.formatMonitorContentText(estimatedState),
                estimatedState
            )
            assertEquals(
                "Mengisi Daya • 56% • ±58 mnt",
                estimatedNotification.extras.getCharSequence(Notification.EXTRA_TITLE)
            )
            assertEquals(
                "-243 mA (-0.9W) • 31.5°C • +0.0 mAh",
                estimatedNotification.extras.getCharSequence(Notification.EXTRA_TEXT)
            )
            assertTrue(
                estimatedNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    .toString().endsWith("Estimasi penuh: 58 menit")
            )
            assertEquals(null, monitorNotification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            assertNotNull(monitorNotification.smallIcon)
            assertEquals(null, monitorNotification.extras.get(Notification.EXTRA_LARGE_ICON))
            assertNotEquals(0, monitorNotification.color)

            val idleNotification = service.buildMonitorNotificationForTesting(
                "Perangkat menggunakan daya baterai",
                LiveBatteryState(isCharging = false)
            )
            assertEquals(
                "Pemantauan Baterai Aktif",
                idleNotification.extras.getCharSequence(Notification.EXTRA_TITLE)
            )

            val anomalyNotification: Notification =
                service.buildAnomalyAlertNotificationForTesting(
                    "Suhu baterai tinggi (45.7°C). Sistem mungkin membatasi arus pengisian.",
                    "THERMAL_THROTTLING",
                    45.7f
                )
            assertNotNull(anomalyNotification)
            assertEquals("SUPERD GUARD • Peringatan Daya", anomalyNotification.extras.getCharSequence(Notification.EXTRA_TITLE))
            assertEquals(R.drawable.ic_status_anomaly, anomalyNotification.icon)
            assertEquals(NotificationVisualRenderer.COLOR_CRIMSON_ANOMALY, anomalyNotification.color)
            assertEquals(
                "Anomali: Thermal Throttling (Puncak 45.7°C)",
                anomalyNotification.extras.getCharSequence(Notification.EXTRA_TEXT)
            )
            assertEquals(null, anomalyNotification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            assertTrue(
                anomalyNotification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    .toString().startsWith("Anomali: Thermal Throttling (Puncak 45.7°C)")
            )
        } finally {
            controller.destroy()
        }
    }
    @Test
    fun anomalyFormatter_mapsAllDetectorCodesAndRequestedDiagnosticCategories() {
        val cases = mapOf(
            "THERMAL_THROTTLING" to "Anomali: Thermal Throttling",
            "NET_DRAIN_CHARGING" to "Anomali: Daya Terkuras Saat Mengisi",
            "CHARGING_NO_INPUT" to "Anomali: Pengisian Terlalu Lambat",
            "CHARGING_PROGRESS_STALLED" to "Anomali: Pengisian Stagnan",
            "CHARGING_PROGRESS_NOT_OBSERVED" to "Anomali: Pengisian Stagnan",
            "SLOW_CHARGING" to "Anomali: Pengisian Terlalu Lambat",
            "HIGH_DRAIN" to "Anomali: Daya Terkuras Saat Mengisi",
            "LOW_CURRENT" to "Anomali: Pengisian Terlalu Lambat",
            "VOLTAGE_DROP" to "Anomali: Tegangan Tidak Stabil",
            "VOLTAGE_SPIKE" to "Anomali: Tegangan Tidak Stabil",
            "VOLTAGE_UNSTABLE" to "Anomali: Tegangan Tidak Stabil",
            "CHARGING_STAGNANT" to "Anomali: Pengisian Stagnan"
        )
        cases.forEach { (code, expected) ->
            assertEquals(code, expected, NotificationVisualRenderer.formatAnomalyReason(code))
        }
        assertEquals(
            "Anomali: Thermal Throttling (Puncak 45.7°C)",
            NotificationVisualRenderer.formatAnomalyReason("THERMAL_THROTTLING", 45.7f)
        )
        assertEquals(
            "Anomali: Thermal Throttling",
            NotificationVisualRenderer.formatAnomalyReason("THERMAL_THROTTLING", null)
        )
    }

    @Test
    fun anomalyFormatter_humanizesUnknownCodesAndMultipleCodesWithoutRawUnderscores() {
        assertEquals(
            "Anomali: Some Enum Name",
            NotificationVisualRenderer.formatAnomalyReason("SOME_ENUM_NAME")
        )
        assertEquals(
            "Anomali: Thermal Throttling • Pengisian Stagnan",
            NotificationVisualRenderer.formatAnomalyReason(
                "THERMAL_THROTTLING, CHARGING_PROGRESS_STALLED"
            )
        )
    }

    @Test
    fun anomalySeverity_usesCurrentDetectorCodes() {
        val cases = listOf(
            "THERMAL_THROTTLING" to NotificationVisualRenderer.AnomalySeverity.CRITICAL,
            "NET_DRAIN_CHARGING" to NotificationVisualRenderer.AnomalySeverity.CRITICAL,
            "CHARGING_NO_INPUT" to NotificationVisualRenderer.AnomalySeverity.WARNING,
            "CHARGING_PROGRESS_STALLED" to NotificationVisualRenderer.AnomalySeverity.WARNING,
            "CHARGING_PROGRESS_NOT_OBSERVED" to NotificationVisualRenderer.AnomalySeverity.WARNING
        )
        cases.forEach { (code, expected) ->
            val spec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
                alertMessage = "Peringatan pengisian berdasarkan pembacaan baterai.",
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
        val base = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Thermal throttling",
            "THERMAL_THROTTLING"
        )
        val first = NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(
            context, base.copy(cacheKey = "eviction-0")
        )
        repeat(NotificationVisualRenderer.MAX_CACHE_ENTRIES + 4) { index ->
            NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(
                context, base.copy(cacheKey = "eviction-$index")
            )
        }
        assertTrue(NotificationVisualRenderer.cacheSize() <= NotificationVisualRenderer.MAX_CACHE_ENTRIES)
        assertFalse("An evicted Bitmap may still be referenced by NotificationManager", first.isRecycled)
    }

    @Test
    fun bitmapCache_concurrentSameKeyRequestsReturnSingleSharedBitmap() {
        val spec = NotificationVisualRenderer.resolveAnomalyVisualSpec(
            "Thermal throttling",
            "THERMAL_THROTTLING"
        )
        val workers = 16
        val ready = java.util.concurrent.CountDownLatch(workers)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(workers)
        try {
            val futures = (0 until workers).map {
                executor.submit<Bitmap> {
                    ready.countDown()
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    NotificationVisualRenderer.getOrRenderAnomalyLargeIcon(context, spec)
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

    @Test
    fun statusBarIndicatorUsesSinglePersistentNotificationId1001AndDeletesLegacyChannel() {
        val controller = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val service = controller.get()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        try {
            service.setActorRunningForTesting()
            service.setBatteryMonitoringEnabledForTesting(false)
            service.setStatusBarIndicatorSettingForTesting(
                com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting(
                    enabled = true,
                    parameter = com.superdenot.powerguard.domain.model.StatusBarParameter.CURRENT
                )
            )
            val unavailable = LiveBatteryState(isCurrentAvailable = false, isChargingStatusAvailable = false)
            service.refreshStatusBarIndicatorNotificationForTesting(unavailable, force = true)

            assertNotNull(Shadows.shadowOf(manager).getNotification(1001))
            assertEquals(null, Shadows.shadowOf(manager).getNotification(1003))
            assertEquals(null, manager.getNotificationChannel("powerguard_status_bar_indicator"))

            val channel = manager.getNotificationChannel("powerguard_live_monitor")
            assertNotNull(channel)
            assertEquals(android.app.NotificationManager.IMPORTANCE_LOW, channel!!.importance)
            val hudBody = NotificationVisualRenderer.formatStatusBarParameterText(
                unavailable,
                StatusBarParameter.CURRENT
            )
            val hudNotification = service.buildMonitorNotificationForTesting(hudBody, unavailable)
            assertEquals("Arus • —", hudNotification.extras.getCharSequence(Notification.EXTRA_TEXT))
            assertNotNull(hudNotification.smallIcon)
            assertTrue((hudNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
            assertTrue((hudNotification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0)

            service.setStatusBarIndicatorSettingForTesting(
                com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting(enabled = false)
            )
            service.refreshStatusBarIndicatorNotificationForTesting(unavailable, force = true)
            assertNotNull(Shadows.shadowOf(manager).getNotification(1001))
            assertEquals(null, Shadows.shadowOf(manager).getNotification(1003))
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun persistentNotificationUsesDynamicMetricIconOnlyWhenIndicatorToggleIsEnabled() {
        val controller = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val service = controller.get()
        try {
            val state = LiveBatteryState(
                currentMa = 2450,
                isCurrentAvailable = true,
                isCharging = true,
                isChargingStatusAvailable = true
            )
            service.setStatusBarIndicatorSettingForTesting(
                com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting(enabled = false)
            )
            val staticNotification = service.buildMonitorNotificationForTesting("Monitoring", state)
            service.setStatusBarIndicatorSettingForTesting(
                com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting(
                    enabled = true,
                    parameter = com.superdenot.powerguard.domain.model.StatusBarParameter.CURRENT
                )
            )
            val dynamicNotification = service.buildMonitorNotificationForTesting("Monitoring", state)

            assertNotNull(staticNotification.smallIcon)
            assertNotNull(dynamicNotification.smallIcon)
            assertEquals(android.graphics.drawable.Icon.TYPE_RESOURCE, staticNotification.smallIcon.type)
            assertEquals(android.graphics.drawable.Icon.TYPE_BITMAP, dynamicNotification.smallIcon.type)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun anomalyAlertRemainsOnIndependentNotificationId1002() {
        val controller = Robolectric.buildService(BatteryMonitorService::class.java).create()
        val service = controller.get()
        try {
            val anomaly = service.buildAnomalyAlertNotificationForTesting("Suhu baterai tinggi (42.5°C).")
            assertEquals("SUPERD GUARD • Peringatan Daya", anomaly.extras.getCharSequence(Notification.EXTRA_TITLE))
            assertNotNull(anomaly.smallIcon)
        } finally {
            controller.destroy()
        }
    }

}