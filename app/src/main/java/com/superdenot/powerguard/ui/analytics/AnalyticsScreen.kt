package com.superdenot.powerguard.ui.analytics

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.superdenot.powerguard.ui.components.PowerActionButton
import com.superdenot.powerguard.ui.components.PowerSaveButton
import com.superdenot.powerguard.ui.components.POWER_BUTTON_CLOSE_DELAY_MS
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.domain.analytics.HistorySessionFilter
import com.superdenot.powerguard.domain.analytics.HistorySessionFilterMatcher
import com.superdenot.powerguard.domain.analytics.SessionAnomaly
import com.superdenot.powerguard.domain.analytics.SessionAnomalyEvaluator
import com.superdenot.powerguard.domain.analytics.SessionPresentationMapper
import com.superdenot.powerguard.domain.benchmark.AccessoryBenchmarkResult
import com.superdenot.powerguard.domain.benchmark.AccessoryBenchmarkRanker
import com.superdenot.powerguard.domain.analytics.DailyCycleTrend
import com.superdenot.powerguard.domain.analytics.DailyCycleTrendPoint
import com.superdenot.powerguard.ui.components.ChargingCurveChart
import com.superdenot.powerguard.ui.components.InfoLongPressDialog
import com.superdenot.powerguard.ui.viewmodel.PopupDetectorViewModel
import com.superdenot.powerguard.ui.components.infoLongPress
import com.superdenot.powerguard.ui.theme.AmberThermal
import com.superdenot.powerguard.ui.theme.CrimsonAnomaly
import com.superdenot.powerguard.ui.theme.CyanFastCharge
import com.superdenot.powerguard.ui.theme.EmeraldCharge
import com.superdenot.powerguard.service.NotificationVisualRenderer
import com.superdenot.powerguard.ui.viewmodel.PowerGuardViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.NumberFormat
import kotlin.math.roundToLong
import java.time.LocalDate

internal fun formatSessionLevelRange(startLevel: Int, endLevel: Int?): String =
    if (endLevel == null) "$startLevel% \u2192 —" else "$startLevel% \u2192 $endLevel%"

internal fun formatCurveSessionCount(visibleCount: Int, totalCount: Int): String =
    "Sesi $visibleCount/$totalCount"

internal fun isDailyRollupSelected(dateKey: String, selectedDate: LocalDate?): Boolean =
    selectedDate?.toString() == dateKey


@Composable
internal fun AnalyticsScreen(
    viewModel: PowerGuardViewModel,
    contentPadding: PaddingValues,
    initialSubTab: Int = 0,
    visibleSubTabs: List<Int> = listOf(0, 1, 2),
    popupDetectorOpenRequest: Int = 0,
    popupDetectorViewModel: PopupDetectorViewModel
) {
    val availableTabs = visibleSubTabs.filter { it in 0..2 }.ifEmpty { listOf(0) }
    var selectedSubTab by remember(availableTabs, initialSubTab) {
        mutableIntStateOf(if (initialSubTab in availableTabs) initialSubTab else availableTabs.first())
    }
    LaunchedEffect(popupDetectorOpenRequest) {
        if (popupDetectorOpenRequest > 0 && 2 in availableTabs) selectedSubTab = 2
    }
    val subTabs = listOf("Sesi & Kurva", "Siklus Harian", "Aktivitas Aplikasi")
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
            2 -> AppUsageTab(viewModel, bottomScrollPadding, popupDetectorOpenRequest, popupDetectorViewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionsAndCurveTab(viewModel: PowerGuardViewModel, bottomScrollPadding: PaddingValues) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSessionId by viewModel.selectedSessionId.collectAsState()
    val samples by viewModel.selectedSessionSamples.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    val accessoryBenchmark by viewModel.accessoryBenchmark.collectAsState()
    var editingAccessorySession by remember { mutableStateOf<ChargingSessionEntity?>(null) }
    var accessoryDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showResetAllAccessoryLabelsConfirmation by remember { mutableStateOf(false) }
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }
    var isCurveExpanded by remember { mutableStateOf(false) }
    var longPressInfoTitle by remember { mutableStateOf<String?>(null) }
    var longPressInfoBody by remember { mutableStateOf("") }
    fun showLongPressInfo(title: String, body: String) {
        longPressInfoTitle = title
        longPressInfoBody = body
    }
    var selectedHistoryFilter by rememberSaveable { mutableStateOf(HistorySessionFilter.MICRO) }
    val nowEpochMs = remember(sessions, liveState.level, selectedHistoryFilter) {
        System.currentTimeMillis()
    }
    val visibleSessions = remember(sessions, liveState.level, selectedHistoryFilter, nowEpochMs) {
        HistorySessionFilterMatcher.filter(sessions, selectedHistoryFilter, liveState.level, nowEpochMs)
    }

    LaunchedEffect(visibleSessions, selectedSessionId) {
        if (selectedSessionId !in visibleSessions.map { it.id }) {
            val firstId = visibleSessions.firstOrNull()?.id
            viewModel.selectSessionForCurve(firstId)
            expandedSessionId = firstId
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isCurveExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatCurveSessionCount(visibleSessions.size, sessions.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                PowerActionButton(
                    onClick = { isCurveExpanded = false },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Tutup")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Riwayat Sesi Pengisian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${visibleSessions.size} dari ${sessions.size} sesi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                PowerActionButton(
                    onClick = { isCurveExpanded = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("Kurva")
                }
            }
        }
        AnimatedVisibility(
            visible = isCurveExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ChargingCurveChart(
                samples = samples,
                modifier = Modifier.infoLongPress(onLongPress = {
                    showLongPressInfo(
                        "Kurva sesi",
                        "Kurva menampilkan sampel arus, suhu, dan level baterai dari sesi yang dipilih. Sentuh atau geser grafik untuk memeriksa satu titik; nilai yang tidak tersedia tidak diinterpolasi."
                    )
                })
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(HistorySessionFilter.values().toList(), key = { it.name }) { filter ->
                FilterChip(selected = selectedHistoryFilter == filter,
                    onClick = { selectedHistoryFilter = filter },
                    label = { Text(filter.label, maxLines = 1, softWrap = false) })
            }
        }

        LazyColumn(
            contentPadding = bottomScrollPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        if (!isCurveExpanded && accessoryBenchmark.groups.size >= 2) {
            item {
                AccessoryBenchmarkCard(
                    result = accessoryBenchmark,
                    onDeleteGroup = { accessoryDeleteConfirmation = it },
                    onResetAll = { showResetAllAccessoryLabelsConfirmation = true },
                    onLongPressInfo = {
                        val notes = accessoryBenchmark.comparabilityNotes.joinToString("\n")
                        showLongPressInfo(
                            "Perbandingan aksesori",
                            "Laju pengisian dinormalisasi sebagai kenaikan level per jam. Suhu puncak, daya, dan arus adalah ringkasan rata-rata sesi berlabel. Perbandingan dipengaruhi level awal, suhu awal, dan kelengkapan sampel." +
                                if (notes.isNotBlank()) "\n\nCatatan kesebandingan:\n" + notes else ""
                        )
                    }
                )
            }
        }
        if (sessions.isEmpty() || visibleSessions.isEmpty()) {
            item {
                Text(
                    text = if (sessions.isEmpty()) "Belum ada sesi pengisian tercatat. Colokkan charger untuk mulai merekam otomatis."
                    else "Tidak ada sesi yang memenuhi filter saat ini. Matikan filter untuk menampilkan semua sesi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(visibleSessions, key = { _, item -> item.id }) { index, session ->
                val presentation = SessionPresentationMapper.map(session, liveState.level, nowEpochMs)
                val endLevel = presentation.endLevel
                val duration = formatDurationIndo(presentation.durationSeconds)
                val delta = presentation.deltaPercent
                val isExpanded = expandedSessionId == session.id
                val isSelected = selectedSessionId == session.id
                val isActive = session.endTime == null
                val currentDate = SessionPresentationMapper.localDate(session.startTime)
                val previousDate = visibleSessions.getOrNull(index - 1)?.let { SessionPresentationMapper.localDate(it.startTime) }
                if (index == 0 || currentDate != previousDate) {
                    Text(SessionPresentationMapper.dateHeaderLabel(session.startTime, nowEpochMs),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold)
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .drawBehind {
                            if (presentation.anomalies.isNotEmpty()) drawLine(
                                color = CrimsonAnomaly, start = Offset(0f, 0f),
                                end = Offset(0f, size.height), strokeWidth = 3.dp.toPx())
                        }
                        .infoLongPress(
                            onLongPress = {
                                showLongPressInfo(
                                    "Sesi pengisian",
                                    "Sesi dimulai pada " + DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                                        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(session.startTime)) +
                                        ". Total muatan tercatat " + NumberFormat.getIntegerInstance(Locale("id", "ID")).format(session.totalMahDelivered) +
                                        " mAh. Sampel dan metadata yang tidak diekspos perangkat tidak ditambahkan sebagai nilai perkiraan."
                                )
                            },
                            onClick = {
                                viewModel.selectSessionForCurve(session.id)
                                expandedSessionId = if (isExpanded) null else session.id
                            }
                        )
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeLabel = remember(session.startTime) {
                            DateTimeFormatter.ofPattern("HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(session.startTime))
                        }
                        Text(
                            text = timeLabel,
                            modifier = Modifier.weight(0.25f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            color = if (isActive) CyanFastCharge else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatSessionLevelRange(session.startLevel, endLevel),
                            modifier = Modifier.weight(0.29f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(session.totalMahDelivered) + " mAh",
                            modifier = Modifier.weight(0.22f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            color = EmeraldCharge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text("Aktif", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        } else {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih sesi untuk melihat kurva",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = duration,
                            modifier = Modifier.weight(0.18f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val metadata = listOfNotNull(
                            presentation.sourceLabel.takeIf { it.isNotBlank() },
                            presentation.powerLabel.takeUnless { it == "— W" || it.isBlank() },
                            presentation.temperatureLabel.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (metadata.isNotBlank()) {
                            Text(metadata, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        session.accessoryLabel?.takeIf { it.isNotBlank() }?.let { label ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(50.dp)) {
                                Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                        presentation.anomalies.forEach { anomaly ->
                            val badgeColor = if (anomaly == SessionAnomaly.HIGH_TEMPERATURE) AmberThermal else CrimsonAnomaly
                            Surface(color = badgeColor.copy(alpha = 0.12f), contentColor = badgeColor,
                                shape = RoundedCornerShape(50.dp),
                                border = BorderStroke(0.75.dp, badgeColor.copy(alpha = 0.45f))) {
                                Text(anomaly.label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }

                    if (isExpanded) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 9.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SessionDetailMiniBox(
                                label = "Arus Rata-rata",
                                value = if (session.hasCurrentData) "${session.avgCurrentMa} mA" else "—",
                                subValue = if (session.hasCurrentData) "Rata-rata arus baterai" else "Data arus tidak tersedia",
                                accent = EmeraldCharge,
                                modifier = Modifier.weight(1f).infoLongPress(onLongPress = {
                                    showLongPressInfo("Arus rata-rata", "Rata-rata arus dihitung dari sampel arus valid pada sesi ini.")
                                })
                            )
                            SessionDetailMiniBox(
                                label = "Perubahan Baterai",
                                value = delta?.let { deltaValue -> (if (deltaValue >= 0) "+$deltaValue%" else "$deltaValue%") } ?: "—",
                                subValue = "Level awal ${session.startLevel}%",
                                accent = when {
                                    delta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    delta >= 0 -> EmeraldCharge
                                    else -> CrimsonAnomaly
                                },
                                modifier = Modifier.weight(1f).infoLongPress(onLongPress = {
                                    showLongPressInfo("Perubahan baterai", "Perubahan level membandingkan level awal dan akhir sesi yang tercatat.")
                                })
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Label aksesori", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    session.accessoryLabel?.takeIf { it.isNotBlank() } ?: "Belum ditandai",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            PowerActionButton(onClick = { editingAccessorySession = session }) {
                                Text(if (session.accessoryLabel.isNullOrBlank()) "Tambah" else "Ubah")
                            }
                        }
                        if (session.isAnomaly) {
                            Text(
                                text = NotificationVisualRenderer.formatAnomalyReason(
                                    session.anomalyReason.orEmpty().split(',')
                                        .filterNot { it.trim() == SessionAnomalyEvaluator.RECOVERY_MARKER }
                                        .joinToString(", ").ifBlank { null }
                                ),
                                color = CrimsonAnomaly,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    editingAccessorySession?.let { session ->
        AccessoryLabelDialog(
            session = session,
            onDismiss = { editingAccessorySession = null },
            onSave = { label ->
                viewModel.updateSessionAccessoryLabel(session.id, label)
                editingAccessorySession = null
            }
        )
    }

    longPressInfoTitle?.let { title ->
        InfoLongPressDialog(title = title, body = longPressInfoBody, onDismiss = { longPressInfoTitle = null })
    }

    accessoryDeleteConfirmation?.let { label ->
        AlertDialog(
            onDismissRequest = { accessoryDeleteConfirmation = null },
            title = { Text("Hapus grup aksesori?") },
            text = { Text("Label \"$label\" akan dikosongkan dari semua sesi dalam grup. Riwayat sesi dan sampel tetap tersimpan.") },
            confirmButton = {
                PowerActionButton(onClick = {
                    viewModel.clearAccessoryLabelGroup(label)
                    accessoryDeleteConfirmation = null
                }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Hapus label") }
            },
            dismissButton = { PowerActionButton(onClick = { accessoryDeleteConfirmation = null }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Batal") } }
        )
    }

    if (showResetAllAccessoryLabelsConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetAllAccessoryLabelsConfirmation = false },
            title = { Text("Reset semua label aksesori?") },
            text = { Text("Semua label aksesori akan dikosongkan. Riwayat sesi dan sampel tidak dihapus.") },
            confirmButton = {
                PowerActionButton(onClick = {
                    viewModel.clearAllAccessoryLabels()
                    showResetAllAccessoryLabelsConfirmation = false
                }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Reset semua") }
            },
            dismissButton = { PowerActionButton(onClick = { showResetAllAccessoryLabelsConfirmation = false }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Batal") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccessoryLabelDialog(
    session: ChargingSessionEntity,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var label by rememberSaveable(session.id) { mutableStateOf(session.accessoryLabel.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (session.accessoryLabel.isNullOrBlank()) "Tambah label aksesori" else "Ubah label aksesori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pilihan cepat", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(onClick = { label = "Charger Bawaan" }, label = { Text("Charger Bawaan") })
                    AssistChip(onClick = { label = "Adaptor Cadangan" }, label = { Text("Adaptor Cadangan") })
                    AssistChip(onClick = { label = "Kabel Utama" }, label = { Text("Kabel Utama") })
                    AssistChip(onClick = { label = "Kabel Cadangan" }, label = { Text("Kabel Cadangan") })
                }
                Text("Nama aksesori", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!session.accessoryLabel.isNullOrBlank()) {
                    PowerActionButton(onClick = { onSave(null) }, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Hapus") }
                }
                PowerActionButton(onClick = onDismiss, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) { Text("Batal") }
                PowerSaveButton(onClick = { onSave(label.trim().takeIf { it.isNotEmpty() }) }, enabled = label.isNotBlank(), clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) {
                    Text("Simpan")
                }
            }
        }
    )
}

@Composable
private fun AccessoryBenchmarkCard(
    result: AccessoryBenchmarkResult,
    onDeleteGroup: (String) -> Unit,
    onResetAll: () -> Unit,
    onLongPressInfo: () -> Unit
) {
    val ranking = remember(result.groups) { AccessoryBenchmarkRanker.rank(result.groups) }
    val maxRate = result.groups.mapNotNull { it.averageChargeRatePercentPerHour }
        .filter { it.isFinite() && it > 0.0 }.maxOrNull() ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(onLongPress = onLongPressInfo),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Perbandingan Aksesori", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                PowerActionButton(onClick = onResetAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Reset Semua")
                }
            }
            Text(
                "Laju dinormalisasi sebagai kenaikan level per jam; suhu puncak adalah rata-rata sesi berlabel yang selesai.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            result.groups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.label, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (ranking.fastestLabel == group.label) {
                            Surface(color = EmeraldCharge.copy(alpha = 0.13f), contentColor = EmeraldCharge,
                                shape = RoundedCornerShape(50)) {
                                Text("Tercepat", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (group.label in ranking.warmLabels) {
                            Spacer(Modifier.width(4.dp))
                            Surface(color = AmberThermal.copy(alpha = 0.15f), contentColor = AmberThermal,
                                shape = RoundedCornerShape(50)) {
                                Text("Cenderung Panas", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${group.sessionCount} sesi", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PowerActionButton(onClick = { onDeleteGroup(group.label) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Hapus") }
                    }
                    val rate = group.averageChargeRatePercentPerHour
                    val fraction = if (rate != null && maxRate > 0.0) (rate / maxRate).toFloat().coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth().height(5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))) {
                        if (fraction > 0f) {
                            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight()
                                .background(if (ranking.fastestLabel == group.label) EmeraldCharge else MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccessoryMetric("Laju pengisian", rate, "%/jam", Modifier.weight(1f))
                        AccessoryMetric("Suhu puncak rata-rata", group.averagePeakTemperatureCelsius, "°C", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccessoryMetric("Daya rata-rata", group.averageBatteryPowerW, "W", Modifier.weight(1f))
                        AccessoryMetric("Arus rata-rata", group.averageCurrentMa, "mA", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryMetric(
    label: String,
    value: Double?,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value?.let {
                if (unit == "%/jam" && it > 0.0) String.format(Locale.US, "+%.1f %s", it, unit)
                else String.format(Locale.US, "%.1f %s", it, unit)
            } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
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
    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold, color = accent)
            Text(subValue, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun formatDurationIndo(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}j ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}d"
        else -> "${seconds}d"
    }
}

@Composable
private fun CyclesAndRollupTab(viewModel: PowerGuardViewModel, bottomScrollPadding: PaddingValues) {
    val rollups by viewModel.dailyRollups.collectAsState()
    val today = LocalDate.now()
    val trend = remember(rollups, today) { DailyCycleTrend.aggregate(rollups, today) }
    var selectedDate by remember(trend) { mutableStateOf(trend.lastOrNull()?.date) }
    var showLongPressInfo by remember { mutableStateOf(false) }

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
                Text("Laporan Siklus Harian", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(end = 10.dp))
                PowerActionButton(
                    onClick = { viewModel.triggerManualRollup() },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text("Hitung Sekarang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, softWrap = false)
                }
            }
        }
        item {
            SevenDayCycleMiniChart(
                points = trend,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onLongPressInfo = { showLongPressInfo = true }
            )
        }
        if (rollups.isEmpty()) {
            item {
                Text("Belum ada ringkasan harian. Tekan 'Hitung Sekarang' setelah melakukan sesi pengisian.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(rollups, key = { it.dateKey }) { rollup ->
                Card(
                    modifier = Modifier.fillMaxWidth().infoLongPress(onLongPress = { showLongPressInfo = true }),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(DailyCycleTrend.formatDateLabel(rollup.dateKey, today),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (isDailyRollupSelected(rollup.dateKey, selectedDate)) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(50.dp)) {
                                    Text("Dipilih", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Akumulasi Siklus: ${String.format(Locale.US, "%.2f", rollup.totalCycles)} siklus • Total Masuk: +${NumberFormat.getIntegerInstance(Locale("id", "ID")).format(rollup.totalMahCharged.roundToLong())} mAh",
                            style = MaterialTheme.typography.bodyMedium, color = EmeraldCharge)
                        Text(
                            "Suhu Maks: ${String.format(Locale.US, "%.1f", rollup.maxTempCelsius)}°C • Jumlah Anomali: ${rollup.anomalyCount}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (showLongPressInfo) {
        InfoLongPressDialog(
            title = "Siklus harian",
            body = "Grafik merangkum siklus dan suhu puncak selama tujuh hari. Titik merah menandai suhu puncak minimal 40°C. Memilih batang akan menyorot kartu tanggal yang sama di daftar bawah.",
            onDismiss = { showLongPressInfo = false }
        )
    }
}

@Composable
private fun SevenDayCycleMiniChart(
    points: List<DailyCycleTrendPoint>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onLongPressInfo: () -> Unit
) {
    val maxCycles = points.maxOfOrNull { it.totalCycles }?.coerceAtLeast(0.1) ?: 0.1
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(onLongPress = onLongPressInfo),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tren Siklus & Suhu Puncak · 7 Hari", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { onDateSelected(point.date) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (point.peakTemperatureCelsius != null && point.peakTemperatureCelsius >= 40.0) {
                            Box(Modifier.size(6.dp).background(CrimsonAnomaly, RoundedCornerShape(50)))
                        } else {
                            Spacer(Modifier.size(6.dp))
                        }
                        Text(if (point.hasData) String.format(Locale.US, "%.1f", point.totalCycles) else "—",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1)
                        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.BottomCenter) {
                            val barHeight = if (point.hasData && point.totalCycles > 0.0)
                                (point.totalCycles / maxCycles * 44.0).coerceAtLeast(3.0).dp else 2.dp
                            Box(
                                Modifier.fillMaxWidth(0.58f).height(barHeight)
                                    .background(
                                        if (point.peakTemperatureCelsius != null && point.peakTemperatureCelsius >= 40.0)
                                            CrimsonAnomaly else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                        Text(DailyCycleTrend.formatWeekdayShort(point.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point.date == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (point.date == selectedDate) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageTab(
    viewModel: PowerGuardViewModel,
    bottomScrollPadding: PaddingValues,
    popupDetectorOpenRequest: Int,
    popupDetectorViewModel: PopupDetectorViewModel
) {
    val context = LocalContext.current
    val hasPerm by viewModel.hasUsagePermission.collectAsState()
    val apps by viewModel.appUsageList.collectAsState()
    val popupDetectorState by popupDetectorViewModel.uiState.collectAsState()
    var showLongPressInfo by remember { mutableStateOf(false) }
    var showPopupDetectorInfo by remember { mutableStateOf(false) }
    var showPopupDetectorSheet by remember { mutableStateOf(false) }

    LaunchedEffect(popupDetectorOpenRequest) {
        if (popupDetectorOpenRequest > 0) {
            showPopupDetectorSheet = true
            popupDetectorViewModel.scan()
        }
    }

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

        item {
            PopupDetectorCard(
                state = popupDetectorState,
                onOpenTimeline = {
                    showPopupDetectorSheet = true
                    popupDetectorViewModel.scan()
                },
                onRescan = popupDetectorViewModel::scan,
                onInfo = { showPopupDetectorInfo = true },
                onOpenUsageSettings = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                }
            )
        }

        if (hasPerm && apps.isEmpty()) {
            item {
                Text(
                    text = "Tidak ada aktivitas aplikasi signifikan dalam 24 jam terakhir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (hasPerm) {
            items(apps, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().infoLongPress(onLongPress = { showLongPressInfo = true }),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appDisplayName = remember(app.packageName) {
                            val systemLabel = runCatching {
                                val applicationInfo = context.packageManager.getApplicationInfo(app.packageName, 0)
                                context.packageManager.getApplicationLabel(applicationInfo).toString()
                            }.getOrNull()
                            AppDisplayNameFormatter.format(app.packageName, systemLabel)
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
    if (showPopupDetectorInfo) {
        PopupDetectorInfoDialog(
            title = "Detektor Pop-up & Pembajak Layar",
            body = "Detektor membaca riwayat perpindahan aktivitas dan status layar Android selama 24 jam terakhir. Skor merupakan heuristik; Riwayat aktivitas tidak membuktikan jendela overlay benar-benar tampil atau memastikan suatu aplikasi adalah malware. Pemindaian berjalan hanya saat tab ini dibuka, tombol Pindai Ulang digunakan, atau tile Cek Pop-up diketuk.",
            onDismiss = { showPopupDetectorInfo = false }
        )
    }
    if (showPopupDetectorSheet) {
        PopupDetectorSheet(
            state = popupDetectorState,
            onDismiss = { showPopupDetectorSheet = false },
            onRescan = popupDetectorViewModel::scan,
            onMarkSafe = popupDetectorViewModel::markSafe,
            onRestoreSafeList = popupDetectorViewModel::restoreSafeList,
            onRestoreSafePackage = popupDetectorViewModel::restoreSafePackage,
            onRunSimulation = popupDetectorViewModel::showSimulation,
            onClearSimulation = popupDetectorViewModel::clearSimulation,
            onOpenUsageSettings = {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            },
            onOpenAppDetails = { packageName ->
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:" + packageName)
                        }
                    )
                }
            },
            onSelectTimelineFilter = popupDetectorViewModel::setTimelineFilter
        )
    }
    if (showLongPressInfo) {
        InfoLongPressDialog(
            title = "Aktivitas Aplikasi",
            body = "Nama utama berasal dari label PackageManager bila tersedia. Jika Android membatasi package visibility atau label tidak terbaca, SPG memakai formatter nama paket yang dikenal. Baris kedua selalu mempertahankan package name asli. Data penggunaan berasal dari Usage Access Android.",
            onDismiss = { showLongPressInfo = false }
        )
    }
}
