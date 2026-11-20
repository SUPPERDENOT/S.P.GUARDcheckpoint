package com.superdenot.powerguard.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Keeps the app's production Application startup (which schedules WorkManager)
 * out of this focused file-logger unit test.
 */
class AutoStartDiagnosticTestApplication : Application()

@RunWith(RobolectricTestRunner::class)
@Config(application = AutoStartDiagnosticTestApplication::class, manifest = Config.NONE)
class AutoStartDiagnosticLogTest {

    @Test
    fun durableMarkerDrainsPreviouslyQueuedEventsBeforeReturning() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "auto-start-diagnostics").deleteRecursively()
        File(context.cacheDir, "diagnostic_exports").deleteRecursively()

        val queuedEvent = "TEST queued receiver entry"
        val durableEvent = "TEST receiver marker fsync complete"

        AutoStartDiagnosticLog.record(context, queuedEvent)
        assertTrue(AutoStartDiagnosticLog.recordAndFlush(context, durableEvent))

        val exportedLog = AutoStartDiagnosticLog.createExportSnapshot(context).readText()
        val queuedIndex = exportedLog.indexOf(queuedEvent)
        val durableIndex = exportedLog.indexOf(durableEvent)

        assertTrue("Queued event must be present before the flush marker", queuedIndex >= 0)
        assertTrue("Durable marker must be present in the exported snapshot", durableIndex >= 0)
        assertTrue("Queued events must remain ordered before the durable marker", queuedIndex < durableIndex)
    }
}
