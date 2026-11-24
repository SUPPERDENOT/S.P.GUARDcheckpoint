package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.BasicAppearanceMode
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.ui.theme.MidnightVioletAccent
import com.superdenot.powerguard.ui.theme.ThemeGaugeSummaryFormatter
import com.superdenot.powerguard.ui.theme.ThemePackConfig
import com.superdenot.powerguard.ui.theme.PowerGuardTheme
import com.superdenot.powerguard.ui.theme.basicAccentSwatches

@Composable
fun ThemePickerDialogs(
    config: ThemePackConfig,
    batteryLevel: Int?,
    isCharging: Boolean,
    temperatureCelsius: Float,
    isTemperatureAvailable: Boolean,
    onSave: (ThemePackConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPackagePicker by remember { mutableStateOf(false) }
    var showConfiguration by remember { mutableStateOf(false) }
    var draft by remember(config) { mutableStateOf(config) }
    val (themeSummary, gaugeSummary) = ThemeGaugeSummaryFormatter.format(config)

    Card(
        modifier = modifier.fillMaxWidth().clickable {
            draft = config
            showPackagePicker = true
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Tema & Gauge", fontWeight = FontWeight.Bold)
                Text(themeSummary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(gaugeSummary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            androidx.compose.material3.Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Konfigurasi tema dan gauge",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPackagePicker) {
        Dialog(onDismissRequest = { showPackagePicker = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih Paket Tema", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    ThemePackageChoice(
                        title = "Basic",
                        description = "Mode sistem, terang, atau gelap dengan tiga aksen.",
                        swatches = basicAccentSwatches(),
                        selected = draft is ThemePackConfig.Basic,
                        onClick = {
                            draft = when (val current = draft) {
                                is ThemePackConfig.Basic -> current
                                is ThemePackConfig.MidnightViolet -> ThemePackConfig.Basic(
                                    gaugeModel = current.gaugeModel,
                                    chargingAnimation = current.chargingAnimation
                                )
                            }
                            showPackagePicker = false
                            showConfiguration = true
                        }
                    )
                    ThemePackageChoice(
                        title = "Midnight Violet",
                        description = "Gelap deep-indigo dengan opsi sub-kartu metrik dua warna.",
                        swatches = listOf(MidnightVioletAccent),
                        selected = draft is ThemePackConfig.MidnightViolet,
                        onClick = {
                            draft = when (val current = draft) {
                                is ThemePackConfig.MidnightViolet -> current
                                is ThemePackConfig.Basic -> ThemePackConfig.MidnightViolet(
                                    gaugeModel = current.gaugeModel,
                                    chargingAnimation = current.chargingAnimation
                                )
                            }
                            showPackagePicker = false
                            showConfiguration = true
                        }
                    )
                    OutlinedButton(
                        onClick = { showPackagePicker = false },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = CircleShape
                    ) { Text("Batal") }
                }
            }
        }
    }

    if (showConfiguration) {
        val hasUnsavedChanges = draft != config
        Dialog(onDismissRequest = { showConfiguration = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
                    Text(
                        "Konfigurasi Tema & Gauge",
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Pratinjau langsung", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PowerGuardTheme(config = draft) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().height(104.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    BatteryGaugeView(
                                        progress = (batteryLevel ?: 0).coerceIn(0, 100) / 100f,
                                        percentageLabel = batteryLevel?.let { "$it%" } ?: "—",
                                        model = draft.gaugeModel,
                                        animation = draft.chargingAnimation,
                                        isCharging = isCharging,
                                        isCriticalThermal = isTemperatureAvailable && temperatureCelsius >= 40f,
                                        isWarm = isTemperatureAvailable && temperatureCelsius >= 38f && temperatureCelsius < 40f,
                                        isVisibleOnScreen = true,
                                        modifier = Modifier.size(82.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            when {
                                                isTemperatureAvailable && temperatureCelsius >= 40f -> "Kritis termal"
                                                isTemperatureAvailable && temperatureCelsius >= 38f -> "Hangat"
                                                isCharging -> "Mengisi"
                                                else -> "Normal"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Model: ${draft.gaugeModel.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Model gauge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        ChoiceRow(BatteryGaugeModel.values().toList(), draft.gaugeModel, { it.label }) { selected ->
                            draft = draft.withGaugeModel(selected)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Animasi pengisian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        ChoiceRow(ChargingArcAnimation.values().toList(), draft.chargingAnimation, { it.label }) { selected ->
                            draft = draft.withAnimation(selected)
                        }
                        when (val current = draft) {
                            is ThemePackConfig.Basic -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Mode tampilan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(BasicAppearanceMode.values().toList(), current.appearanceMode, { it.label }) { selected ->
                                    draft = current.copy(appearanceMode = selected)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Aksen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(BasicAccent.values().toList(), current.accent, { it.label }) { selected ->
                                    draft = current.copy(accent = selected)
                                }
                            }
                            is ThemePackConfig.MidnightViolet -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Sub-kartu Dua Warna", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text("Warna dingin untuk metrik listrik dan hangat untuk suhu.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = current.dualTintMetrics,
                                        onCheckedChange = { draft = current.copy(dualTintMetrics = it) }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showConfiguration = false },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = CircleShape
                        ) { Text("Batal") }
                        Button(
                            onClick = {
                                onSave(draft)
                                showConfiguration = false
                            },
                            enabled = hasUnsavedChanges,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = CircleShape
                        ) { Text("Simpan") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePackageChoice(
    title: String,
    description: String,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                swatches.forEach { swatch ->
                    Box(Modifier.size(13.dp).clip(CircleShape).background(swatch))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Text("AKTIF", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        values.forEach { value ->
            Surface(
                modifier = Modifier.weight(1f).height(32.dp).clickable { onSelected(value) },
                color = if (value == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (value == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Text(label(value), fontSize = 11.sp, maxLines = 1,
                        color = if (value == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun ThemePackConfig.withGaugeModel(model: BatteryGaugeModel): ThemePackConfig = when (this) {
    is ThemePackConfig.Basic -> copy(gaugeModel = model)
    is ThemePackConfig.MidnightViolet -> copy(gaugeModel = model)
}

private fun ThemePackConfig.withAnimation(animation: ChargingArcAnimation): ThemePackConfig = when (this) {
    is ThemePackConfig.Basic -> copy(chargingAnimation = animation)
    is ThemePackConfig.MidnightViolet -> copy(chargingAnimation = animation)
}
