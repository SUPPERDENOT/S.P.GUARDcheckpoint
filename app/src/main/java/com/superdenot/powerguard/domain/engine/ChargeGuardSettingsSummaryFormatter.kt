package com.superdenot.powerguard.domain.engine

/** Two compact lines for the Settings Charge Guard summary card. */
object ChargeGuardSettingsSummaryFormatter {
    fun formatRules(configuration: ChargeGuardConfiguration): String {
        val config = configuration.normalized()
        val activeRules = buildList {
            if (config.chargeLimitEnabled) add("Target ${config.targetLevelPercent}%")
            if (config.highTemperatureEnabled) add("Suhu ≥${config.highTemperatureThresholdCelsius}°C")
        }
        return activeRules.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Nonaktif"
    }

    fun formatAlert(soundTitle: String, vibrationTitle: String): String {
        val sound = soundTitle.trim().ifEmpty { "Nada Default" }
        val vibration = vibrationTitle.trim().ifEmpty { "Ganda" }
        return "Nada: $sound · Getar: $vibration"
    }
}
