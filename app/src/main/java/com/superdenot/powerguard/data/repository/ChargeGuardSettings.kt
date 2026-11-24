package com.superdenot.powerguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.superdenot.powerguard.domain.engine.ChargeGuardAlarmPolicy
import com.superdenot.powerguard.domain.engine.ChargeGuardAlarmState
import com.superdenot.powerguard.domain.engine.ChargeGuardAlert
import com.superdenot.powerguard.domain.engine.ChargeGuardConfiguration

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
    private val lock = Any()

    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): ChargeGuardConfiguration {
        val prefs = preferences(context)
        return ChargeGuardConfiguration(
            chargeLimitEnabled = prefs.getBoolean(KEY_CHARGE_LIMIT_ENABLED, false),
            targetLevelPercent = prefs.getInt(KEY_CHARGE_TARGET_PERCENT, 85),
            highTemperatureEnabled = prefs.getBoolean(KEY_HIGH_TEMPERATURE_ENABLED, false),
            highTemperatureThresholdCelsius = prefs.getInt(KEY_HIGH_TEMPERATURE_THRESHOLD, 40)
        ).normalized()
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
        if (!config.chargeLimitEnabled && !config.highTemperatureEnabled) return@synchronized emptyList()
        val storedSessionId = prefs.getLong(KEY_LAST_CHARGE_SESSION_ID, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        val storedTarget = prefs.getInt(KEY_LAST_CHARGE_TARGET, -1).takeIf { it >= 0 }
        val storedThreshold = prefs.getInt(KEY_STORED_TEMPERATURE_THRESHOLD, -1).takeIf { it >= 0 }
        val state = ChargeGuardAlarmState(
            lastChargeAlertSessionId = storedSessionId,
            lastChargeAlertTargetPercent = storedTarget,
            temperatureArmed = prefs.getBoolean(KEY_TEMPERATURE_ARMED, true),
            lastTemperatureAlertAtEpochMs = prefs.getLong(KEY_LAST_TEMPERATURE_ALERT_AT, 0L),
            temperatureThresholdCelsius = storedThreshold
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
                .apply()
        }
        result.alerts
    }
}
