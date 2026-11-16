package com.superdenot.powerguard.service

import android.graphics.Bitmap
import android.graphics.Color
import com.superdenot.powerguard.PowerGuardTestApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class LiveStatusBarRendererTest {

    @After
    fun tearDown() {
        LiveStatusBarRenderer.clearCacheForTesting()
    }

    @Test
    fun render_producesAlphaOnlyBitmapWithExpectedDimensionsAndVisibleGlyph() {
        val bitmap = LiveStatusBarRenderer.render("33°")

        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.ALPHA_8, bitmap!!.config)
        assertEquals(LiveStatusBarRenderer.ICON_WIDTH, bitmap.width)
        assertEquals(LiveStatusBarRenderer.ICON_HEIGHT, bitmap.height)
        assertFalse(bitmap.isRecycled)

        var visiblePixels = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) visiblePixels++
            }
        }
        assertTrue("Expected rendered glyph pixels", visiblePixels > 0)
    }

    @Test
    fun render_cachesIdenticalDisplayTextAndSeparatesDifferentValues() {
        val first = LiveStatusBarRenderer.render("4.1V")
        val repeated = LiveStatusBarRenderer.render(" 4.1V ")
        val different = LiveStatusBarRenderer.render("4.2V")

        assertSame(first, repeated)
        assertNotNull(different)
        assertNotSame(first, different)
        assertEquals(2, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun render_acceptsZeroNegativeAndExtremeDisplayValues() {
        assertNotNull(LiveStatusBarRenderer.render("0A"))
        assertNotNull(LiveStatusBarRenderer.render("-950mA"))
        assertNotNull(LiveStatusBarRenderer.render("99999W"))
        assertEquals(3, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun render_returnsNullForUnavailableBlankControlOrOversizedInput() {
        assertNull(LiveStatusBarRenderer.render(null))
        assertNull(LiveStatusBarRenderer.render(""))
        assertNull(LiveStatusBarRenderer.render("   "))
        assertNull(LiveStatusBarRenderer.render("bad\nvalue"))
        assertNull(LiveStatusBarRenderer.render("123456789"))
        assertEquals(0, LiveStatusBarRenderer.cacheSizeForTesting())
    }

    @Test
    fun render_cacheIsBoundedAndEvictionDoesNotRecycleIssuedBitmaps() {
        val first = LiveStatusBarRenderer.render("0%")
        repeat(LiveStatusBarRenderer.MAX_CACHE_ENTRIES + 8) { index ->
            LiveStatusBarRenderer.render("V$index")
        }

        assertTrue(LiveStatusBarRenderer.cacheSizeForTesting() <= LiveStatusBarRenderer.MAX_CACHE_ENTRIES)
        assertNotNull(first)
        assertFalse(first!!.isRecycled)
    }

    @Test
    fun render_concurrentRequestsForSameTextReturnOneCachedBitmap() {
        val workers = 12
        val ready = java.util.concurrent.CountDownLatch(workers)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(workers)
        try {
            val futures = (0 until workers).map {
                executor.submit<Bitmap?> {
                    ready.countDown()
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    LiveStatusBarRenderer.render("85%")
                }
            }
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
            start.countDown()
            val results = futures.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            val first = results.first()
            assertNotNull(first)
            assertTrue(results.all { it === first })
            assertEquals(1, LiveStatusBarRenderer.cacheSizeForTesting())
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }
}
