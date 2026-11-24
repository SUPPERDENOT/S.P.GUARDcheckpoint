package com.superdenot.powerguard.service

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.superdenot.powerguard.data.repository.ChargeGuardAlertSoundSetting
import com.superdenot.powerguard.domain.engine.ChargeGuardAlertSoundUriResolver

object ChargeGuardAlertSoundPlayer {
    fun play(context: Context, setting: ChargeGuardAlertSoundSetting) {
        val appContext = context.applicationContext
        val defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val resolved = ChargeGuardAlertSoundUriResolver.resolve(
            selection = setting.selection,
            defaultUri = defaultUri.toString(),
            canOpenCustomUri = { raw ->
                appContext.contentResolver.openAssetFileDescriptor(Uri.parse(raw), "r")?.use { true } ?: false
            }
        )
        val played = playUri(appContext, resolved)
        if (!played && setting.selection != ChargeGuardAlertSoundSetting.SILENT && resolved != defaultUri.toString()) {
            playUri(appContext, defaultUri.toString())
        }
        if (setting.vibrate) {
            runCatching {
                val vibrator = ContextCompat.getSystemService(appContext, Vibrator::class.java)
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 220L, 110L, 220L), -1))
                }
            }
        }
    }

    private fun playUri(context: Context, rawUri: String?): Boolean {
        if (rawUri == null) return false
        val ringtone = runCatching { RingtoneManager.getRingtone(context, Uri.parse(rawUri)) }.getOrNull() ?: return false
        return runCatching { ringtone.play(); true }.getOrDefault(false)
    }
}
