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
    @After fun tearDown() = LiveStatusBarRenderer.clearCacheForTesting()
    @Test fun renderCreatesAlphaBitmapWithExpectedDimensions() {
        val b=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"))
        assertNotNull(b); assertEquals(Bitmap.Config.ALPHA_8,b!!.config); assertEquals(48,b.width); assertEquals(48,b.height); assertFalse(b.isRecycled)
    }
    @Test fun densityIsPartOfCacheKey() {
        val a=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"),1f)
        val b=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"),3f)
        assertEquals(48,a!!.width); assertEquals(144,b!!.width); assertNotSame(a,b); assertEquals(2,LiveStatusBarRenderer.cacheSizeForTesting())
    }
    @Test fun cacheKeyIncludesMetricValueUnitAndHitsIdenticalRequest() {
        val a=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"))
        val hit=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"))
        val metric=LiveStatusBarRenderer.render(StatusBarParameter.CURRENT,StatusBarReading("2.8","W"))
        val value=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.9","W"))
        val unit=LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","mW"))
        assertSame(a,hit); assertNotSame(a,metric); assertNotSame(a,value); assertNotSame(a,unit); assertEquals(4,LiveStatusBarRenderer.cacheSizeForTesting())
    }
    @Test fun renderAcceptsZeroNegativeAndLongValues() {
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.CURRENT,StatusBarReading("0","mA")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.CURRENT,StatusBarReading("-1.8","A")))
        assertNotNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("99999","W")))
    }
    @Test fun renderRejectsBlankControlAndOversizedInput() {
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("","W")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("bad\nvalue","W")))
        assertNull(LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("123456789","W")))
        assertEquals(0,LiveStatusBarRenderer.cacheSizeForTesting())
    }
    @Test fun cacheIsBoundedAndDoesNotRecycleEvictedBitmaps() {
        val first=LiveStatusBarRenderer.render(StatusBarParameter.BATTERY_PERCENTAGE,StatusBarReading("0","%"))
        repeat(LiveStatusBarRenderer.MAX_CACHE_ENTRIES+8){LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading(it.toString(),"W"))}
        assertTrue(LiveStatusBarRenderer.cacheSizeForTesting()<=LiveStatusBarRenderer.MAX_CACHE_ENTRIES); assertFalse(first!!.isRecycled)
    }
    @Test fun concurrentIdenticalRequestsShareOneBitmap() {
        val n=12; val ready=java.util.concurrent.CountDownLatch(n); val start=java.util.concurrent.CountDownLatch(1)
        val executor=java.util.concurrent.Executors.newFixedThreadPool(n)
        try {
            val futures=(0 until n).map{executor.submit<Bitmap?>{ready.countDown();check(start.await(5,java.util.concurrent.TimeUnit.SECONDS));LiveStatusBarRenderer.render(StatusBarParameter.POWER,StatusBarReading("2.8","W"))}}
            assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown()
            val results=futures.map{it.get(10,java.util.concurrent.TimeUnit.SECONDS)}
            assertTrue(results.all{it===results.first()});assertEquals(1,LiveStatusBarRenderer.cacheSizeForTesting())
        } finally {start.countDown();executor.shutdownNow()}
    }
    @Test fun adaptivePrecisionAndAllFiveMetricsAreCorrect() {
        val s=LiveBatteryState(level=85,currentMa=2800,voltageMv=4120,powerWatt=2.8f,tempCelsius=2.8f,isCharging=true)
        assertEquals(StatusBarReading("2.8","W"),LiveStatusBarRenderer.format(s,StatusBarParameter.POWER))
        assertEquals(StatusBarReading("2.8","A"),LiveStatusBarRenderer.format(s,StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("4.1","V"),LiveStatusBarRenderer.format(s,StatusBarParameter.VOLTAGE))
        assertEquals(StatusBarReading("2.8","°C"),LiveStatusBarRenderer.format(s,StatusBarParameter.TEMPERATURE))
        assertEquals(StatusBarReading("85","%"),LiveStatusBarRenderer.format(s,StatusBarParameter.BATTERY_PERCENTAGE))
        assertEquals("12",LiveStatusBarRenderer.formatAdaptive(12.4));assertEquals("-15",LiveStatusBarRenderer.formatAdaptive(-15.2));assertEquals("-1.8",LiveStatusBarRenderer.formatAdaptive(-1.8))
    }
    @Test fun currentSwitchesBetweenMilliampAndAmp() {
        val s=LiveBatteryState(currentMa=850)
        assertEquals(StatusBarReading("850","mA"),LiveStatusBarRenderer.format(s,StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("1.2","A"),LiveStatusBarRenderer.format(s.copy(currentMa=1200),StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("-1.8","A"),LiveStatusBarRenderer.format(s.copy(currentMa=-1800),StatusBarParameter.CURRENT))
        assertEquals(StatusBarReading("12","A"),LiveStatusBarRenderer.format(s.copy(currentMa=12000),StatusBarParameter.CURRENT))
    }
    @Test fun unavailableMetricsAreNotPresentedAsZero() {
        val s=LiveBatteryState(isCurrentAvailable=false,isVoltageAvailable=false,isPowerAvailable=false,isTemperatureAvailable=false,isLevelAvailable=false)
        StatusBarParameter.selectableValues.forEach{val r=LiveStatusBarRenderer.format(s,it);assertEquals("—",r.valueText);assertTrue(r.unitText.isNotBlank())}
    }
}
