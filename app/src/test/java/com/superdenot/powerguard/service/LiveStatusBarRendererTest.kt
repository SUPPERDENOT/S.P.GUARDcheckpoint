package com.superdenot.powerguard.service

import android.graphics.Bitmap
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.model.LiveBatteryState
import com.superdenot.powerguard.domain.model.StatusBarParameter
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class LiveStatusBarRendererTest {
    @After
    fun tearDown() = LiveStatusBarRenderer.clearCacheForTesting()

    @Test
    fun renderCreatesFullSizeAlphaBitmapForSystemSmallIcon() {
        val bitmap = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"))
        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.ALPHA_8, bitmap!!.config)
        assertEquals(24, bitmap.width)
        assertEquals(24, bitmap.height)
        assertFalse(bitmap.isRecycled)
    }

    @Test
    fun densityIsPartOfCacheKey() {
        val a = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"), 1f)
        val b = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"), 3f)
        assertEquals(24, a!!.width)
        assertEquals(72, b!!.width)
        assertNotSame(a, b)
        assertEquals(2, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun cacheKeyIncludesMetricValueUnitAndHitsIdenticalRequest() {
        val a = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"))
        val hit = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"))
        val metric = LiveStatusBarRenderer.render(StatusBarParameter.CURRENT, StatusBarReading("2.8", "W"))
        val value = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.9", "W"))
        val unit = LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "mW"))
        assertSame(a, hit)
        assertNotSame(a, metric)
        assertNotSame(a, value)
        assertNotSame(a, unit)
        assertEquals(4, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun twoLineLayoutUsesRequestedValueUnitRatioAndUnitWidth() {
        assertEquals(0.58f, LiveStatusBarRenderer.VALUE_HEIGHT_SHARE, 0.001f)
        assertEquals(0.42f, LiveStatusBarRenderer.UNIT_HEIGHT_SHARE, 0.001f)
        assertEquals(0.75f, LiveStatusBarRenderer.ROW_GAP_DP, 0.001f)
        assertEquals(0.88f, LiveStatusBarRenderer.UNIT_MAX_WIDTH_RATIO, 0.001f)
    }

    @Test
    fun renderAcceptsValuesUpToFourCharacters() {
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.CURRENT, StatusBarReading("0", "mA")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.CURRENT, StatusBarReading("-1.8", "A")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.CURRENT, StatusBarReading("-15", "mA")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("9999", "W")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("12345", "W")))
    }

    @Test
    fun renderRejectsBlankControlAndMoreThanFiveNumericCharacters() {
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("", "W")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("bad\nvalue", "W")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("123456", "W")))
        assertEquals(0, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun alphaContrastBoostStrengthensPartialGlyphCoverage() {
        assertEquals(0, LiveStatusBarRenderer.boostAlphaForTesting(0))
        assertTrue(LiveStatusBarRenderer.boostAlphaForTesting(32) > 32)
        assertTrue(LiveStatusBarRenderer.boostAlphaForTesting(128) > 128)
        assertEquals(255, LiveStatusBarRenderer.boostAlphaForTesting(255))
        assertEquals(0.08f, LiveStatusBarRenderer.ALPHA_CONTRAST_BOOST, 0.001f)
    }

    @Test
    fun resourceSourceUsesFullBleedSingleLineWithoutUnit() {
        listOf("AC", "USB", "WL", "BAT").forEach { sourceCode ->
            val bitmap = LiveStatusBarRenderer.render(
                StatusBarParameter.RESOURCE_SOURCE,
                StatusBarReading(sourceCode, "")
            )
            assertNotNull("Expected a one-line bitmap for $sourceCode", bitmap)
            assertEquals(24, bitmap!!.width)
            assertEquals(24, bitmap.height)
        }
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.RESOURCE_SOURCE, StatusBarReading("USB", "W")))
    }

    @Test
    fun cacheIsBoundedAndDoesNotRecycleEvictedBitmaps() {
        val first = LiveStatusBarRenderer.render(StatusBarParameter.BATTERY_PERCENTAGE, StatusBarReading("0", "%"))
        repeat(LiveStatusBarRenderer.MAX_CACHE_ENTRIES + 8) {
            LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading(it.toString(), "W"))
        }
        assertTrue(LiveStatusBarRenderer.cacheSizeForTesting() <= LiveStatusBarRenderer.MAX_CACHE_ENTRIES)
        assertFalse(first!!.isRecycled)
    }

    @Test
    fun concurrentIdenticalRequestsShareOneBitmap() {
        val count = 12
        val ready = java.util.concurrent.CountDownLatch(count)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(count)
        try {
            val futures = (0 until count).map {
                executor.submit<Bitmap?> {
                    ready.countDown()
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    LiveStatusBarRenderer.render(StatusBarParameter.POWER, StatusBarReading("2.8", "W"))
                }
            }
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
            start.countDown()
            val results = futures.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            assertTrue(results.all { it === results.first() })
            assertEquals(1, LiveStatusBarRenderer.cacheSizeForTesting())
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun adaptivePrecisionKeepsNegativeDecimalsWithinFourCharacters() {
        assertEquals("2.8", LiveStatusBarRenderer.formatAdaptive(2.8))
        assertEquals("7.3", LiveStatusBarRenderer.formatAdaptive(7.34))
        assertEquals("9.9", LiveStatusBarRenderer.formatAdaptive(9.9))
        assertEquals("12", LiveStatusBarRenderer.formatAdaptive(12.4))
        assertEquals("-15", LiveStatusBarRenderer.formatAdaptive(-15.2))
        assertEquals("-1.8", LiveStatusBarRenderer.formatAdaptive(-1.8))
        assertEquals("-2.5", LiveStatusBarRenderer.formatAdaptive(-2.48))
        assertEquals("10", LiveStatusBarRenderer.formatAdaptive(9.96))
        assertEquals("-10", LiveStatusBarRenderer.formatAdaptive(-9.96))
    }

    @Test
    fun allSixMetricsAndAdaptiveBatteryLevelAreCorrect() {
        val state = LiveBatteryState(
            level = 85, currentMa = 2800, voltageMv = 4120, powerWatt = 2.8f,
            tempCelsius = 2.8f, isCharging = true
        )
        assertEquals(StatusBarReading("2.8", "W"), LiveStatusBarRenderer.format(state, StatusBarParameter.POWER))
        assertEquals(StatusBarReading("2.8", "A"), LiveStatusBarRenderer.format(state, StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("4.1", "V"), LiveStatusBarRenderer.format(state, StatusBarParameter.VOLTAGE))
        assertEquals(StatusBarReading("2.8", "°C"), LiveStatusBarRenderer.format(state, StatusBarParameter.TEMPERATURE))
        assertEquals(StatusBarReading("85", "%"), LiveStatusBarRenderer.format(state, StatusBarParameter.BATTERY_PERCENTAGE))
        assertEquals(StatusBarReading("5.0", "%"), LiveStatusBarRenderer.format(state.copy(level = 5), StatusBarParameter.BATTERY_PERCENTAGE))
        assertEquals(StatusBarReading("AC", ""), LiveStatusBarRenderer.format(state.copy(isPowerConnected = true, isChargeSourceAvailable = true, chargeSource = "AC Adapter"), StatusBarParameter.RESOURCE_SOURCE))
        assertEquals(StatusBarReading("USB", ""), LiveStatusBarRenderer.format(state.copy(isPowerConnected = true, isChargeSourceAvailable = true, chargeSource = "USB Port"), StatusBarParameter.RESOURCE_SOURCE))
        assertEquals(StatusBarReading("WL", ""), LiveStatusBarRenderer.format(state.copy(isPowerConnected = true, isChargeSourceAvailable = true, chargeSource = "Wireless"), StatusBarParameter.RESOURCE_SOURCE))
        assertEquals(StatusBarReading("BAT", ""), LiveStatusBarRenderer.format(state.copy(isPowerConnected = false, isChargeSourceAvailable = true), StatusBarParameter.RESOURCE_SOURCE))
        assertEquals(StatusBarReading("—", ""), LiveStatusBarRenderer.format(state.copy(isChargeSourceAvailable = false), StatusBarParameter.RESOURCE_SOURCE))
    }

    @Test
    fun currentSwitchesBetweenMilliampAndAmp() {
        val state = LiveBatteryState(currentMa = 850)
        assertEquals(StatusBarReading("850", "mA"), LiveStatusBarRenderer.format(state, StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("1.2", "A"), LiveStatusBarRenderer.format(state.copy(currentMa = 1200), StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("-1.8", "A"), LiveStatusBarRenderer.format(state.copy(currentMa = -1800), StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("12", "A"), LiveStatusBarRenderer.format(state.copy(currentMa = 12000), StatusBarParameter.CURRENT))
    }

    @Test
    fun unavailableMetricsAreNotPresentedAsZero() {
        val state = LiveBatteryState(
            isCurrentAvailable = false, isVoltageAvailable = false, isPowerAvailable = false,
            isTemperatureAvailable = false, isLevelAvailable = false
        )
        StatusBarParameter.selectableValues.forEach {
            val reading = LiveStatusBarRenderer.format(state, it)
            assertEquals("—", reading.valueText)
            if (it == StatusBarParameter.RESOURCE_SOURCE) assertTrue(reading.unitText.isEmpty())
            else assertTrue(reading.unitText.isNotBlank())
        }
    }
}
