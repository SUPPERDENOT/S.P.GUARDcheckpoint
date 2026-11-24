package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeGuardSettingsSummaryFormatterTest {
    @Test fun activeThresholdsAreFormattedOnFirstSummaryLine() {
        assertEquals(
            "Target 90% · Suhu ≥45°C",
            ChargeGuardSettingsSummaryFormatter.formatRules(ChargeGuardConfiguration(true, 90, true, 45))
        )
    }

    @Test fun inactiveGuardUsesCompactFirstSummaryLine() {
        assertEquals("Nonaktif", ChargeGuardSettingsSummaryFormatter.formatRules(ChargeGuardConfiguration()))
    }

    @Test fun toneAndVibrationAreFormattedOnSecondSummaryLine() {
        assertEquals(
            "Nada: Nada pilihan · Getar: SOS",
            ChargeGuardSettingsSummaryFormatter.formatAlert("Nada pilihan", "SOS")
        )
    }


    @Test fun nonDefaultRepeatModeIsIncludedInFirstSummaryLine() {
        assertEquals(
            "Target 80% · Suhu ≥40°C · Ulang 1m",
            ChargeGuardSettingsSummaryFormatter.formatRules(
                ChargeGuardConfiguration(
                    chargeLimitEnabled = true,
                    targetLevelPercent = 80,
                    highTemperatureEnabled = true,
                    highTemperatureThresholdCelsius = 40,
                    repeatMode = ChargeGuardRepeatMode.EVERY_1_MINUTE
                )
            )
        )
        assertEquals(
            "Nonaktif · Ulang 3m",
            ChargeGuardSettingsSummaryFormatter.formatRules(
                ChargeGuardConfiguration(repeatMode = ChargeGuardRepeatMode.EVERY_3_MINUTES)
            )
        )
    }

    @Test fun invalidThresholdIsNormalizedBeforeFormatting() {
        assertEquals(
            "Suhu ≥40°C",
            ChargeGuardSettingsSummaryFormatter.formatRules(
                ChargeGuardConfiguration(highTemperatureEnabled = true, highTemperatureThresholdCelsius = 48)
            )
        )
    }
}
