package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.superdenot.powerguard.domain.analytics.ChargingThermalPhaseAnalyzer
import com.superdenot.powerguard.ui.theme.BasicAccent
import com.superdenot.powerguard.ui.theme.BasicAppearanceMode
import com.superdenot.powerguard.ui.theme.AmoledPureAccent
import com.superdenot.powerguard.ui.theme.EvCockpitAccent
import com.superdenot.powerguard.ui.theme.NordicFrostAccent
import com.superdenot.powerguard.ui.theme.MidnightAccent
import com.superdenot.powerguard.ui.theme.ThemeGaugeSummaryFormatter
import com.superdenot.powerguard.ui.theme.ThemePackConfig
import com.superdenot.powerguard.ui.theme.PowerGuardTheme
import com.superdenot.powerguard.ui.theme.hasThemeGaugeUnsavedChanges
import com.superdenot.powerguard.ui.theme.themePreviewConfig
import com.superdenot.powerguard.ui.components.PowerActionButton
import com.superdenot.powerguard.ui.components.PowerSaveButton
import com.superdenot.powerguard.ui.theme.basicAccentSwatches
import com.superdenot.powerguard.ui.theme.amoledAccentSwatches
import com.superdenot.powerguard.ui.theme.evCockpitAccentSwatches
import com.superdenot.powerguard.ui.theme.nordicFrostAccentSwatches
import com.superdenot.powerguard.ui.theme.monochromeAccentSwatches
import com.superdenot.powerguard.ui.theme.midnightAccentSwatches
import com.superdenot.powerguard.ui.components.pressScale
import com.superdenot.powerguard.ui.components.pressScaleClickable

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
    var showConfiguration by remember { mutableStateOf(false) }
    var draft by remember(config) { mutableStateOf(config) }
    val (themeSummary, gaugeSummary) = ThemeGaugeSummaryFormatter.format(config)

    Card(
        modifier = modifier.fillMaxWidth().pressScaleClickable {
            draft = config
            showConfiguration = true
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

    if (showConfiguration) {
        val hasUnsavedChanges = hasThemeGaugeUnsavedChanges(draft, config)
        Dialog(onDismissRequest = { showConfiguration = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).imePadding(),
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
                        PowerGuardTheme(config = themePreviewConfig(draft)) {
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
                                        modifier = Modifier.size(82.dp),
                                        forceAnimate = true
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            when {
                                                isTemperatureAvailable && temperatureCelsius >= ChargingThermalPhaseAnalyzer.HIGH_TEMPERATURE_CELSIUS -> "Kritis termal"
                                                isTemperatureAvailable && temperatureCelsius >= ChargingThermalPhaseAnalyzer.WARM_TEMPERATURE_CELSIUS -> "Hangat"
                                                isCharging -> "Mengisi"
                                                else -> "Normal"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Model: ${draft.gaugeModel.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
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
                        Text("Paket tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "Basic",
                                    swatches = basicAccentSwatches(), selected = draft is ThemePackConfig.Basic,
                                    onClick = { draft = draft.toBasic() }
                                )
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "Midnight",
                                    swatches = midnightAccentSwatches(), selected = draft is ThemePackConfig.MidnightViolet,
                                    onClick = { draft = draft.toMidnight() }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "AMOLED Pure",
                                    swatches = amoledAccentSwatches(), selected = draft is ThemePackConfig.AmoledPure,
                                    onClick = { draft = draft.toAmoledPure() }
                                )
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "EV Cockpit",
                                    swatches = evCockpitAccentSwatches(), selected = draft is ThemePackConfig.EvCockpit,
                                    onClick = { draft = draft.toEvCockpit() }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "Nordic Frost",
                                    swatches = nordicFrostAccentSwatches(), selected = draft is ThemePackConfig.NordicFrost,
                                    onClick = { draft = draft.toNordicFrost() }
                                )
                                ThemePackageChoice(
                                    modifier = Modifier.weight(1f), title = "Monochrome",
                                    swatches = monochromeAccentSwatches(), selected = draft is ThemePackConfig.Monochrome,
                                    onClick = { draft = draft.toMonochrome() }
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                Text("Warna tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(MidnightAccent.values().toList(), current.midnightAccent, { it.label }) { selected ->
                                    draft = current.copy(midnightAccent = selected)
                                }
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
                            is ThemePackConfig.AmoledPure -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Aksen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(AmoledPureAccent.values().toList(), current.accent, { it.label }) { selected ->
                                    draft = current.copy(accent = selected)
                                }
                            }
                            is ThemePackConfig.EvCockpit -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Aksen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(EvCockpitAccent.values().toList(), current.accent, { it.label }) { selected ->
                                    draft = current.copy(accent = selected)
                                }
                            }
                            is ThemePackConfig.NordicFrost -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Aksen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                ChoiceRow(NordicFrostAccent.values().toList(), current.accent, { it.label }) { selected ->
                                    draft = current.copy(accent = selected)
                                }
                            }
                            is ThemePackConfig.Monochrome -> {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Palet tunggal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Grayscale tanpa aksen dekoratif; mengikuti mode sistem.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PowerActionButton(onClick = { showConfiguration = false }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Batal") }
                        PowerSaveButton(
                            clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS,
                            onClick = {
                                onSave(draft)
                                showConfiguration = false
                            },
                            enabled = hasUnsavedChanges
                        ) { Text("Simpan") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePackageChoice(
    modifier: Modifier,
    title: String,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 68.dp).pressScale(interactionSource)
            .selectable(selected = selected, role = Role.RadioButton, interactionSource = interactionSource,
                indication = LocalIndication.current, onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                swatches.forEach { swatch ->
                    Box(Modifier.size(12.dp).clip(CircleShape).background(swatch))
                }
                if (selected) {
                    Spacer(Modifier.weight(1f))
                    Text("Aktif", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
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
            val interactionSource = remember(value) { MutableInteractionSource() }
            Surface(
                modifier = Modifier.weight(1f).height(36.dp).pressScale(interactionSource)
                    .selectable(selected = value == selected, role = Role.RadioButton,
                        interactionSource = interactionSource, indication = LocalIndication.current, onClick = { onSelected(value) }),
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
    is ThemePackConfig.AmoledPure -> copy(gaugeModel = model)
    is ThemePackConfig.EvCockpit -> copy(gaugeModel = model)
    is ThemePackConfig.NordicFrost -> copy(gaugeModel = model)
    is ThemePackConfig.Monochrome -> copy(gaugeModel = model)
}

private fun ThemePackConfig.withAnimation(animation: ChargingArcAnimation): ThemePackConfig = when (this) {
    is ThemePackConfig.Basic -> copy(chargingAnimation = animation)
    is ThemePackConfig.MidnightViolet -> copy(chargingAnimation = animation)
    is ThemePackConfig.AmoledPure -> copy(chargingAnimation = animation)
    is ThemePackConfig.EvCockpit -> copy(chargingAnimation = animation)
    is ThemePackConfig.NordicFrost -> copy(chargingAnimation = animation)
    is ThemePackConfig.Monochrome -> copy(chargingAnimation = animation)
}

private fun ThemePackConfig.toBasic(): ThemePackConfig.Basic = when (this) {
    is ThemePackConfig.Basic -> this
    else -> ThemePackConfig.Basic(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
private fun ThemePackConfig.toMidnight(): ThemePackConfig.MidnightViolet = when (this) {
    is ThemePackConfig.MidnightViolet -> this
    else -> ThemePackConfig.MidnightViolet(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
private fun ThemePackConfig.toAmoledPure(): ThemePackConfig.AmoledPure = when (this) {
    is ThemePackConfig.AmoledPure -> this
    else -> ThemePackConfig.AmoledPure(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
private fun ThemePackConfig.toEvCockpit(): ThemePackConfig.EvCockpit = when (this) {
    is ThemePackConfig.EvCockpit -> this
    else -> ThemePackConfig.EvCockpit(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
private fun ThemePackConfig.toNordicFrost(): ThemePackConfig.NordicFrost = when (this) {
    is ThemePackConfig.NordicFrost -> this
    else -> ThemePackConfig.NordicFrost(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
private fun ThemePackConfig.toMonochrome(): ThemePackConfig.Monochrome = when (this) {
    is ThemePackConfig.Monochrome -> this
    else -> ThemePackConfig.Monochrome(gaugeModel = gaugeModel, chargingAnimation = chargingAnimation)
}
