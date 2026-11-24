package com.superdenot.powerguard.domain.engine

/** Compact, single-line summary for the Settings Charge Guard card. */
object ChargeGuardSettingsSummaryFormatter {
    fun format(configuration: ChargeGuardConfiguration, soundTitle: String): String {
        val config = configuration.normalized()
        val activeRules = buildList {
            if (config.chargeLimitEnabled) add("Target ${config.targetLevelPercent}%")
            if (config.highTemperatureEnabled) add("Suhu ≥${config.highTemperatureThresholdCelsius}°C")
        }
        val title = soundTitle.trim().ifEmpty { "Nada Default" }
        return if (activeRules.isEmpty()) "Nonaktif · $title"
        else (activeRules + title).joinToString(" · ")
    }
}
