package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeGuardSettingsSummaryFormatterTest {
    @Test fun activeThresholdsAndToneShareOneLine() {
        val summary = ChargeGuardSettingsSummaryFormatter.format(
            ChargeGuardConfiguration(true, 90, true, 45),
            "Garry's Mod (x64)"
        )
        assertEquals("Target 90% · Suhu ≥45°C · Garry's Mod (x64)", summary)
    }

    @Test fun inactiveGuardStillShowsSelectedTone() {
        assertEquals(
            "Nonaktif · Nada Default",
            ChargeGuardSettingsSummaryFormatter.format(ChargeGuardConfiguration(), "Nada Default")
        )
    }

    @Test fun invalidThresholdIsNormalizedBeforeFormatting() {
        assertEquals(
            "Suhu ≥40°C · Nada Default",
            ChargeGuardSettingsSummaryFormatter.format(
                ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 48),
                "Nada Default"
            )
        )
    }
}
