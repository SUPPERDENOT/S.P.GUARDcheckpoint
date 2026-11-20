package com.superdenot.powerguard.ui.analytics

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.ui.components.ChargingCurveChart
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CrimsonAnomaly
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatSessionLevelRange(startLevel: Int, endLevel: Int): String =
    "$startLevel% -> $endLevel%"


@Composable
fun AnalyticsScreen(
    viewModel: PowerGuardViewModel,
    contentPadding: PaddingValues,
    initialSubTab: Int = 0,
    visibleSubTabs: List<Int> = listOf(0, 1, 2)
) {
    val availableTabs = visibleSubTabs.filter { it in 0..2 }.ifEmpty { listOf(0) }
    var selectedSubTab by remember(availableTabs, initialSubTab) {
        mutableIntStateOf(if (initialSubTab in availableTabs) initialSubTab else availableTabs.first())
    }
    val subTabs = listOf("Sesi & Kurva", "Siklus Harian", "Aktivitas App")
    val bottomScrollPadding = PaddingValues(
        bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 12.dp)
    ) {
        // Segmented tab bar hanya ditampilkan jika halaman memiliki beberapa sub-tab.
        if (availableTabs.size > 1) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            availableTabs.forEach { index ->
                val title = subTabs[index]
                val isSelected = selectedSubTab == index
                Surface(
                    onClick = {
                        selectedSubTab = index
                        if (index == 2) viewModel.refreshPermissionsAndUsage()
                    },
                    color = if (isSelected) CyanFastCharge.copy(alpha = 0.22f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, CyanFastCharge.copy(alpha = 0.6f)) else null,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CyanFastCharge else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        when (selectedSubTab) {
            0 -> SessionsAndCurveTab(viewModel, bottomScrollPadding)
            1 -> CyclesAndRollupTab(viewModel, bottomScrollPadding)
            2 -> AppUsageTab(viewModel, bottomScrollPadding)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionsAndCurveTab(viewModel: PowerGuardViewModel, bottomScrollPadding: PaddingValues) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSessionId by viewModel.selectedSessionId.collectAsState()
    val samples by viewModel.selectedSessionSamples.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }
    var showCurve by remember { mutableStateOf(false) }

    LaunchedEffect(sessions) {
        if (selectedSessionId == null && sessions.isNotEmpty()) {
            val firstId = sessions.first().id
            viewModel.selectSessionForCurve(firstId)
            expandedSessionId = firstId
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Riwayat Sesi Pengisian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${sessions.size} sesi tersimpan • pilih sesi untuk detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.TextButton(onClick = { showCurve = !showCurve }) {
                Text(if (showCurve) "Tutup Kurva" else "Lihat Kurva")
            }
        }
        AnimatedVisibility(
            visible = showCurve,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ChargingCurveChart(samples = samples)
        }

        LazyColumn(
            contentPadding = bottomScrollPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        if (sessions.isEmpty()) {
            item {
                Text(
                    text = "Belum ada sesi pengisian tercatat. Colokkan charger untuk mulai merekam otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            stickyHeader {
                SessionTableHeader()
            }
            items(sessions, key = { it.id }) { session ->
                val isActive = session.endTime == null
                val endLevel = session.endLevel ?: if (isActive) liveState.level else session.startLevel
                val endTime = session.endTime ?: System.currentTimeMillis()
                val duration = formatDurationIndo(((endTime - session.startTime) / 1000L).coerceAtLeast(0L))
                val delta = endLevel - session.startLevel
                val isExpanded = expandedSessionId == session.id
                val isSelected = selectedSessionId == session.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) CyanFastCharge.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.selectSessionForCurve(session.id)
                            expandedSessionId = if (isExpanded) null else session.id
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateTime = remember(session.startTime) {
                            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(session.startTime))
                        }
                        Text(
                            text = dateTime + if (isActive) " • Aktif" else "",
                            modifier = Modifier.weight(0.36f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            color = if (isActive) CyanFastCharge else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatSessionLevelRange(session.startLevel, endLevel),
                            modifier = Modifier.weight(0.18f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f", session.totalMahDelivered),
                            modifier = Modifier.weight(0.20f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            color = EmeraldCharge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = duration,
                            modifier = Modifier.weight(0.26f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            maxLines = 2
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 9.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SessionDetailMiniBox(
                                label = "Arus Rata-rata",
                                value = "${session.avgCurrentMa} mA",
                                subValue = "Suhu maks. ${String.format(Locale.US, "%.1f", session.maxTempCelsius)}°C",
                                accent = EmeraldCharge,
                                modifier = Modifier.weight(1f)
                            )
                            SessionDetailMiniBox(
                                label = "Perubahan Baterai",
                                value = (if (delta >= 0) "+" else "") + "${delta}%",
                                subValue = "Daya masuk ${String.format(Locale.US, "%.1f", session.totalMahDelivered)} mAh",
                                accent = if (delta >= 0) EmeraldCharge else CrimsonAnomaly,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (session.isAnomaly) {
                            Text(
                                text = "ANOMALI: ${session.anomalyReason ?: "Perlu pemeriksaan"}",
                                color = CrimsonAnomaly,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            text = "Ketuk sesi lain untuk memuat kurva sesi tersebut",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SessionTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Waktu", modifier = Modifier.weight(0.36f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("Level", modifier = Modifier.weight(0.18f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("mAh", modifier = Modifier.weight(0.20f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        Text("Durasi", modifier = Modifier.weight(0.26f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun SessionHistoryCard(
    session: ChargingSessionEntity,
    effectiveEndLevel: Int,
    effectiveEndTime: Long,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())
    }
    val timeOnlyFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    val startStr = dateTimeFormatter.format(Instant.ofEpochMilli(session.startTime))
    val startClock = timeOnlyFormatter.format(Instant.ofEpochMilli(session.startTime))
    val endClock = if (session.endTime != null) {
        timeOnlyFormatter.format(Instant.ofEpochMilli(session.endTime))
    } else {
        "Sedang Berjalan"
    }

    val durationSec = ((effectiveEndTime - session.startTime) / 1000L).coerceAtLeast(0L)
    val durationFormatted = formatDurationIndo(durationSec)

    val deltaPct = effectiveEndLevel - session.startLevel
    val deltaPctStr = when {
        deltaPct > 0 -> "+$deltaPct%"
        deltaPct < 0 -> "$deltaPct%"
        else -> "0%"
    }
    val endPctLabel = formatSessionLevelRange(session.startLevel, effectiveEndLevel) +
        if (session.endLevel == null) " (Aktif)" else ""

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanFastCharge.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(1.dp, CyanFastCharge.copy(alpha = 0.45f)) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$startStr • ${session.chargerType}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (session.isAnomaly) {
                        Surface(color = CrimsonAnomaly.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                            Text(
                                text = "ANOMALI",
                                color = CrimsonAnomaly,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup Detail" else "Lihat Detail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Level: $endPctLabel ($deltaPctStr)  |  Terisi: ${String.format(Locale.US, "%+.1f mAh", session.totalMahDelivered)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (session.totalMahDelivered >= 0) EmeraldCharge else CrimsonAnomaly
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Durasi: $durationFormatted • Arus Rata-rata: ${session.avgCurrentMa} mA • Suhu Maks: ${String.format(Locale.US, "%.1f", session.maxTempCelsius)}°C",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Panel Detail Interaktif saat Kartu Sesi Diklik
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SessionDetailMiniBox(
                            label = "Durasi Pengisian",
                            value = durationFormatted,
                            subValue = "$startClock - $endClock",
                            accent = CyanFastCharge,
                            modifier = Modifier.weight(1f)
                        )
                        SessionDetailMiniBox(
                            label = "Persentase Terisi",
                            value = "$deltaPctStr (${formatSessionLevelRange(session.startLevel, effectiveEndLevel)})",
                            subValue = String.format(Locale.US, "Kapasitas: %+.1f mAh", session.totalMahDelivered),
                            accent = if (deltaPct > 0 || session.totalMahDelivered > 0) EmeraldCharge else CrimsonAnomaly,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val estAvgWatt = (session.avgCurrentMa / 1000.0) * 4.1
                        SessionDetailMiniBox(
                            label = "Performa Arus & Daya",
                            value = "${session.avgCurrentMa} mA (~${String.format(Locale.US, "%.1f", estAvgWatt)} W)",
                            subValue = "Sumber: ${session.chargerType}",
                            accent = EmeraldCharge,
                            modifier = Modifier.weight(1f)
                        )
                        SessionDetailMiniBox(
                            label = "Suhu & Deep Sleep",
                            value = "${String.format(Locale.US, "%.1f", session.maxTempCelsius)}°C Maks",
                            subValue = "Deep Sleep CPU: ${(session.deepSleepRatio * 100).toInt()}%",
                            accent = if (session.maxTempCelsius >= 40f) CrimsonAnomaly else AmberThermal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            session.anomalyReason?.let { reason ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Diagnosa: $reason",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAnomaly
                )
            }
        }
    }
}

@Composable
private fun SessionDetailMiniBox(
    label: String,
    value: String,
    subValue: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDurationIndo(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format(Locale.US, "%d jam %02d mnt", hours, minutes)
        minutes > 0 -> String.format(Locale.US, "%02d mnt %02d dtk", minutes, seconds)
        else -> String.format(Locale.US, "%02d detik", seconds)
    }
}

@Composable
private fun CyclesAndRollupTab(viewModel: PowerGuardViewModel, bottomScrollPadding: PaddingValues) {
    val rollups by viewModel.dailyRollups.collectAsState()

    LazyColumn(
        contentPadding = bottomScrollPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laporan Siklus Harian",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                )
                OutlinedButton(
                    onClick = { viewModel.triggerManualRollup() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Hitung Sekarang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (rollups.isEmpty()) {
            item {
                Text(
                    text = "Belum ada ringkasan harian. Tekan 'Hitung Sekarang' setelah melakukan sesi pengisian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(rollups, key = { it.dateKey }) { rollup ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Tanggal: ${rollup.dateKey}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Akumulasi Siklus: ${String.format(Locale.US, "%.2f", rollup.totalCycles)} cycle • Total Masuk: +${String.format(Locale.US, "%.0f", rollup.totalMahCharged)} mAh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldCharge
                        )
                        Text(
                            text = "Suhu Maks: ${String.format(Locale.US, "%.1f", rollup.maxTempCelsius)}°C • Jumlah Anomali: ${rollup.anomalyCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageTab(viewModel: PowerGuardViewModel, bottomScrollPadding: PaddingValues) {
    val context = LocalContext.current
    val hasPerm by viewModel.hasUsagePermission.collectAsState()
    val apps by viewModel.appUsageList.collectAsState()

    LazyColumn(
        contentPadding = bottomScrollPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Aktivitas Aplikasi Latar Belakang & Layar (24 Jam)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasPerm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Izin Usage Access diperlukan agar PowerGuard dapat mendeteksi aplikasi aktif saat terjadi anomali pengisian daya.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("Buka Pengaturan Izin", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else if (apps.isEmpty()) {
            item {
                Text(
                    text = "Tidak ada aktivitas aplikasi signifikan dalam 24 jam terakhir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(apps, key = { it.packageName }) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appDisplayName = remember(app.packageName) {
                            runCatching {
                                val applicationInfo = context.packageManager.getApplicationInfo(app.packageName, 0)
                                context.packageManager.getApplicationLabel(applicationInfo).toString()
                                    .takeIf { it.isNotBlank() } ?: app.packageName
                            }.getOrDefault(app.packageName)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(appDisplayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${app.foregroundTimeMinutes} mnt",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanFastCharge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
