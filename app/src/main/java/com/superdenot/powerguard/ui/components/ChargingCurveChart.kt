package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.domain.analytics.ChargingThermalPhaseAnalyzer
import com.superdenot.powerguard.domain.analytics.ChargingThermalSample
import com.superdenot.powerguard.domain.analytics.ChargingThermalSummary
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.CrimsonAnomaly
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.ui.theme.CrimsonAnomaly
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun formatCurveReading(isAvailable: Boolean, value: String): String =
    if (isAvailable) value else "—"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChargingCurveChart(
    samples: List<BatterySampleEntity>,
    modifier: Modifier = Modifier
) {
    val selectedIndexState = remember(samples) {
        mutableIntStateOf((samples.lastIndex).coerceAtLeast(0))
    }
    val selectedIndex = selectedIndexState.intValue.coerceIn(0, (samples.lastIndex).coerceAtLeast(0))
    val selectedSample = samples.getOrNull(selectedIndex)
    val thermalSummary = remember(samples) {
        ChargingThermalPhaseAnalyzer.analyze(samples.map { sample ->
            ChargingThermalSample(sample.timestamp, sample.tempCelsius, sample.isTemperatureAvailable,
                sample.currentMa, sample.isCurrentAvailable, sample.voltageMv, sample.isVoltageAvailable,
                sample.isChargingStatusAvailable, sample.isCharging)
        })
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val focusLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss", Locale("id", "ID"))
            .withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 6.dp)
    ) {
        Text(
            text = "Analisis Kurva Pengisian",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LegendItem(color = EmeraldCharge, label = "Arus (mA)")
            LegendItem(color = AmberThermal, label = "Suhu (°C)")
            LegendItem(color = CyanFastCharge, label = "Baterai (%)")
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (samples.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Menunggu minimal 2 sampel untuk membentuk grafik...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val currentSamples = samples.filter { it.isCurrentAvailable }
            val temperatureSamples = samples.filter { it.isTemperatureAvailable }
            val minMa = if (currentSamples.isNotEmpty()) min(0f, currentSamples.minOf { it.currentMa.toFloat() }) else 0f
            val maxMa = if (currentSamples.isNotEmpty()) max(500f, currentSamples.maxOf { it.currentMa.toFloat() }) else 500f
            val minTemp = if (temperatureSamples.isNotEmpty()) min(20f, temperatureSamples.minOf { it.tempCelsius }) else 20f
            val maxTemp = if (temperatureSamples.isNotEmpty()) max(50f, temperatureSamples.maxOf { it.tempCelsius }) else 50f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .pointerInput(samples) {
                        detectTapGestures { offset ->
                            selectedIndexState.intValue = (
                                (offset.x / size.width.coerceAtLeast(1) * samples.lastIndex)
                                    .roundToInt()
                                ).coerceIn(0, samples.lastIndex)
                        }
                    }
                    .pointerInput(samples) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndexState.intValue = (
                                    (offset.x / size.width.coerceAtLeast(1) * samples.lastIndex)
                                        .roundToInt()
                                    ).coerceIn(0, samples.lastIndex)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                selectedIndexState.intValue = (
                                    (change.position.x / size.width.coerceAtLeast(1) * samples.lastIndex)
                                        .roundToInt()
                                    ).coerceIn(0, samples.lastIndex)
                            }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val stepX = chartWidth / samples.lastIndex.coerceAtLeast(1)

                fun pointY(value: Float, minValue: Float, maxValue: Float): Float {
                    val range = (maxValue - minValue).coerceAtLeast(1f)
                    val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
                    return chartHeight - normalized * chartHeight
                }

                fun buildPath(
                    valueSelector: (BatterySampleEntity) -> Float,
                    availabilitySelector: (BatterySampleEntity) -> Boolean,
                    minValue: Float,
                    maxValue: Float
                ): Path {
                    val path = Path()
                    var segmentOpen = false
                    samples.forEachIndexed { index, sample ->
                        if (!availabilitySelector(sample)) {
                            // Missing readings terminate the current segment; never interpolate across unavailable data.
                            segmentOpen = false
                        } else {
                            val point = Offset(
                                x = index * stepX,
                                y = pointY(valueSelector(sample), minValue, maxValue)
                            )
                            if (segmentOpen) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                            segmentOpen = true
                        }
                    }
                    return path
                }

                for (line in 0..4) {
                    val y = chartHeight * (line / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawPath(
                    path = buildPath({ it.batteryLevel.toFloat() }, { it.isLevelAvailable }, 0f, 100f),
                    color = CyanFastCharge,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = buildPath({ it.tempCelsius }, { it.isTemperatureAvailable }, minTemp, maxTemp),
                    color = AmberThermal,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = buildPath({ it.currentMa.toFloat() }, { it.isCurrentAvailable }, minMa, maxMa),
                    color = EmeraldCharge,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                val focus = samples[selectedIndex]
                val focusX = selectedIndex * stepX
                drawLine(
                    color = focusLineColor,
                    start = Offset(focusX, 0f),
                    end = Offset(focusX, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                if (focus.isCurrentAvailable) {
                    drawCircle(
                        color = EmeraldCharge,
                        radius = 5.dp.toPx(),
                        center = Offset(focusX, pointY(focus.currentMa.toFloat(), minMa, maxMa))
                    )
                }
                if (focus.isTemperatureAvailable) {
                    drawCircle(
                        color = AmberThermal,
                        radius = 4.dp.toPx(),
                        center = Offset(focusX, pointY(focus.tempCelsius, minTemp, maxTemp))
                    )
                }
                if (focus.isLevelAvailable) {
                    drawCircle(
                        color = CyanFastCharge,
                        radius = 4.dp.toPx(),
                        center = Offset(focusX, pointY(focus.batteryLevel.toFloat(), 0f, 100f))
                    )
                }
            }

            selectedSample?.let { sample ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurveSampleCell(
                            label = "Waktu",
                            value = timeFormatter.format(Instant.ofEpochMilli(sample.timestamp)),
                            isAvailable = true,
                            accent = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.7f)
                        )
                        CurveSampleCell(
                            label = "Arus",
                            value = String.format(Locale.US, "%+d mA", sample.currentMa),
                            isAvailable = sample.isCurrentAvailable,
                            accent = EmeraldCharge,
                            modifier = Modifier.weight(1.05f)
                        )
                        CurveSampleCell(
                            label = "Suhu",
                            value = String.format(Locale.US, "%.1f°C", sample.tempCelsius),
                            isAvailable = sample.isTemperatureAvailable,
                            accent = AmberThermal,
                            modifier = Modifier.weight(0.9f)
                        )
                        CurveSampleCell(
                            label = "Baterai",
                            value = "${sample.batteryLevel}%",
                            isAvailable = sample.isLevelAvailable,
                            accent = CyanFastCharge,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
                Text(
                    text = "Sentuh/geser grafik untuk memilih titik",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = if (sample.isVoltageAvailable) {
                        String.format(Locale.US, "Tegangan: %.2f V (%d mV)", sample.voltageMv / 1000.0, sample.voltageMv)
                    } else "Tegangan: —",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            val peakMa = currentSamples.maxOfOrNull { it.currentMa }
            val minSampleMa = currentSamples.minOfOrNull { it.currentMa }
            val peakTemp = temperatureSamples.maxOfOrNull { it.tempCelsius }
            val currentSummary = if (peakMa != null && minSampleMa != null) {
                "${if (peakMa > 0) "+$peakMa" else "$peakMa"} / ${if (minSampleMa > 0) "+$minSampleMa" else "$minSampleMa"} mA"
            } else {
                "Arus: —"
            }
            val temperatureSummary = peakTemp?.let { "Maks ${String.format(Locale.US, "%.1f", it)}°C" } ?: "Suhu: —"
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSummary,
                    modifier = Modifier.weight(1.8f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = temperatureSummary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${samples.size} titik",
                    modifier = Modifier.weight(0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ChargingThermalSummaryPanel(thermalSummary)
        }
    }
}

@Composable
internal fun ChargingThermalSummaryPanel(summary: ChargingThermalSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Distribusi zona suhu", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Aman <38°C · Hangat 38–<40°C · Tinggi ≥40°C",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val totalMinutes = summary.safeMinutes + summary.warmMinutes + summary.highMinutes
            if (totalMinutes > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (summary.safeMinutes > 0) {
                        Box(Modifier.weight(summary.safeMinutes.toFloat()).fillMaxHeight().background(EmeraldCharge))
                    }
                    if (summary.warmMinutes > 0) {
                        Box(Modifier.weight(summary.warmMinutes.toFloat()).fillMaxHeight().background(AmberThermal))
                    }
                    if (summary.highMinutes > 0) {
                        Box(Modifier.weight(summary.highMinutes.toFloat()).fillMaxHeight().background(CrimsonAnomaly))
                    }
                }
            }
            Text(
                if (summary.hasTemperatureData) {
                    "${summary.safeMinutes}m Aman · ${summary.warmMinutes}m Hangat · ${summary.highMinutes}m Tinggi"
                } else "Data suhu sesi tidak tersedia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(summary.phase.label, style = MaterialTheme.typography.labelMedium,
                color = when (summary.phase) {
                    com.superdenot.powerguard.domain.analytics.ChargingPhaseIndicator.CC -> EmeraldCharge
                    com.superdenot.powerguard.domain.analytics.ChargingPhaseIndicator.CV -> AmberThermal
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold)
            Text("Jeda sampel >2 menit tidak dihitung. Pola sisi baterai ini bukan identifikasi protokol langsung.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CurveSampleCell(
    label: String,
    value: String,
    isAvailable: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val displayValue = formatCurveReading(isAvailable, value)
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label: ${if (isAvailable) value else "Tidak tersedia"}"
        }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            displayValue,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
