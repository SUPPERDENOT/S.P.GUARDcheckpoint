package com.superdenot.powerguard.domain.benchmark

data class AccessoryBenchmarkRanking(
    val fastestLabel: String?,
    val warmLabels: Set<String>
)

object AccessoryBenchmarkRanker {
    const val WARM_PEAK_TEMPERATURE_CELSIUS = 40.0

    fun rank(groups: List<AccessoryBenchmarkGroup>): AccessoryBenchmarkRanking {
        val fastest = groups
            .filter { it.averageChargeRatePercentPerHour?.let { rate -> rate > 0.0 && rate.isFinite() } == true }
            .maxByOrNull { it.averageChargeRatePercentPerHour!! }
            ?.label
        val warm = groups.filter {
            it.averagePeakTemperatureCelsius?.let { temp ->
                temp.isFinite() && temp >= WARM_PEAK_TEMPERATURE_CELSIUS
            } == true
        }.mapTo(linkedSetOf()) { it.label }
        return AccessoryBenchmarkRanking(fastestLabel = fastest, warmLabels = warm)
    }
}
