package com.superdenot.powerguard.domain.benchmark

data class AccessoryBenchmarkSample(
    val timestampMillis: Long,
    val currentMa: Int,
    val powerWatt: Float,
    val tempCelsius: Float,
    val isCurrentAvailable: Boolean,
    val isPowerAvailable: Boolean,
    val isTemperatureAvailable: Boolean
)

data class AccessoryBenchmarkSession(
    val sessionId: Long,
    val accessoryLabel: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val startLevelPercent: Int,
    val endLevelPercent: Int?,
    val averageCurrentMa: Int?,
    val maxTemperatureCelsius: Float?,
    val samples: List<AccessoryBenchmarkSample>
)

data class AccessoryBenchmarkGroup(
    val label: String,
    val sessionCount: Int,
    val averageBatteryPowerW: Double?,
    val averageCurrentMa: Double?,
    val averageChargeRatePercentPerHour: Double?,
    val averageTemperatureRiseCelsius: Double?,
    val averageStartLevelPercent: Double?,
    val averageStartTemperatureCelsius: Double?
)

data class AccessoryBenchmarkResult(
    val groups: List<AccessoryBenchmarkGroup> = emptyList(),
    val comparabilityNotes: List<String> = emptyList()
)
