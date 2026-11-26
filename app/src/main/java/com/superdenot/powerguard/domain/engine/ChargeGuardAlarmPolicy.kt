package com.superdenot.powerguard.domain.engine

import java.util.Locale

enum class ChargeGuardRepeatMode(val title: String, val intervalMs: Long) {
    ONCE("Sekali", 0L),
    EVERY_1_MINUTE("Tiap 1 mnt", 60_000L),
    EVERY_3_MINUTES("Tiap 3 mnt", 180_000L)
}

data class ChargeGuardConfiguration(
    val chargeLimitEnabled: Boolean = false,
    val targetLevelPercent: Int = 85,
    val highTemperatureEnabled: Boolean = false,
    val highTemperatureThresholdCelsius: Int = 45,
    val repeatMode: ChargeGuardRepeatMode = ChargeGuardRepeatMode.ONCE
) {
    fun normalized(): ChargeGuardConfiguration = copy(
        targetLevelPercent = targetLevelPercent.takeIf { it in SUPPORTED_CHARGE_TARGETS } ?: 85,
        highTemperatureThresholdCelsius = highTemperatureThresholdCelsius
            .takeIf { it in SUPPORTED_TEMPERATURE_THRESHOLDS } ?: 45
    )

    companion object {
        val SUPPORTED_CHARGE_TARGETS = setOf(80, 85, 90, 95, 100)
        val HIGH_TEMPERATURE_PRESETS = listOf(42, 45, 48, 50)
        val SUPPORTED_TEMPERATURE_THRESHOLDS = HIGH_TEMPERATURE_PRESETS.toSet()
        const val MAX_REPEATS_PER_INCIDENT = 5
    }
}

enum class ChargeGuardAlertType { CHARGE_LIMIT_REACHED, HIGH_TEMPERATURE_REACHED }

data class ChargeGuardAlert(val type: ChargeGuardAlertType, val title: String, val message: String, val code: String)

data class ChargeGuardAlarmState(
    val lastChargeAlertSessionId: Long? = null,
    val lastChargeAlertTargetPercent: Int? = null,
    val temperatureArmed: Boolean = true,
    val lastTemperatureAlertAtEpochMs: Long = 0L,
    val temperatureThresholdCelsius: Int? = null,
    val chargeRepeatSessionId: Long? = null,
    val chargeRepeatTargetPercent: Int? = null,
    val chargeRepeatCount: Int = 0,
    val chargeRepeatLastAtEpochMs: Long = 0L,
    val chargeRepeatDismissed: Boolean = false,
    val temperatureRepeatCount: Int = 0,
    val temperatureRepeatLastAtEpochMs: Long = 0L,
    val temperatureRepeatDismissed: Boolean = false
)

data class ChargeGuardAlarmEvaluation(val state: ChargeGuardAlarmState, val alerts: List<ChargeGuardAlert>)

/** Pure policy; never controls charging or changes sensor sampling. */
object ChargeGuardAlarmPolicy {
    const val TEMPERATURE_COOLDOWN_MS = 15 * 60 * 1_000L
    const val TEMPERATURE_REARM_HYSTERESIS_C = 2

    fun evaluate(
        configuration: ChargeGuardConfiguration,
        state: ChargeGuardAlarmState,
        sessionId: Long?,
        isCharging: Boolean,
        batteryLevel: Int,
        isBatteryLevelAvailable: Boolean,
        temperatureCelsius: Float,
        isTemperatureAvailable: Boolean,
        nowEpochMs: Long
    ): ChargeGuardAlarmEvaluation {
        val config = configuration.normalized()
        var next = state
        val alerts = mutableListOf<ChargeGuardAlert>()
        val repeatIntervalMs = config.repeatMode.intervalMs

        val chargeConditionActive = config.chargeLimitEnabled && isCharging &&
            isBatteryLevelAvailable && sessionId != null &&
            batteryLevel >= config.targetLevelPercent

        if (!chargeConditionActive) {
            next = next.copy(
                chargeRepeatSessionId = null,
                chargeRepeatTargetPercent = null,
                chargeRepeatCount = 0,
                chargeRepeatLastAtEpochMs = 0L,
                chargeRepeatDismissed = false
            )
        } else {
            val isNewChargeIncident = next.lastChargeAlertSessionId != sessionId ||
                next.lastChargeAlertTargetPercent != config.targetLevelPercent
            if (isNewChargeIncident) {
                alerts += ChargeGuardAlert(
                    ChargeGuardAlertType.CHARGE_LIMIT_REACHED,
                    "Batas pengisian tercapai",
                    "Level baterai telah mencapai ${config.targetLevelPercent}%. Lepaskan charger jika ingin menghentikan pengisian.",
                    "CHARGE_LIMIT_REACHED"
                )
                next = next.copy(
                    lastChargeAlertSessionId = sessionId,
                    lastChargeAlertTargetPercent = config.targetLevelPercent,
                    chargeRepeatSessionId = sessionId,
                    chargeRepeatTargetPercent = config.targetLevelPercent,
                    chargeRepeatCount = 0,
                    chargeRepeatLastAtEpochMs = nowEpochMs,
                    chargeRepeatDismissed = false
                )
            } else if (
                next.chargeRepeatSessionId == sessionId &&
                next.chargeRepeatTargetPercent == config.targetLevelPercent &&
                repeatIntervalMs > 0L &&
                !next.chargeRepeatDismissed &&
                next.chargeRepeatCount < ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT &&
                nowEpochMs - next.chargeRepeatLastAtEpochMs >= repeatIntervalMs
            ) {
                alerts += ChargeGuardAlert(
                    ChargeGuardAlertType.CHARGE_LIMIT_REACHED,
                    "Pengulangan: batas pengisian tercapai",
                    "Level baterai masih ${batteryLevel}% (target ${config.targetLevelPercent}%). Lepaskan charger jika ingin menghentikan pengisian.",
                    "CHARGE_LIMIT_REACHED"
                )
                next = next.copy(
                    chargeRepeatCount = next.chargeRepeatCount + 1,
                    chargeRepeatLastAtEpochMs = nowEpochMs
                )
            }
        }

        if (next.temperatureThresholdCelsius != config.highTemperatureThresholdCelsius) {
            next = next.copy(
                temperatureArmed = true,
                temperatureThresholdCelsius = config.highTemperatureThresholdCelsius,
                temperatureRepeatCount = 0,
                temperatureRepeatLastAtEpochMs = 0L,
                temperatureRepeatDismissed = false
            )
        }

        val temperatureAvailable = isTemperatureAvailable && temperatureCelsius.isFinite()
        val temperatureThreshold = config.highTemperatureThresholdCelsius.toFloat()
        val temperatureRearmLevel = temperatureThreshold - TEMPERATURE_REARM_HYSTERESIS_C

        if (!config.highTemperatureEnabled || !isCharging || !temperatureAvailable) {
            next = next.copy(
                temperatureRepeatCount = 0,
                temperatureRepeatLastAtEpochMs = 0L,
                temperatureRepeatDismissed = false
            )
            if (!config.highTemperatureEnabled) next = next.copy(temperatureArmed = true)
        } else if (temperatureCelsius < temperatureRearmLevel) {
            next = next.copy(
                temperatureArmed = true,
                temperatureRepeatCount = 0,
                temperatureRepeatLastAtEpochMs = 0L,
                temperatureRepeatDismissed = false
            )
        } else if (temperatureCelsius >= temperatureThreshold && next.temperatureArmed) {
            val cooldownElapsed = next.lastTemperatureAlertAtEpochMs == 0L ||
                nowEpochMs - next.lastTemperatureAlertAtEpochMs >= TEMPERATURE_COOLDOWN_MS
            if (cooldownElapsed) {
                alerts += ChargeGuardAlert(
                    ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED,
                    "Peringatan suhu baterai tinggi",
                    "Suhu baterai mencapai ${String.format(Locale.US, "%.1f", temperatureCelsius)}°C (ambang ${config.highTemperatureThresholdCelsius}°C). Periksa kondisi perangkat dan pengisian.",
                    "CHARGE_GUARD_HIGH_TEMPERATURE"
                )
                next = next.copy(
                    temperatureArmed = false,
                    lastTemperatureAlertAtEpochMs = nowEpochMs,
                    temperatureRepeatCount = 0,
                    temperatureRepeatLastAtEpochMs = nowEpochMs,
                    temperatureRepeatDismissed = false
                )
            }
        } else if (
            !next.temperatureArmed &&
            next.temperatureRepeatLastAtEpochMs > 0L &&
            repeatIntervalMs > 0L &&
            !next.temperatureRepeatDismissed &&
            next.temperatureRepeatCount < ChargeGuardConfiguration.MAX_REPEATS_PER_INCIDENT &&
            nowEpochMs - next.temperatureRepeatLastAtEpochMs >= repeatIntervalMs
        ) {
            alerts += ChargeGuardAlert(
                ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED,
                "Pengulangan: suhu baterai tinggi",
                "Suhu baterai masih ${String.format(Locale.US, "%.1f", temperatureCelsius)}°C (ambang ${config.highTemperatureThresholdCelsius}°C). Periksa kondisi perangkat dan pengisian.",
                "CHARGE_GUARD_HIGH_TEMPERATURE"
            )
            next = next.copy(
                temperatureRepeatCount = next.temperatureRepeatCount + 1,
                temperatureRepeatLastAtEpochMs = nowEpochMs
            )
        }

        return ChargeGuardAlarmEvaluation(next, alerts)
    }
}
