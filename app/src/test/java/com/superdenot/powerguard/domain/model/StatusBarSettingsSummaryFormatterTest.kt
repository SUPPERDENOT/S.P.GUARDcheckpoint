package com.superdenot.powerguard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarSettingsSummaryFormatterTest {
    @Test fun formatsCompactRotationSummaryInIndonesian() {
        assertEquals(
            "Rotasi 1 dtk · 4 metrik (W, A / mA, °C, Sumber) · 1x",
            StatusBarSettingsSummaryFormatter.format(
                StatusBarDisplayMode.CUSTOM_ROTATION,
                StatusBarParameter.POWER,
                listOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT, StatusBarParameter.TEMPERATURE, StatusBarParameter.RESOURCE_SOURCE),
                1,
                BatteryCurrentScale.STANDARD
            )
        )
    }

    @Test fun formatsSingleMetricSummary() {
        assertEquals(
            "V · 1000x",
            StatusBarSettingsSummaryFormatter.format(
                StatusBarDisplayMode.SINGLE_METRIC,
                StatusBarParameter.VOLTAGE,
                emptyList(),
                3,
                BatteryCurrentScale.OEM_MILLIAMPS_AS_MICROAMPS
            )
        )
    }
}
