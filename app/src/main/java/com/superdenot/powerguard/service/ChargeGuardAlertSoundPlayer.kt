package com.superdenot.powerguard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.superdenot.powerguard.data.repository.ChargeGuardAlertSoundSetting

/** Retains preview ringtone until its bounded playback window ends. */
object ChargeGuardAlertSoundPlayer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var activeRingtone: Ringtone? = null
    private var stopRunnable: Runnable? = null

    fun play(context: Context, setting: ChargeGuardAlertSoundSetting) {
        val appContext = context.applicationContext
        val normalized = setting.normalized()
        val timings = normalized.vibrationTimings()
        val systemDefault = Settings.System.DEFAULT_NOTIFICATION_URI
        val notificationDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val customUri = normalized.selection.takeUnless {
            it == ChargeGuardAlertSoundSetting.DEFAULT ||
                it == ChargeGuardAlertSoundSetting.SILENT || it.isBlank()
        }?.let(Uri::parse)
        val candidates = buildList {
            customUri?.let(::add)
            add(systemDefault)
            notificationDefault?.let(::add)
        }.distinct()
        mainHandler.post {
            if (normalized.selection == ChargeGuardAlertSoundSetting.SILENT) {
                synchronized(lock) { stopActiveLocked() }
            } else {
                playFirstAvailable(appContext, candidates, customUri)
            }
        }

        runCatching {
            val vibrator = ContextCompat.getSystemService(appContext, Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (timings.isEmpty()) {
                    vibrator.cancel()
                } else {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1), attributes)
                }
            }
        }
    }

    private fun playFirstAvailable(context: Context, candidates: List<Uri>, customUri: Uri?) {
        synchronized(lock) {
            stopActiveLocked()
            for (uri in candidates) {
                if (uri == customUri && !isReadable(context, uri)) continue
                val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull() ?: continue
                try {
                    activeRingtone = ringtone
                    ringtone.play()
                    val stop = Runnable {
                        synchronized(lock) {
                            if (activeRingtone === ringtone) {
                                runCatching { ringtone.stop() }
                                activeRingtone = null
                                stopRunnable = null
                            }
                        }
                    }
                    stopRunnable = stop
                    mainHandler.postDelayed(stop, PREVIEW_DURATION_MS)
                    return
                } catch (_: SecurityException) {
                    runCatching { ringtone.stop() }
                    activeRingtone = null
                } catch (_: Exception) {
                    runCatching { ringtone.stop() }
                    activeRingtone = null
                }
            }
        }
    }

    private fun isReadable(context: Context, uri: Uri): Boolean =
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length != 0L } == true
        } catch (_: SecurityException) {
            false
        } catch (_: java.io.FileNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }

    private fun stopActiveLocked() {
        stopRunnable?.let(mainHandler::removeCallbacks)
        stopRunnable = null
        activeRingtone?.let { runCatching { it.stop() } }
        activeRingtone = null
    }

    private const val PREVIEW_DURATION_MS = 4_000L
}
