package com.superdenot.powerguard.domain.engine

data class ChargeGuardConfiguration(
    val chargeLimitEnabled: Boolean = false,
    val targetLevelPercent: Int = 85,
    val highTemperatureEnabled: Boolean = false,
    val highTemperatureThresholdCelsius: Int = 40
) {
    fun normalized(): ChargeGuardConfiguration = copy(
        targetLevelPercent = targetLevelPercent.takeIf { it in SUPPORTED_CHARGE_TARGETS } ?: 85,
        highTemperatureThresholdCelsius = highTemperatureThresholdCelsius
            .takeIf { it in SUPPORTED_TEMPERATURE_THRESHOLDS } ?: 40
    )

    companion object {
        val SUPPORTED_CHARGE_TARGETS = setOf(80, 85, 90, 95, 100)
        val SUPPORTED_TEMPERATURE_THRESHOLDS = setOf(40, 42, 45)
    }
}

enum class ChargeGuardAlertType { CHARGE_LIMIT_REACHED, HIGH_TEMPERATURE_REACHED }

data class ChargeGuardAlert(val type: ChargeGuardAlertType, val title: String, val message: String, val code: String)

data class ChargeGuardAlarmState(
    val lastChargeAlertSessionId: Long? = null,
    val lastChargeAlertTargetPercent: Int? = null,
    val temperatureArmed: Boolean = true,
    val lastTemperatureAlertAtEpochMs: Long = 0L,
    val temperatureThresholdCelsius: Int? = null
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

        if (config.chargeLimitEnabled && isCharging && isBatteryLevelAvailable &&
            sessionId != null && batteryLevel >= config.targetLevelPercent &&
            (next.lastChargeAlertSessionId != sessionId ||
                next.lastChargeAlertTargetPercent != config.targetLevelPercent)
        ) {
            alerts += ChargeGuardAlert(
                ChargeGuardAlertType.CHARGE_LIMIT_REACHED,
                "Batas pengisian tercapai",
                "Level baterai telah mencapai ${config.targetLevelPercent}%. Lepaskan charger jika ingin menghentikan pengisian.",
                "CHARGE_LIMIT_REACHED"
            )
            next = next.copy(lastChargeAlertSessionId = sessionId, lastChargeAlertTargetPercent = config.targetLevelPercent)
        }

        if (next.temperatureThresholdCelsius != config.highTemperatureThresholdCelsius) {
            next = next.copy(temperatureArmed = true, temperatureThresholdCelsius = config.highTemperatureThresholdCelsius)
        }

        if (!config.highTemperatureEnabled) {
            next = next.copy(temperatureArmed = true)
        } else if (isCharging && isTemperatureAvailable && temperatureCelsius.isFinite()) {
            val threshold = config.highTemperatureThresholdCelsius.toFloat()
            if (temperatureCelsius <= threshold - TEMPERATURE_REARM_HYSTERESIS_C) {
                next = next.copy(temperatureArmed = true)
            } else if (temperatureCelsius >= threshold && next.temperatureArmed) {
                val cooldownElapsed = next.lastTemperatureAlertAtEpochMs == 0L ||
                    nowEpochMs - next.lastTemperatureAlertAtEpochMs >= TEMPERATURE_COOLDOWN_MS
                if (cooldownElapsed) {
                    alerts += ChargeGuardAlert(
                        ChargeGuardAlertType.HIGH_TEMPERATURE_REACHED,
                        "Peringatan suhu baterai tinggi",
                        "Suhu baterai mencapai ${temperatureCelsius.toInt()}°C (ambang ${config.highTemperatureThresholdCelsius}°C). Periksa kondisi perangkat dan pengisian.",
                        "CHARGE_GUARD_HIGH_TEMPERATURE"
                    )
                    next = next.copy(temperatureArmed = false, lastTemperatureAlertAtEpochMs = nowEpochMs)
                }
            }
        }
        return ChargeGuardAlarmEvaluation(next, alerts)
    }
}
