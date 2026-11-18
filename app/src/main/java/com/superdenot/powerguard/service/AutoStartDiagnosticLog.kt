package com.superdenot.powerguard.service

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val safeEvent = event.replace('\n', ' ').replace('\r', ' ').take(500)
        val elapsed = SystemClock.elapsedRealtime()
        Log.i(TAG, safeEvent)
        runCatching {
            writer.execute {
                runCatching {
                    val logFile = logFile(context.applicationContext)
                    logFile.parentFile?.mkdirs()
                    rotateIfNeeded(logFile)
                    logFile.appendText("$elapsed ms | $safeEvent\n", Charsets.UTF_8)
                }.onFailure { error ->
                    Log.w(TAG, "Diagnostic file write failed: ${error.javaClass.simpleName}")
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Diagnostic queue rejected an event: ${error.javaClass.simpleName}")
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

    private fun logFile(context: Context): File =
        File(File(context.filesDir, DIRECTORY), LOG_NAME)

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_LOG_BYTES) return
        val backup = File(file.parentFile, "$LOG_NAME.1")
        if (backup.exists()) backup.delete()
        file.renameTo(backup)
    }
}
