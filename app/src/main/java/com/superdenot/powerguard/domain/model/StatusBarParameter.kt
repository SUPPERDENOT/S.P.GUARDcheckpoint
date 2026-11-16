package com.superdenot.powerguard.domain.model

/** User-selectable live value rendered in the Android notification status-bar icon. */
enum class StatusBarParameter(val label: String) {
    TEMPERATURE("Temperature"),
    VOLTAGE("Voltage"),
    CURRENT("Current"),
    POWER("Power"),
    BATTERY_PERCENTAGE("Battery percentage"),
    CHARGING_STATUS("Charging status");

    companion object {
        fun fromStoredValue(value: String?): StatusBarParameter =
            values().firstOrNull { it.name == value } ?: BATTERY_PERCENTAGE
    }
}
