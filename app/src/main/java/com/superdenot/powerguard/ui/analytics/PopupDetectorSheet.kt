package com.superdenot.powerguard.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.graphics.drawable.toBitmap
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import com.superdenot.powerguard.ui.components.infoLongPress
import com.superdenot.powerguard.ui.viewmodel.PopupDetectorUiState
import com.superdenot.powerguard.ui.viewmodel.TimelineRiskFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PopupDetectorCard(
    state: PopupDetectorUiState,
    onOpenTimeline: () -> Unit,
    onRescan: () -> Unit,
    onInfo: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    val suspects = state.suspects.take(3)
    val statusText = when {
        state.isScanning -> "Sedang memindai..."
        state.simulationActive -> "Mode uji simulasi aktif (data contoh tidak ditampilkan)"
        !state.hasUsagePermission -> "Akses Penggunaan belum aktif"
        !state.hasActivityData -> "Data aktivitas belum tersedia"
        suspects.isEmpty() && state.archivedIncidents.isNotEmpty() -> "Tidak ada indikasi baru"
        suspects.isEmpty() -> "Layar Aman"
        else -> suspects.size.toString() + " indikasi perlu diperiksa"
    }
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(
            onLongPress = onInfo,
            onClick = onOpenTimeline
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Detektor Pop-up & Pembajak Layar",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suspects.isEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (state.isScanning) {
                    Text("24 jam", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (suspects.isNotEmpty()) {
                suspects.forEach { incident ->
                    Text(
                        popupAppLabel(incident) + " · " + incidentSummary(incident),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (state.hasUsagePermission && !state.hasActivityData && !state.isScanning) {
                Text(
                    "Belum ada riwayat perpindahan aktivitas yang dapat dinilai pada periode ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!state.hasUsagePermission && state.simulationActive) {
                Text(
                    "Item yang tampil berasal dari simulasi. Aktifkan Akses Penggunaan untuk memindai aktivitas nyata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!state.hasUsagePermission) {
                Text(
                    "Aktifkan Akses Penggunaan Android agar SPG dapat membaca riwayat perpindahan aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOpenUsageSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                        contentDescription = "Buka pengaturan Akses Penggunaan Android"
                    }
                ) { Text("Buka Akses Penggunaan", maxLines = 1) }
            }
            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenTimeline,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        contentDescription = "Buka Lini Masa detektor pop-up"
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Lihat Lini Masa", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onRescan,
                    enabled = !state.isScanning && !state.isLoadingArchive,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        contentDescription = "Pindai ulang aktivitas aplikasi"
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Pindai Ulang", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun PopupDetectorSheet(
    state: PopupDetectorUiState,
    onDismiss: () -> Unit,
    onRescan: () -> Unit,
    onMarkSafe: (PopupIncident) -> Unit,
    onRestoreSafeList: () -> Unit,
    onRestoreSafePackage: (String) -> Unit,
    onRunSimulation: () -> Unit,
    onClearSimulation: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onSelectTimelineFilter: (TimelineRiskFilter) -> Unit
) {
    var showSafeList by remember { mutableStateOf(false) }
    val safePackages = remember(state.safePackages) { state.safePackages.sorted() }
    val timeline = state.filteredTimelineEvents

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Lini masa detektor pop-up", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pola aktivitas 24 jam terakhir · indikasi, bukan bukti malware",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "scan-summary") {
                        Text(
                            when {
                                state.simulationActive -> "Mode simulasi aktif. Data contoh tidak dimasukkan ke daftar aplikasi atau Lini Masa."
                                !state.hasUsagePermission -> "Akses Penggunaan belum aktif. Pemindaian nyata belum tersedia."
                                state.isLoadingArchive -> "Memuat dan memeriksa arsip deteksi..."
                                state.isScanning -> "Sedang membaca jejak aktivitas aplikasi..."
                                !state.hasActivityData -> "Belum ada riwayat perpindahan aktivitas yang dapat dinilai."
                                state.suspects.isEmpty() -> "Tidak ada indikasi aktif yang memenuhi ambang pemeriksaan."
                                else -> "${state.suspects.size} aplikasi memiliki indikasi untuk diperiksa."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!state.hasUsagePermission) {
                        item(key = "usage-permission") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Aktifkan Akses Penggunaan melalui tombol di bawah. SPG hanya membaca data ketika pemindaian diminta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onOpenUsageSettings,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                                        contentDescription = "Buka pengaturan Akses Penggunaan Android"
                                    }
                                ) { Text("Buka Akses Penggunaan", maxLines = 1) }
                            }
                        }
                    }
                    item(key = "scan-actions") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onRunSimulation,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                    contentDescription = "Aktifkan mode uji simulasi"
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Uji Simulasi", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                            if (state.simulationActive) {
                                Button(
                                    onClick = onClearSimulation,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Matikan mode uji simulasi"
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("Hapus Simulasi", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            } else {
                                Button(
                                    onClick = onRescan,
                                    enabled = !state.isScanning && !state.isLoadingArchive && state.hasUsagePermission,
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Pindai ulang aktivitas aplikasi"
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("Pindai Ulang", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    item(key = "safe-list-header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Daftar Aman", style = MaterialTheme.typography.titleSmall)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "${state.safePackages.size} aplikasi",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { showSafeList = !showSafeList },
                                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                                        contentDescription = if (showSafeList) "Sembunyikan daftar aplikasi aman"
                                            else "Lihat daftar aplikasi aman"
                                    }
                                ) {
                                    Text(if (showSafeList) "Sembunyikan" else "Lihat", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                    if (showSafeList) {
                        item(key = "safe-list-restore-all") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onRestoreSafeList,
                                    enabled = state.safePackages.isNotEmpty() && !state.isLoadingArchive && !state.isScanning,
                                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                                        contentDescription = "Pulihkan semua aplikasi dari daftar aman"
                                    }
                                ) { Text("Pulihkan Semua", maxLines = 1, softWrap = false) }
                            }
                        }
                        if (safePackages.isEmpty()) {
                            item(key = "safe-list-empty") {
                                Text(
                                    "Belum ada aplikasi yang ditandai aman.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(safePackages, key = { "safe-$it" }) { packageName ->
                                PopupSafePackageRow(packageName) { onRestoreSafePackage(packageName) }
                            }
                        }
                    }
                    item(key = "timeline-section-title") {
                        Text(
                            "Lini Masa",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    item(key = "timeline-filter-chips") {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.timelineFilter == TimelineRiskFilter.ALL,
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.ALL) },
                                label = { Text("Semua (${state.timelineEvents.size})", maxLines = 1, softWrap = false) },
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = "Filter semua insiden, ${state.timelineEvents.size} aplikasi"
                                }
                            )
                            FilterChip(
                                selected = state.timelineFilter == TimelineRiskFilter.SUSPICIOUS,
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.SUSPICIOUS) },
                                label = { Text("Mencurigakan (${state.suspiciousCount})", maxLines = 1, softWrap = false) },
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = "Filter Mencurigakan, ${state.suspiciousCount} aplikasi"
                                }
                            )
                            FilterChip(
                                selected = state.timelineFilter == TimelineRiskFilter.NEEDS_REVIEW,
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.NEEDS_REVIEW) },
                                label = { Text("Perlu Dicek (${state.needsReviewCount})", maxLines = 1, softWrap = false) },
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = "Filter Perlu Dicek, ${state.needsReviewCount} aplikasi"
                                }
                            )
                        }
                    }
                    if (timeline.isEmpty()) {
                        item(key = "timeline-empty") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (state.timelineFilter == TimelineRiskFilter.ALL) {
                                        if (state.isLoadingArchive) "Sedang memuat Lini Masa..."
                                        else "Belum ada insiden nyata yang tersimpan."
                                    } else {
                                        "Tidak ada aplikasi dengan status ${state.timelineFilter.label}."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (state.timelineFilter != TimelineRiskFilter.ALL) {
                                    TextButton(
                                        onClick = { onSelectTimelineFilter(TimelineRiskFilter.ALL) },
                                        modifier = Modifier.heightIn(min = 48.dp).semantics {
                                            contentDescription = "Reset filter Lini Masa ke Semua"
                                        }
                                    ) { Text("Reset Filter") }
                                }
                            }
                        }
                    } else {
                        items(timeline, key = { "timeline-${it.packageName}" }) { incident ->
                            PopupIncidentCard(
                                incident = incident,
                                onInfoAndRemove = { onOpenAppDetails(incident.packageName) },
                                onMarkSafe = { onMarkSafe(incident) }
                            )
                        }
                    }
                    state.errorMessage?.let { message ->
                        item(key = "timeline-error") {
                            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp).semantics {
                            contentDescription = "Tutup Lini Masa detektor pop-up"
                        }
                    ) { Text("Mengerti") }
                }
            }
        }
    }
}

@Composable
private fun popupAppLabel(incident: PopupIncident): String {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(
        initialValue = popupDisplayName(incident.packageName, incident.isSimulated, null),
        key1 = context,
        key2 = incident.packageName,
        key3 = incident.isSimulated
    ) {
        value = if (incident.isSimulated) {
            popupDisplayName(incident.packageName, true, null)
        } else {
            val label = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val info = context.packageManager.getApplicationInfo(incident.packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                }.getOrNull()
            }
            popupDisplayName(incident.packageName, false, label)
        }
    }.value
}

@Composable
private fun PopupIncidentCard(
    incident: PopupIncident,
    onInfoAndRemove: () -> Unit,
    onMarkSafe: () -> Unit
) {
    val context = LocalContext.current
    var showIncidentInfo by remember { mutableStateOf(false) }
    val appLabel = popupAppLabel(incident)
    val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = context,
        key2 = incident.packageName,
        key3 = incident.isSimulated
    ) {
        value = if (incident.isSimulated) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(incident.packageName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(
            onLongPress = { showIncidentInfo = true },
            onClick = { showIncidentInfo = true }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val loadedBitmap = bitmap
                if (loadedBitmap != null) {
                    Image(
                        loadedBitmap,
                        contentDescription = "Ikon " + appLabel,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.Apps,
                        contentDescription = "Ikon aplikasi tidak tersedia",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        appLabel + if (incident.isSimulated) " · Uji Simulasi" else "",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        incident.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val suspicious = incident.status == PopupRiskStatus.SUSPICIOUS
                Surface(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = if (suspicious) "Status risiko: Mencurigakan"
                            else "Status risiko: Perlu Dicek"
                    },
                    color = if (suspicious) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (suspicious) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    border = BorderStroke(1.dp, if (suspicious) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape).background(
                                if (suspicious) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Text(
                            if (suspicious) "Mencurigakan" else "Perlu Dicek",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Text(
                "Skor indikasi " + incident.riskScore + "/100 · " + incidentSummary(incident),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            incident.reasons.forEach { reason ->
                Text(
                    reasonDisplayLabel(reason),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (incident.eventTimesMs.isNotEmpty()) {
                Text(
                    "Waktu: " + incident.eventTimesMs.takeLast(4).joinToString(", ") { formatPopupTime(it) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInfoAndRemove,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        contentDescription = "Buka detail aplikasi ${incident.packageName}"
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("Info & Hapus", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onMarkSafe,
                    enabled = !incident.isSimulated,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        contentDescription = "Tandai ${incident.packageName} sebagai aplikasi aman"
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("Tandai Aman", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (showIncidentInfo) {
        PopupDetectorInfoDialog(
            title = appLabel,
            body = buildString {
                append("Pola yang teramati berasal dari riwayat perpindahan aktivitas Android.")
                if (incident.reasons.isNotEmpty()) {
                    append("\n\n")
                    append(incident.reasons.joinToString("\n") {
                        "• " + it.userLabel.trim().trimEnd('.', ',', ';') + "."
                    })
                }
                append("\n\nIzin overlay dan metadata paket merupakan sinyal tambahan, bukan bukti bahwa jendela iklan benar-benar tampil atau bahwa aplikasi adalah malware. Tandai Aman jika aplikasi ini memang Anda kenal.")
            },
            onDismiss = { showIncidentInfo = false }
        )
    }
}


@Composable
internal fun PopupDetectorInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp).semantics {
                            contentDescription = "Tutup penjelasan aplikasi"
                        }
                    ) { Text("Mengerti") }
                }
            }
        }
    }
}

@Composable
private fun PopupSafePackageRow(packageName: String, onRestore: () -> Unit) {
    val context = LocalContext.current
    val appLabel by androidx.compose.runtime.produceState(
        initialValue = AppDisplayNameFormatter.format(packageName, null),
        key1 = context,
        key2 = packageName
    ) {
        val label = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrNull()
        }
        value = AppDisplayNameFormatter.format(packageName, label)
    }
    val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, key1 = context, key2 = packageName
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap() }.getOrNull()
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        if (bitmap != null) Image(bitmap!!, contentDescription = "Ikon $appLabel", modifier = Modifier.size(32.dp))
        else Icon(Icons.Rounded.Apps, contentDescription = "Ikon aplikasi tidak tersedia",
            modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(appLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(packageName, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(
            onClick = onRestore,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Pulihkan $packageName dari daftar aman"
            }
        ) { Text("Pulihkan", maxLines = 1, softWrap = false) }
    }
}

private fun popupDisplayName(packageName: String, isSimulated: Boolean, label: String?): String {
    if (isSimulated) {
        return if (packageName.contains("redirect")) "Simulasi Pengalihan Browser"
            else "Simulasi Pop-up"
    }
    return AppDisplayNameFormatter.format(packageName, label)
}

private fun incidentSummary(incident: PopupIncident): String {
    val times = incident.eventTimesMs.distinct().sorted()
    val burst = PopupSignalReason.BURST_LOOP_2 in incident.reasons ||
        PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons
    if (burst && times.size >= 2) {
        val averageMinutes = ((times.last() - times.first()) / (times.size - 1) / 60_000L).coerceAtLeast(1L)
        return "Muncul berulang " + times.size + "x tiap sekitar " + averageMinutes + " menit"
    }
    return when {
        PopupSignalReason.BROWSER_REDIRECT_CHAIN in incident.reasons ->
            "Muncul di antara aplikasi lain lalu membuka browser"
        PopupSignalReason.SANDWICH_LOOP in incident.reasons ->
            "Menyela layar singkat lalu kembali ke aplikasi sebelumnya"
        PopupSignalReason.POST_UNLOCK_AMBUSH in incident.reasons ->
            "Muncul segera setelah layar dibuka"
        PopupSignalReason.SCREEN_HIJACKING in incident.reasons ->
            "Menyela perpindahan layar aplikasi"
        else -> "Ada sinyal aplikasi yang perlu diperiksa"
    }
}

private fun reasonDisplayLabel(reason: PopupSignalReason): String = when (reason) {
    PopupSignalReason.NO_LAUNCHER_ICON -> "Tanpa ikon menu"
    PopupSignalReason.RECENT_INSTALL -> "Baru dipasang"
    PopupSignalReason.OVERLAY_PERMISSION -> "Izin overlay"
    PopupSignalReason.SCREEN_HIJACKING -> "Pola perpindahan layar tidak biasa"
    PopupSignalReason.BROWSER_REDIRECT_CHAIN -> "Diikuti pembukaan browser"
    PopupSignalReason.POST_UNLOCK_AMBUSH -> "Muncul setelah layar dibuka"
    PopupSignalReason.SANDWICH_LOOP -> "Menyela lalu kembali ke aplikasi sebelumnya"
    PopupSignalReason.BURST_LOOP_2, PopupSignalReason.BURST_LOOP_3_PLUS -> "Siklus berulang dalam 45 menit"
}

private fun formatPopupTime(timestampMs: Long): String =
    SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(timestampMs))
