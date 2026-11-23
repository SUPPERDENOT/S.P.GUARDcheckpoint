package com.superdenot.powerguard.domain.model
/** User-selectable live value rendered in the Android notification status-bar icon. */
enum class StatusBarParameter(val label: String) {
    TEMPERATURE("Suhu baterai (°C)"), VOLTAGE("Tegangan baterai (V)"),
    CURRENT("Arus baterai (A / mA)"), POWER("Daya sisi baterai (W)"),
    BATTERY_PERCENTAGE("Level baterai (%)"),
    /** Retained only for legacy notification rendering; not a selectable metric. */
    CHARGING_STATUS("Charging status");
    companion object {
        val selectableValues: List<StatusBarParameter> =
            listOf(POWER, CURRENT, VOLTAGE, TEMPERATURE, BATTERY_PERCENTAGE)
        fun fromStoredValue(value: String?): StatusBarParameter =
            selectableValues.firstOrNull { it.name == value } ?: POWER
    }
}
