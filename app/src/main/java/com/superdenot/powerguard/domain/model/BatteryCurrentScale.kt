package com.superdenot.powerguard.domain.model
/** Explicit OEM calibration for BatteryManager.CURRENT_NOW raw values. */
enum class BatteryCurrentScale(val label: String, val multiplier: Double) {
    STANDARD("1x", 1.0), OEM_MILLIAMPS_AS_MICROAMPS("1000x", 1000.0),
    OEM_MICROAMPS_AS_MILLIAMPS("0.001x", 0.001);
    companion object {
        fun fromStoredValue(value: String?): BatteryCurrentScale =
            values().firstOrNull { it.name == value } ?: STANDARD
    }
}
