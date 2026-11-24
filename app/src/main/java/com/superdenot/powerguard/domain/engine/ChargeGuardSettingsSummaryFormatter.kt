package com.superdenot.powerguard.domain.engine

import com.superdenot.powerguard.data.repository.ChargeGuardAlertSoundSetting

/** Compact, single-line summary for the Settings Charge Guard card. */
object ChargeGuardSettingsSummaryFormatter {
    fun format(configuration: ChargeGuardConfiguration, sound: ChargeGuardAlertSoundSetting): String {
        val config = configuration.normalized()
        val activeRules = buildList {
            if (config.chargeLimitEnabled) add("Target ${config.targetLevelPercent}%")
            if (config.highTemperatureEnabled) add("Suhu ≥${config.highTemperatureThresholdCelsius}°C")
        }
        return if (activeRules.isEmpty()) "Nonaktif · ${sound.title}"
        else (activeRules + sound.title).joinToString(" · ")
    }
}
