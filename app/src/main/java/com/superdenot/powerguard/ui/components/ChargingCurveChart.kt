package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChargingCurveChart(
    samples: List<BatterySampleEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Charging Curve Analyzer (Arus • Suhu • Level)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = EmeraldCharge, label = "Arus (mA)")
            LegendItem(color = AmberThermal, label = "Suhu (°C)")
            LegendItem(color = CyanFastCharge, label = "Level (%)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (samples.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Menunggu minimal 2 sampel menit pengisian untuk membentuk grafik...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val minMa = min(0f, samples.minOf { it.currentMa.toFloat() })
            val maxMa = max(500f, samples.maxOf { it.currentMa.toFloat() })
            val minTemp = 20f
            val maxTemp = max(50f, samples.maxOf { it.tempCelsius })

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (samples.size - 1).coerceAtLeast(1)

                for (i in 0..4) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun buildPath(valueSelector: (BatterySampleEntity) -> Float, minVal: Float, maxVal: Float): Path {
                    val path = Path()
                    val range = (maxVal - minVal).coerceAtLeast(1f)
                    samples.forEachIndexed { idx, sample ->
                        val x = idx * stepX
                        val normalized = ((valueSelector(sample) - minVal) / range).coerceIn(0f, 1f)
                        val y = height - (normalized * height)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    return path
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            val peakMa = samples.maxOf { it.currentMa }
            val minSampleMa = samples.minOf { it.currentMa }
            val peakTemp = samples.maxOf { it.tempCelsius }
            val peakMaFormatted = if (peakMa > 0) "+$peakMa" else "$peakMa"
            val minMaFormatted = if (minSampleMa > 0) "+$minSampleMa" else "$minSampleMa"
            val peakTempFormatted = String.format(Locale.US, "%.1f", peakTemp)

            Text(
                text = "Puncak Arus: $peakMaFormatted mA • Terendah: $minMaFormatted mA • Suhu Maks: $peakTempFormatted°C (${samples.size} menit)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
