package com.superdenot.powerguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.superdenot.powerguard.domain.engine.ChargeGuardAlarmPolicy
import com.superdenot.powerguard.domain.engine.ChargeGuardAlarmState
import com.superdenot.powerguard.domain.engine.ChargeGuardAlert
import com.superdenot.powerguard.domain.engine.ChargeGuardConfiguration
import com.superdenot.powerguard.domain.engine.ChargeGuardAlertType
import com.superdenot.powerguard.domain.engine.ChargeGuardRepeatMode

enum class ChargeGuardVibrationPattern(val title: String) {
    OFF("Off"),
    SHORT("Singkat"),
    DOUBLE("Ganda"),
    SOS("SOS"),
    CUSTOM("Kustom")
}

data class ChargeGuardAlertSoundSetting(
    val selection: String = DEFAULT,
    val title: String = "Nada Default",
    val vibrationPattern: ChargeGuardVibrationPattern = ChargeGuardVibrationPattern.DOUBLE,
    val customVibrationTimings: List<Long> = emptyList()
) {
    fun normalized(): ChargeGuardAlertSoundSetting {
        val safeTimings = customVibrationTimings.take(15)
            .map { it.coerceIn(MIN_VIBRATION_SEGMENT_MS, MAX_VIBRATION_SEGMENT_MS) }
            .let { if (it.size % 2 == 0) it.dropLast(1) else it }
        return copy(
            selection = selection.takeIf { it == DEFAULT || it == SILENT || it.startsWith("content://") || it.startsWith("file://") }
                ?: DEFAULT,
            title = title.trim().takeIf { it.isNotEmpty() } ?: when (selection) {
                SILENT -> "Hening"
                DEFAULT -> "Nada Default"
                else -> "Nada pilihan"
            },
            customVibrationTimings = safeTimings
        )
    }

    fun vibrationTimings(): LongArray = when (vibrationPattern) {
        ChargeGuardVibrationPattern.OFF -> longArrayOf()
        ChargeGuardVibrationPattern.SHORT -> longArrayOf(0L, 250L)
        ChargeGuardVibrationPattern.DOUBLE -> longArrayOf(0L, 200L, 120L, 200L)
        ChargeGuardVibrationPattern.SOS -> longArrayOf(
            0L, 120L, 90L, 120L, 90L, 120L, 180L,
            320L, 90L, 320L, 90L, 320L, 180L,
            120L, 90L, 120L, 90L, 120L
        )
        ChargeGuardVibrationPattern.CUSTOM -> normalized().customVibrationTimings
            .takeIf { it.size >= 3 }
            ?.let { (listOf(0L) + it).toLongArray() }
            ?: longArrayOf()
    }

    companion object {
        const val DEFAULT = "DEFAULT"
        const val SILENT = "SILENT"
        const val MIN_VIBRATION_SEGMENT_MS = 50L
        const val MAX_VIBRATION_SEGMENT_MS = 1_500L
    }
}

/** Preferences and per-incident alarm state are isolated from Room and sensor calibration. */
object ChargeGuardSettings {
    private const val PREFERENCES_NAME = "powerguard_charge_guard"
    private const val KEY_CHARGE_LIMIT_ENABLED = "charge_limit_enabled"
    private const val KEY_CHARGE_TARGET_PERCENT = "charge_target_percent"
    private const val KEY_HIGH_TEMPERATURE_ENABLED = "high_temperature_enabled"
    private const val KEY_HIGH_TEMPERATURE_THRESHOLD = "high_temperature_threshold_c"
    private const val KEY_LAST_CHARGE_SESSION_ID = "last_charge_alert_session_id"
    private const val KEY_LAST_CHARGE_TARGET = "last_charge_alert_target"
    private const val KEY_TEMPERATURE_ARMED = "temperature_alarm_armed"
    private const val KEY_LAST_TEMPERATURE_ALERT_AT = "last_temperature_alert_at"
    private const val KEY_STORED_TEMPERATURE_THRESHOLD = "stored_temperature_threshold_c"
    private const val KEY_REPEAT_MODE = "alert_repeat_mode"
    private const val KEY_CHARGE_REPEAT_SESSION_ID = "charge_repeat_session_id"
    private const val KEY_CHARGE_REPEAT_TARGET = "charge_repeat_target"
    private const val KEY_CHARGE_REPEAT_COUNT = "charge_repeat_count"
    private const val KEY_CHARGE_REPEAT_LAST_AT = "charge_repeat_last_at"
    private const val KEY_CHARGE_REPEAT_DISMISSED = "charge_repeat_dismissed"
    private const val KEY_TEMPERATURE_REPEAT_COUNT = "temperature_repeat_count"
    private const val KEY_TEMPERATURE_REPEAT_LAST_AT = "temperature_repeat_last_at"
    private const val KEY_TEMPERATURE_REPEAT_DISMISSED = "temperature_repeat_dismissed"
    private const val KEY_ALERT_SOUND_SELECTION = "alert_sound_selection"
    private const val KEY_ALERT_SOUND_TITLE = "alert_sound_title"
    private const val KEY_ALERT_VIBRATE = "alert_vibrate"
    private const val KEY_ALERT_VIBRATION_PATTERN = "alert_vibration_pattern"
    private const val KEY_ALERT_CUSTOM_VIBRATION_TIMINGS = "alert_custom_vibration_timings"
    private const val KEY_ACTIVE_ALERT_CHANNEL_ID = "active_alert_channel_id"
    private val lock = Any()

    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): ChargeGuardConfiguration {
        val prefs = preferences(context)
        return ChargeGuardConfiguration(
            chargeLimitEnabled = prefs.getBoolean(KEY_CHARGE_LIMIT_ENABLED, false),
            targetLevelPercent = prefs.getInt(KEY_CHARGE_TARGET_PERCENT, 85),
            highTemperatureEnabled = prefs.getBoolean(KEY_HIGH_TEMPERATURE_ENABLED, false),
            highTemperatureThresholdCelsius = prefs.getInt(KEY_HIGH_TEMPERATURE_THRESHOLD, 45),
            repeatMode = runCatching {
                ChargeGuardRepeatMode.valueOf(
                    prefs.getString(KEY_REPEAT_MODE, ChargeGuardRepeatMode.ONCE.name)
                        ?: ChargeGuardRepeatMode.ONCE.name
                )
            }.getOrDefault(ChargeGuardRepeatMode.ONCE)
        ).normalized()
    }

    fun readAlertSound(context: Context): ChargeGuardAlertSoundSetting {
        val prefs = preferences(context)
        val selection = prefs.getString(KEY_ALERT_SOUND_SELECTION, ChargeGuardAlertSoundSetting.DEFAULT)
            ?: ChargeGuardAlertSoundSetting.DEFAULT
        val defaultTitle = when (selection) {
            ChargeGuardAlertSoundSetting.SILENT -> "Hening"
            ChargeGuardAlertSoundSetting.DEFAULT -> "Nada Default"
            else -> "Nada pilihan"
        }
        val vibrationPattern = if (prefs.contains(KEY_ALERT_VIBRATION_PATTERN)) {
            runCatching {
                ChargeGuardVibrationPattern.valueOf(
                    prefs.getString(KEY_ALERT_VIBRATION_PATTERN, ChargeGuardVibrationPattern.DOUBLE.name)
                        ?: ChargeGuardVibrationPattern.DOUBLE.name
                )
            }.getOrDefault(ChargeGuardVibrationPattern.DOUBLE)
        } else if (prefs.getBoolean(KEY_ALERT_VIBRATE, true)) {
            ChargeGuardVibrationPattern.DOUBLE
        } else {
            ChargeGuardVibrationPattern.OFF
        }
        val customTimings = prefs.getString(KEY_ALERT_CUSTOM_VIBRATION_TIMINGS, "")
            .orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        return ChargeGuardAlertSoundSetting(
            selection = selection,
            title = prefs.getString(KEY_ALERT_SOUND_TITLE, defaultTitle) ?: defaultTitle,
            vibrationPattern = vibrationPattern,
            customVibrationTimings = customTimings
        ).normalized()
    }

    fun saveAlertSound(context: Context, setting: ChargeGuardAlertSoundSetting) {
        val normalized = setting.normalized()
        preferences(context).edit()
            .putString(KEY_ALERT_SOUND_SELECTION, normalized.selection)
            .putString(KEY_ALERT_SOUND_TITLE, normalized.title)
            .putBoolean(KEY_ALERT_VIBRATE, normalized.vibrationPattern != ChargeGuardVibrationPattern.OFF)
            .putString(KEY_ALERT_VIBRATION_PATTERN, normalized.vibrationPattern.name)
            .putString(KEY_ALERT_CUSTOM_VIBRATION_TIMINGS, normalized.customVibrationTimings.joinToString(","))
            .apply()
    }

    fun readActiveAlertChannelId(context: Context): String? =
        preferences(context).getString(KEY_ACTIVE_ALERT_CHANNEL_ID, null)

    fun saveActiveAlertChannelId(context: Context, channelId: String) {
        preferences(context).edit().putString(KEY_ACTIVE_ALERT_CHANNEL_ID, channelId).apply()
    }

    fun save(context: Context, configuration: ChargeGuardConfiguration) {
        val config = configuration.normalized()
        val prefs = preferences(context)
        val previous = read(context)
        val editor = prefs.edit()
            .putBoolean(KEY_CHARGE_LIMIT_ENABLED, config.chargeLimitEnabled)
            .putInt(KEY_CHARGE_TARGET_PERCENT, config.targetLevelPercent)
            .putBoolean(KEY_HIGH_TEMPERATURE_ENABLED, config.highTemperatureEnabled)
            .putInt(KEY_HIGH_TEMPERATURE_THRESHOLD, config.highTemperatureThresholdCelsius)
            .putString(KEY_REPEAT_MODE, config.repeatMode.name)
        if (!config.highTemperatureEnabled ||
            previous.highTemperatureThresholdCelsius != config.highTemperatureThresholdCelsius
        ) {
            editor.putBoolean(KEY_TEMPERATURE_ARMED, true)
        }
        editor.apply()
    }

    fun evaluate(
        context: Context,
        sessionId: Long?,
        isCharging: Boolean,
        batteryLevel: Int,
        isBatteryLevelAvailable: Boolean,
        temperatureCelsius: Float,
        isTemperatureAvailable: Boolean,
        nowEpochMs: Long
    ): List<ChargeGuardAlert> = synchronized(lock) {
        val prefs = preferences(context)
        val config = read(context)
        val storedSessionId = prefs.getLong(KEY_LAST_CHARGE_SESSION_ID, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        val storedTarget = prefs.getInt(KEY_LAST_CHARGE_TARGET, -1).takeIf { it >= 0 }
        val storedThreshold = prefs.getInt(KEY_STORED_TEMPERATURE_THRESHOLD, -1).takeIf { it >= 0 }
        val state = ChargeGuardAlarmState(
            lastChargeAlertSessionId = storedSessionId,
            lastChargeAlertTargetPercent = storedTarget,
            temperatureArmed = prefs.getBoolean(KEY_TEMPERATURE_ARMED, true),
            lastTemperatureAlertAtEpochMs = prefs.getLong(KEY_LAST_TEMPERATURE_ALERT_AT, 0L),
            temperatureThresholdCelsius = storedThreshold,
            chargeRepeatSessionId = prefs.getLong(KEY_CHARGE_REPEAT_SESSION_ID, Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE },
            chargeRepeatTargetPercent = prefs.getInt(KEY_CHARGE_REPEAT_TARGET, -1).takeIf { it >= 0 },
            chargeRepeatCount = prefs.getInt(KEY_CHARGE_REPEAT_COUNT, 0).coerceIn(0, ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT),
            chargeRepeatLastAtEpochMs = prefs.getLong(KEY_CHARGE_REPEAT_LAST_AT, 0L),
            chargeRepeatDismissed = prefs.getBoolean(KEY_CHARGE_REPEAT_DISMISSED, false),
            temperatureRepeatCount = prefs.getInt(KEY_TEMPERATURE_REPEAT_COUNT, 0).coerceIn(0, ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT),
            temperatureRepeatLastAtEpochMs = prefs.getLong(KEY_TEMPERATURE_REPEAT_LAST_AT, 0L),
            temperatureRepeatDismissed = prefs.getBoolean(KEY_TEMPERATURE_REPEAT_DISMISSED, false)
        )
        val result = ChargeGuardAlarmPolicy.evaluate(
            configuration = config,
            state = state,
            sessionId = sessionId,
            isCharging = isCharging,
            batteryLevel = batteryLevel,
            isBatteryLevelAvailable = isBatteryLevelAvailable,
            temperatureCelsius = temperatureCelsius,
            isTemperatureAvailable = isTemperatureAvailable,
            nowEpochMs = nowEpochMs
        )
        if (result.state != state) {
            prefs.edit()
                .putLong(KEY_LAST_CHARGE_SESSION_ID, result.state.lastChargeAlertSessionId ?: Long.MIN_VALUE)
                .putInt(KEY_LAST_CHARGE_TARGET, result.state.lastChargeAlertTargetPercent ?: -1)
                .putBoolean(KEY_TEMPERATURE_ARMED, result.state.temperatureArmed)
                .putLong(KEY_LAST_TEMPERATURE_ALERT_AT, result.state.lastTemperatureAlertAtEpochMs)
                .putInt(KEY_STORED_TEMPERATURE_THRESHOLD, result.state.temperatureThresholdCelsius ?: -1)
                .putLong(KEY_CHARGE_REPEAT_SESSION_ID, result.state.chargeRepeatSessionId ?: Long.MIN_VALUE)
                .putInt(KEY_CHARGE_REPEAT_TARGET, result.state.chargeRepeatTargetPercent ?: -1)
                .putInt(KEY_CHARGE_REPEAT_COUNT, result.state.chargeRepeatCount)
                .putLong(KEY_CHARGE_REPEAT_LAST_AT, result.state.chargeRepeatLastAtEpochMs)
                .putBoolean(KEY_CHARGE_REPEAT_DISMISSED, result.state.chargeRepeatDismissed)
                .putInt(KEY_TEMPERATURE_REPEAT_COUNT, result.state.temperatureRepeatCount)
                .putLong(KEY_TEMPERATURE_REPEAT_LAST_AT, result.state.temperatureRepeatLastAtEpochMs)
                .putBoolean(KEY_TEMPERATURE_REPEAT_DISMISSED, result.state.temperatureRepeatDismissed)
                .apply()
        }
        result.alerts
    }

    fun dismissRepeats(context: Context, alertType: ChargeGuardAlertType) = synchronized(lock) {
        val editor = preferences(context).edit()
        when (alertType) {
            ChargeGuardAlertType.CHARGE_LIMIT_REACHED ->
                editor.putBoolean(KEY_CHARGE_REPEAT_DISMISSED, true)
            ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED ->
                editor.putBoolean(KEY_TEMPERATURE_REPEAT_DISMISSED, true)
        }
        editor.apply()
    }

    /** Called directly by the power-disconnected receiver; does not start or stop the monitor service. */
    fun stopRepeats(context: Context) = synchronized(lock) {
        preferences(context).edit()
            .putLong(KEY_CHARGE_REPEAT_SESSION_ID, Long.MIN_VALUE)
            .putInt(KEY_CHARGE_REPEAT_TARGET, -1)
            .putInt(KEY_CHARGE_REPEAT_COUNT, 0)
            .putLong(KEY_CHARGE_REPEAT_LAST_AT, 0L)
            .putBoolean(KEY_CHARGE_REPEAT_DISMISSED, false)
            .putInt(KEY_TEMPERATURE_REPEAT_COUNT, 0)
            .putLong(KEY_TEMPERATURE_REPEAT_LAST_AT, 0L)
            .putBoolean(KEY_TEMPERATURE_REPEAT_DISMISSED, false)
            .apply()
    }
}
