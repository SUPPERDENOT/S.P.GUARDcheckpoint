package com.superdenot.powerguard.service

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Temporary Auto Start diagnostics. Writes only fixed technical state markers;
 * it does not collect sensor values, account data, or user content.
 */
internal object AutoStartDiagnosticLog {
    private const val TAG = "SPG_AUTO_START_DIAG"
    private const val DIRECTORY = "auto-start-diagnostics"
    private const val LOG_NAME = "auto_start_diagnostics.log"
    private const val EXPORT_DIRECTORY = "diagnostic_exports"
    private const val MAX_LOG_BYTES = 256L * 1024L
    private const val MAX_EXPORT_FILES = 3

    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SPG-AutoStart-Diagnostics").apply { isDaemon = true }
    }

    fun record(context: Context, event: String) {
        val safeEvent = sanitize(event)
        val elapsed = SystemClock.elapsedRealtime()
        Log.i(TAG, safeEvent)
        runCatching {
            writer.execute {
                runCatching {
                    appendRecord(context.applicationContext, elapsed, safeEvent, syncToDisk = false)
                }.onFailure { error ->
                    Log.w(TAG, "Diagnostic file write failed: ${error.javaClass.simpleName}")
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Diagnostic queue rejected an event: ${error.javaClass.simpleName}")
        }
    }

    /**
     * Persists a critical lifecycle marker before returning. The single writer
     * serializes this barrier after previously queued records; fd.sync() asks
     * the filesystem to flush the marker before the receiver releases goAsync().
     */
    suspend fun recordAndFlush(context: Context, event: String): Boolean {
        val safeEvent = sanitize(event)
        val elapsed = SystemClock.elapsedRealtime()
        Log.i(TAG, safeEvent)
        val completion = CompletableDeferred<Unit>()

        return try {
            writer.execute {
                try {
                    appendRecord(context.applicationContext, elapsed, safeEvent, syncToDisk = true)
                    completion.complete(Unit)
                } catch (error: Exception) {
                    completion.completeExceptionally(error)
                }
            }
            completion.await()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val cause = error.cause ?: error
            Log.w(TAG, "Diagnostic durable write failed: ${cause.javaClass.simpleName}")
            false
        }
    }

    suspend fun createExportSnapshot(context: Context): File = withContext(Dispatchers.IO) {
        writer.submit<File> {
            val appContext = context.applicationContext
            val source = logFile(appContext)
            if (!source.exists()) {
                source.parentFile?.mkdirs()
                source.writeText(
                    "SUPERD POWER GUARD — Auto Start diagnostic log\n" +
                        "Elapsed time is monotonic milliseconds since device boot.\n" +
                        "No sensor readings or personal content are collected by this logger.\n",
                    Charsets.UTF_8
                )
            }

            val exportDir = File(appContext.cacheDir, EXPORT_DIRECTORY)
            if (!exportDir.exists()) exportDir.mkdirs()
            exportDir.listFiles()?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_EXPORT_FILES - 1)?.forEach { it.delete() }

            val snapshot = File(exportDir, "spg_auto_start_diagnostics_${System.currentTimeMillis()}.txt")
            source.copyTo(snapshot, overwrite = true)
            snapshot
        }.get()
    }

    private fun appendRecord(
        context: Context,
        elapsedRealtime: Long,
        event: String,
        syncToDisk: Boolean
    ) {
        val logFile = logFile(context)
        val parent = logFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("Unable to create Auto Start diagnostic directory")
        }
        rotateIfNeeded(logFile)

        FileOutputStream(logFile, true).use { output ->
            output.write("$elapsedRealtime ms | $event\n".toByteArray(Charsets.UTF_8))
            output.flush()
            if (syncToDisk) output.fd.sync()
        }
    }

    private fun sanitize(event: String): String =
        event.replace('\n', ' ').replace('\r', ' ').take(500)

    private fun logFile(context: Context): File =
        File(File(context.filesDir, DIRECTORY), LOG_NAME)

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_LOG_BYTES) return
        val backup = File(file.parentFile, "$LOG_NAME.1")
        if (backup.exists()) backup.delete()
        file.renameTo(backup)
    }
}
