package com.superdenot.powerguard.domain.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryBenchmarkCalculatorTest {

    @Test
    fun aggregatesMeasuredBatterySideMetricsPerAccessoryLabel() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(
                    id = 1,
                    label = "Charger Bawaan",
                    startLevel = 20,
                    endLevel = 50,
                    startTime = 0L,
                    endTime = 30 * MINUTE,
                    samples = listOf(
                        sample(1_000L, current = 1_000, power = 4f, temp = 32f),
                        sample(2_000L, current = 2_000, power = 6f, temp = 38f)
                    ),
                    averageCurrentMa = 1_700,
                    maxTemperatureCelsius = 39f
                ),
                session(
                    id = 2,
                    label = "Kabel Utama",
                    startLevel = 70,
                    endLevel = 85,
                    startTime = 0L,
                    endTime = 60 * MINUTE,
                    samples = listOf(
                        sample(1_000L, current = 3_000, power = 8f, temp = 36f),
                        sample(2_000L, current = 3_000, power = 10f, temp = 40f)
                    )
                )
            )
        )

        assertEquals(2, result.groups.size)
        val charger = result.groups.single { it.label == "Charger Bawaan" }
        assertEquals(1, charger.sessionCount)
        assertEquals(5.0, charger.averageBatteryPowerW!!, 0.0001)
        assertEquals(1_700.0, charger.averageCurrentMa!!, 0.0001)
        assertEquals(60.0, charger.averageChargeRatePercentPerHour!!, 0.0001)
        assertEquals(7.0, charger.averageTemperatureRiseCelsius!!, 0.0001)
        assertEquals(20.0, charger.averageStartLevelPercent!!, 0.0001)
        assertEquals(32.0, charger.averageStartTemperatureCelsius!!, 0.0001)

        val cable = result.groups.single { it.label == "Kabel Utama" }
        assertEquals(9.0, cable.averageBatteryPowerW!!, 0.0001)
        assertEquals(3_000.0, cable.averageCurrentMa!!, 0.0001)
        assertEquals(15.0, cable.averageChargeRatePercentPerHour!!, 0.0001)
        assertEquals(4.0, cable.averageTemperatureRiseCelsius!!, 0.0001)
        assertTrue(result.comparabilityNotes.any { it.contains("Level awal") })
        assertTrue(result.comparabilityNotes.none { it.contains("Suhu awal") })
    }

    @Test
    fun chargeRateIsUnavailableBelowFiveMinuteMinimum() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(
                    id = 1,
                    label = "Kabel Utama",
                    startLevel = 20,
                    endLevel = 25,
                    startTime = 0L,
                    endTime = 4 * MINUTE,
                    samples = listOf(sample(1_000L, current = 500, power = 2f, temp = 30f))
                ),
                session(
                    id = 2,
                    label = "Adaptor Cadangan",
                    startLevel = 20,
                    endLevel = 25,
                    startTime = 0L,
                    endTime = 5 * MINUTE,
                    samples = listOf(sample(1_000L, current = 500, power = 2f, temp = 30f))
                )
            )
        )

        assertNull(result.groups.single { it.label == "Kabel Utama" }.averageChargeRatePercentPerHour)
        assertEquals(
            60.0,
            result.groups.single { it.label == "Adaptor Cadangan" }.averageChargeRatePercentPerHour!!,
            0.0001
        )
    }

    @Test
    fun unavailableSamplesDoNotBecomeMeasuredZero() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(
                    id = 1,
                    label = "Charger Bawaan",
                    startLevel = 40,
                    endLevel = 45,
                    startTime = 0L,
                    endTime = 10 * MINUTE,
                    samples = listOf(
                        sample(
                            timestamp = 1_000L,
                            current = 0,
                            power = 0f,
                            temp = 0f,
                            currentAvailable = false,
                            powerAvailable = false,
                            temperatureAvailable = false
                        )
                    )
                ),
                session(
                    id = 2,
                    label = "Kabel Cadangan",
                    startLevel = 40,
                    endLevel = 45,
                    startTime = 0L,
                    endTime = 10 * MINUTE,
                    samples = emptyList()
                )
            )
        )

        result.groups.forEach { group ->
            assertNull(group.averageBatteryPowerW)
            assertNull(group.averageCurrentMa)
            assertNull(group.averageTemperatureRiseCelsius)
            assertNull(group.averageStartTemperatureCelsius)
        }
    }

    @Test
    fun measuredZeroRemainsAValidMeasurement() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(
                    id = 1,
                    label = "Charger Bawaan",
                    startLevel = 40,
                    endLevel = 40,
                    startTime = 0L,
                    endTime = 10 * MINUTE,
                    samples = listOf(sample(1_000L, current = 0, power = 0f, temp = 33f))
                ),
                session(
                    id = 2,
                    label = "Kabel Cadangan",
                    startLevel = 40,
                    endLevel = 40,
                    startTime = 0L,
                    endTime = 10 * MINUTE,
                    samples = listOf(sample(1_000L, current = 0, power = 0f, temp = 33f))
                )
            )
        )

        result.groups.forEach { group ->
            assertEquals(0.0, group.averageBatteryPowerW!!, 0.0001)
            assertEquals(0.0, group.averageCurrentMa!!, 0.0001)
            assertEquals(0.0, group.averageChargeRatePercentPerHour!!, 0.0001)
            assertEquals(0.0, group.averageTemperatureRiseCelsius!!, 0.0001)
        }
    }

    @Test
    fun openSessionsAndBlankLabelsAreExcluded() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(
                    id = 1,
                    label = "Charger Bawaan",
                    startLevel = 40,
                    endLevel = null,
                    startTime = 0L,
                    endTime = null,
                    samples = listOf(sample(1_000L, current = 1_000, power = 4f, temp = 30f))
                ),
                session(
                    id = 2,
                    label = "  ",
                    startLevel = 40,
                    endLevel = 45,
                    startTime = 0L,
                    endTime = 10 * MINUTE,
                    samples = emptyList()
                )
            )
        )

        assertTrue(result.groups.isEmpty())
        assertTrue(result.comparabilityNotes.isEmpty())
    }

    @Test
    fun labelMatchingIsTrimmedAndCaseInsensitive() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(1, " Charger Bawaan ", 40, 45, 0L, 10 * MINUTE, emptyList()),
                session(2, "charger bawaan", 50, 55, 0L, 10 * MINUTE, emptyList())
            )
        )

        assertEquals(1, result.groups.size)
        assertEquals(2, result.groups.single().sessionCount)
    }

    @Test
    fun startingTemperatureDifferenceAboveFourDegreesAddsWarning() {
        val result = AccessoryBenchmarkCalculator.calculate(
            listOf(
                session(1, "A", 40, 45, 0L, 10 * MINUTE, listOf(sample(1_000L, 1_000, 4f, 30f))),
                session(2, "B", 42, 47, 0L, 10 * MINUTE, listOf(sample(1_000L, 1_000, 4f, 34.1f)))
            )
        )

        assertTrue(result.comparabilityNotes.any { it.contains("Suhu awal") && it.contains(">4°C") })
    }

    private fun session(
        id: Long,
        label: String,
        startLevel: Int,
        endLevel: Int?,
        startTime: Long,
        endTime: Long?,
        samples: List<AccessoryBenchmarkSample>,
        averageCurrentMa: Int? = samples.filter { it.isCurrentAvailable }.map { it.currentMa }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        maxTemperatureCelsius: Float? = samples.filter { it.isTemperatureAvailable }.maxOfOrNull { it.tempCelsius }
    ) = AccessoryBenchmarkSession(
        sessionId = id,
        accessoryLabel = label,
        startTimeMillis = startTime,
        endTimeMillis = endTime,
        startLevelPercent = startLevel,
        endLevelPercent = endLevel,
        averageCurrentMa = averageCurrentMa,
        maxTemperatureCelsius = maxTemperatureCelsius,
        samples = samples
    )

    private fun sample(
        timestamp: Long,
        current: Int,
        power: Float,
        temp: Float,
        currentAvailable: Boolean = true,
        powerAvailable: Boolean = true,
        temperatureAvailable: Boolean = true,
        chargingStatusAvailable: Boolean = true,
        isCharging: Boolean = true
    ) = AccessoryBenchmarkSample(
        timestampMillis = timestamp,
        currentMa = current,
        powerWatt = power,
        tempCelsius = temp,
        isCurrentAvailable = currentAvailable,
        isPowerAvailable = powerAvailable,
        isTemperatureAvailable = temperatureAvailable,
        isChargingStatusAvailable = chargingStatusAvailable,
        isCharging = isCharging
    )

    private companion object {
        const val MINUTE = 60_000L
    }
}
