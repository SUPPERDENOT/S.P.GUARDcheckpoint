package com.superdenot.powerguard.domain.model

object StatusBarSettingsSummaryFormatter {
    fun format(
        displayMode: StatusBarDisplayMode,
        parameter: StatusBarParameter,
        rotationParameters: List<StatusBarParameter>,
        rotationIntervalSeconds: Int,
        currentScale: BatteryCurrentScale
    ): String {
        val scale = currentScale.label
        if (displayMode != StatusBarDisplayMode.CUSTOM_ROTATION) return "${shortLabel(parameter)} · $scale"
        val labels = StatusBarParameter.selectableValues.filter { it in rotationParameters }.map(::shortLabel)
        return "Rotasi ${rotationIntervalSeconds.coerceIn(1, 60)} dtk · ${labels.size} metrik (${labels.joinToString(", ")}) · $scale"
    }

    private fun shortLabel(parameter: StatusBarParameter): String = when (parameter) {
        StatusBarParameter.POWER -> "W"
        StatusBarParameter.CURRENT -> "A / mA"
        StatusBarParameter.VOLTAGE -> "V"
        StatusBarParameter.TEMPERATURE -> "°C"
        StatusBarParameter.BATTERY_PERCENTAGE -> "%"
        StatusBarParameter.RESOURCE_SOURCE -> "Sumber"
        StatusBarParameter.CHARGING_STATUS -> "Status"
    }
}
