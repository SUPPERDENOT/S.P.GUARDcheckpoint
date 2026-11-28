package com.superdenot.powerguard.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import com.superdenot.powerguard.ui.theme.AmberThermal
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
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Informasi heuristik detektor pop-up",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (suspects.isNotEmpty()) {
                suspects.forEach { incident ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val appLabel = popupAppLabel(incident)
                        PopupAppIcon(incident.packageName, appLabel, Modifier.size(28.dp))
                        Text(
                            appLabel + " · " + incidentSummary(incident),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (state.hasUsagePermission && !state.hasActivityData && !state.isScanning) {
                Text(
                    "Belum ada riwayat perpindahan aktivitas yang dapat dinilai pada periode ini.",
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
                CompactActionButton(
                    label = "Lihat Lini Masa",
                    contentDescription = "Buka Lini Masa detektor pop-up",
                    onClick = onOpenTimeline,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Timeline
                )
                CompactActionButton(
                    label = "Pindai Ulang",
                    contentDescription = "Pindai ulang aktivitas aplikasi",
                    onClick = onRescan,
                    enabled = !state.isScanning && !state.isLoadingArchive,
                    primary = true,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Refresh
                )
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
    onOpenUsageSettings: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onSelectTimelineFilter: (TimelineRiskFilter) -> Unit
) {
    var showSafeListDialog by remember { mutableStateOf(false) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactActionButton(
                            label = "Daftar Aman (${safePackages.size})",
                            contentDescription = "Buka Daftar Aman, ${safePackages.size} aplikasi",
                            onClick = { showSafeListDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        CompactActionButton(
                            label = "Pindai Ulang",
                            contentDescription = "Pindai ulang aktivitas aplikasi",
                            onClick = onRescan,
                            enabled = !state.isScanning && !state.isLoadingArchive && state.hasUsagePermission,
                            primary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        .heightIn(max = if (timeline.isEmpty()) 180.dp else 420.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "scan-summary") {
                        Text(
                            when {
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
                    item(key = "timeline-section-title") {
                        Text(
                            "Lini Masa",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    item(key = "timeline-filter-chips") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TimelineFilterChip(
                                label = "Semua",
                                selected = state.timelineFilter == TimelineRiskFilter.ALL,
                                contentDescription = "Filter semua insiden, ${state.timelineEvents.size} aplikasi",
                                modifier = Modifier.weight(0.82f),
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.ALL) }
                            )
                            TimelineFilterChip(
                                label = "Mencurigakan",
                                selected = state.timelineFilter == TimelineRiskFilter.SUSPICIOUS,
                                contentDescription = "Filter Mencurigakan, ${state.suspiciousCount} aplikasi",
                                modifier = Modifier.weight(1.15f),
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.SUSPICIOUS) }
                            )
                            TimelineFilterChip(
                                label = "Perlu Dicek",
                                selected = state.timelineFilter == TimelineRiskFilter.NEEDS_REVIEW,
                                contentDescription = "Filter Perlu Dicek, ${state.needsReviewCount} aplikasi",
                                modifier = Modifier.weight(1.03f),
                                onClick = { onSelectTimelineFilter(TimelineRiskFilter.NEEDS_REVIEW) }
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
                                    CompactActionButton(
                                        label = "Reset Filter",
                                        contentDescription = "Reset filter Lini Masa ke Semua",
                                        onClick = { onSelectTimelineFilter(TimelineRiskFilter.ALL) },
                                        modifier = Modifier.width(120.dp)
                                    )
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
    if (showSafeListDialog) {
        Dialog(onDismissRequest = { showSafeListDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(max = 520.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Daftar Aman", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${safePackages.size} aplikasi diizinkan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CompactActionButton(
                            label = "Pulihkan Semua",
                            contentDescription = "Pulihkan semua aplikasi dari Daftar Aman",
                            onClick = {
                                onRestoreSafeList()
                                showSafeListDialog = false
                            },
                            enabled = safePackages.isNotEmpty() && !state.isLoadingArchive && !state.isScanning,
                            primary = true,
                            modifier = Modifier.width(132.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showSafeListDialog = false },
                            modifier = Modifier.heightIn(min = 48.dp).semantics {
                                contentDescription = "Tutup Daftar Aman"
                            }
                        ) { Text("Tutup", maxLines = 1, softWrap = false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun popupAppLabel(incident: PopupIncident): String {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(
        initialValue = popupDisplayName(incident.packageName, null),
        key1 = context,
        key2 = incident.packageName
    ) {
        val label = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val info = context.packageManager.getApplicationInfo(incident.packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
        value = popupDisplayName(incident.packageName, label)
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
                PopupAppIcon(incident.packageName, appLabel, Modifier.size(36.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        appLabel,
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
                val badgeAccent = if (suspicious) MaterialTheme.colorScheme.error else AmberThermal
                Surface(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = if (suspicious) "Status risiko: Mencurigakan"
                            else "Status risiko: Perlu Dicek"
                    },
                    color = if (suspicious) MaterialTheme.colorScheme.errorContainer else AmberThermal.copy(alpha = 0.12f),
                    contentColor = if (suspicious) MaterialTheme.colorScheme.onErrorContainer else AmberThermal,
                    border = BorderStroke(1.dp, badgeAccent),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape).background(
                                badgeAccent
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
                CompactActionButton(
                    label = "Info & Hapus",
                    contentDescription = "Buka detail aplikasi ${incident.packageName}",
                    onClick = onInfoAndRemove,
                    modifier = Modifier.weight(1f)
                )
                CompactActionButton(
                    label = "Tandai Aman",
                    contentDescription = "Tandai ${incident.packageName} sebagai aplikasi aman",
                    onClick = onMarkSafe,
                    primary = true,
                    modifier = Modifier.weight(1f)
                )
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
private fun TimelineFilterChip(
    label: String,
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                this.selected = selected
            }
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class PopupAppIconResult(
    val bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    val packageNotFound: Boolean
)

@Composable
internal fun PopupAppIcon(packageName: String, appLabel: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconResult by androidx.compose.runtime.produceState<PopupAppIconResult?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                PopupAppIconResult(
                    bitmap = context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap(),
                    packageNotFound = false
                )
            } catch (_: PackageManager.NameNotFoundException) {
                PopupAppIconResult(bitmap = null, packageNotFound = true)
            }
        }
    }
    when (val result = iconResult) {
        null -> Spacer(modifier)
        else -> when {
            result.packageNotFound -> Icon(
                Icons.Rounded.Apps,
                contentDescription = "Ikon aplikasi tidak ditemukan",
                modifier = modifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            result.bitmap != null -> Image(
                result.bitmap,
                contentDescription = "Ikon $appLabel",
                modifier = modifier
            )
            else -> Spacer(modifier)
        }
    }
}

@Composable
private fun CompactActionButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = CircleShape,
            color = when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant
                primary -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                primary -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            },
            border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PopupSafePackageRow(packageName: String, onRestore: () -> Unit) {
    val context = LocalContext.current
    val appLabel by androidx.compose.runtime.produceState(
        initialValue = AppDisplayNameFormatter.format(packageName, null),
        key1 = packageName
    ) {
        val label = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
        value = AppDisplayNameFormatter.format(packageName, label)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        PopupAppIcon(packageName, appLabel, Modifier.size(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(appLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CompactActionButton(
            label = "Pulihkan",
            contentDescription = "Pulihkan $packageName dari daftar aman",
            onClick = onRestore,
            modifier = Modifier.width(88.dp)
        )
    }
}

private fun popupDisplayName(packageName: String, label: String?): String =
    AppDisplayNameFormatter.format(packageName, label)

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
    PopupSignalReason.BURST_LOOP_2, PopupSignalReason.BURST_LOOP_3_PLUS -> "Siklus berulang dalam 5 menit"
}

private fun formatPopupTime(timestampMs: Long): String =
    SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(timestampMs))
