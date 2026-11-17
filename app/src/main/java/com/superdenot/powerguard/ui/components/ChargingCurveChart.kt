package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
            .padding(10.dp)
    ) {
        Text(
            text = "Charging Curve Analyzer (Arus • Suhu • Level)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(5.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = EmeraldCharge, label = "Arus (mA)")
            LegendItem(color = AmberThermal, label = "Suhu (°C)")
            LegendItem(color = CyanFastCharge, label = "Level (%)")
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (samples.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Menunggu minimal 2 sampel untuk membentuk grafik...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val minMa = min(0f, samples.minOf { it.currentMa.toFloat() })
            val maxMa = max(500f, samples.maxOf { it.currentMa.toFloat() })
            val minTemp = min(20f, samples.minOf { it.tempCelsius })
            val maxTemp = max(50f, samples.maxOf { it.tempCelsius })

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
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
                    minValue: Float,
                    maxValue: Float
                ): Path {
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val point = Offset(
                            x = index * stepX,
                            y = pointY(valueSelector(sample), minValue, maxValue)
                        )
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
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
                    path = buildPath({ it.batteryLevel.toFloat() }, 0f, 100f),
                    color = CyanFastCharge,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = buildPath({ it.tempCelsius }, minTemp, maxTemp),
                    color = AmberThermal,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = buildPath({ it.currentMa.toFloat() }, minMa, maxMa),
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
                drawCircle(
                    color = EmeraldCharge,
                    radius = 5.dp.toPx(),
                    center = Offset(focusX, pointY(focus.currentMa.toFloat(), minMa, maxMa))
                )
                drawCircle(
                    color = AmberThermal,
                    radius = 4.dp.toPx(),
                    center = Offset(focusX, pointY(focus.tempCelsius, minTemp, maxTemp))
                )
                drawCircle(
                    color = CyanFastCharge,
                    radius = 4.dp.toPx(),
                    center = Offset(focusX, pointY(focus.batteryLevel.toFloat(), 0f, 100f))
                )
            }

            selectedSample?.let { sample ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = timeFormatter.format(Instant.ofEpochMilli(sample.timestamp)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TooltipValue("Arus", String.format(Locale.US, "%+d mA", sample.currentMa), EmeraldCharge)
                            TooltipValue("Suhu", String.format(Locale.US, "%.1f°C", sample.tempCelsius), AmberThermal)
                            TooltipValue("Baterai", "${sample.batteryLevel}%", CyanFastCharge)
                        }
                    }
                }
                Text(
                    text = "Geser atau sentuh grafik untuk membaca titik data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val peakMa = samples.maxOf { it.currentMa }
            val minSampleMa = samples.minOf { it.currentMa }
            val peakTemp = samples.maxOf { it.tempCelsius }
            Text(
                text = "Puncak Arus: ${if (peakMa > 0) "+$peakMa" else "$peakMa"} mA • Terendah: ${if (minSampleMa > 0) "+$minSampleMa" else "$minSampleMa"} mA • Suhu Maks: ${String.format(Locale.US, "%.1f", peakTemp)}°C (${samples.size} titik)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TooltipValue(label: String, value: String, accent: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = accent)
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
