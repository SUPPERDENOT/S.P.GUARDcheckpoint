package com.superdenot.powerguard.domain.model

enum class StatusBarParameter(val label: String) {
    POWER("Daya sisi baterai (W)"),
    CURRENT("Arus baterai (A / mA)"),
    VOLTAGE("Tegangan baterai (V)"),
    TEMPERATURE("Suhu baterai (°C)"),
    BATTERY_PERCENTAGE("Level baterai (%)"),
    RESOURCE_SOURCE("Status sumber daya"),
    /** Retained only to migrate the legacy preference value. */
    CHARGING_STATUS("Charging status");

    companion object {
        val selectableValues: List<StatusBarParameter> = listOf(
            POWER, CURRENT, VOLTAGE, TEMPERATURE, BATTERY_PERCENTAGE, RESOURCE_SOURCE
        )
        fun fromStoredValue(value: String?): StatusBarParameter = when (value) {
            "CHARGING_STATUS" -> RESOURCE_SOURCE
            else -> selectableValues.firstOrNull { it.name == value } ?: POWER
        }
    }
}

enum class StatusBarDisplayMode(val label: String) {
    SINGLE_METRIC("Satu metrik"),
    CUSTOM_ROTATION("Rotasi kustom");
    companion object {
        fun fromStoredValue(value: String?): StatusBarDisplayMode =
            values().firstOrNull { it.name == value } ?: SINGLE_METRIC
    }
}
