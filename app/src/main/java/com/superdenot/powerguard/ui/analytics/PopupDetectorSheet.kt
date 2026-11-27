package com.superdenot.powerguard.ui.analytics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import com.superdenot.powerguard.ui.components.InfoDialogSurface
import com.superdenot.powerguard.ui.components.infoLongPress
import com.superdenot.powerguard.ui.viewmodel.PopupDetectorUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PopupDetectorCard(
    state: PopupDetectorUiState,
    onOpenTimeline: () -> Unit,
    onRescan: () -> Unit,
    onInfo: () -> Unit
) {
    val suspects = state.currentIncidents.take(3)
    val statusText = when {
        state.isScanning -> "Sedang memindai..."
        !state.hasUsagePermission -> "Akses Penggunaan belum aktif"
        state.currentIncidents.isEmpty() -> "Layar Aman"
        else -> state.currentIncidents.size.toString() + " indikasi perlu diperiksa"
    }
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(
            onLongPress = onInfo,
            onClick = onOpenTimeline
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.currentIncidents.isEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
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
                        popupDisplayName(LocalContext.current, incident) + " · " + incidentSummary(incident),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (state.hasUsagePermission && !state.isScanning) {
                Text(
                    "Belum ada pola yang memenuhi ambang pemeriksaan pada pemindaian terakhir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!state.hasUsagePermission) {
                Text(
                    "Aktifkan Akses Penggunaan Android agar SPG dapat membaca riwayat perpindahan aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Lihat Lini Masa", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onRescan,
                    enabled = !state.isScanning && state.hasUsagePermission,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
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
    onRunSimulation: () -> Unit,
    onClearSimulation: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onOpenSystemApps: () -> Unit
) {
    var showSimulatedInfo by remember { mutableStateOf(false) }
    val timeline = remember(state.currentIncidents, state.archivedIncidents) {
        (state.currentIncidents + state.archivedIncidents)
            .distinctBy { listOf(it.packageName, it.lastSeenAtMs.toString(), it.isSimulated.toString()).joinToString(":") }
            .sortedByDescending { it.lastSeenAtMs }
    }
    InfoDialogSurface(
        title = "Lini masa detektor pop-up",
        subtitle = "Pola aktivitas 24 jam terakhir · indikasi, bukan bukti malware",
        onDismiss = onDismiss
    ) {
        Text(
            when {
                !state.hasUsagePermission -> "Akses Penggunaan belum aktif. Pemindaian nyata belum tersedia."
                state.isScanning -> "Sedang membaca jejak aktivitas aplikasi..."
                state.currentIncidents.isEmpty() -> "Tidak ada indikasi yang memenuhi ambang pemeriksaan."
                else -> state.currentIncidents.size.toString() + " aplikasi memiliki indikasi untuk diperiksa."
            },
            style = MaterialTheme.typography.bodyMedium
        )
        if (!state.hasUsagePermission) {
            Text(
                "Aktifkan Akses Penggunaan melalui tombol pada kartu detektor. SPG hanya membaca data ketika pemindaian diminta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRunSimulation,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text("Uji Simulasi", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            if (state.simulationActive) {
                Button(
                    onClick = onClearSimulation,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text("Hapus Simulasi", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Button(
                    onClick = onRescan,
                    enabled = !state.isScanning && state.hasUsagePermission,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text("Pindai Ulang", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daftar aman: " + state.safePackages.size + " aplikasi",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRestoreSafeList) {
                Text("Pulihkan daftar aman", maxLines = 1, softWrap = false)
            }
        }
        if (timeline.isEmpty()) {
            Text(
                "Belum ada insiden tersimpan. Jalankan pemindaian atau aktifkan Uji Simulasi untuk mencoba tampilan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            timeline.forEach { incident ->
                PopupIncidentCard(
                    incident = incident,
                    onInfoAndRemove = {
                        if (incident.isSimulated) showSimulatedInfo = true
                        else onOpenAppDetails(incident.packageName)
                    },
                    onMarkSafe = { onMarkSafe(incident) }
                )
            }
        }
        state.errorMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
    if (showSimulatedInfo) {
        AlertDialog(
            onDismissRequest = { showSimulatedInfo = false },
            title = { Text("Item Uji Simulasi") },
            text = {
                Text("Item ini dibuat oleh SPG untuk menguji tampilan dan alur detektor. Tidak ada aplikasi nyata yang dipasang atau dihapus.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showSimulatedInfo = false
                    onOpenSystemApps()
                }) { Text("Daftar Aplikasi Sistem") }
            },
            dismissButton = {
                TextButton(onClick = { showSimulatedInfo = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun PopupIncidentCard(
    incident: PopupIncident,
    onInfoAndRemove: () -> Unit,
    onMarkSafe: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(context, incident.packageName, incident.isSimulated) {
        if (incident.isSimulated) null else runCatching {
            context.packageManager.getApplicationIcon(incident.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    Card(
        modifier = Modifier.fillMaxWidth().infoLongPress(
            onLongPress = {},
            onClick = {}
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (bitmap != null) {
                    Image(bitmap, contentDescription = "Ikon " + popupDisplayName(context, incident), modifier = Modifier.size(36.dp))
                } else {
                    Icon(Icons.Rounded.Apps, contentDescription = "Ikon aplikasi tidak tersedia",
                        modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        popupDisplayName(context, incident) + if (incident.isSimulated) " · Uji Simulasi" else "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
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
                Text(
                    if (incident.status == PopupRiskStatus.SUSPICIOUS) "Mencurigakan" else "Perlu Dicek",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (incident.status == PopupRiskStatus.SUSPICIOUS) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
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
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("Info & Hapus", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onMarkSafe,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("Tandai Aman", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun popupDisplayName(context: Context, incident: PopupIncident): String {
    if (incident.isSimulated) {
        return if (incident.packageName.contains("redirect")) "Simulasi Pengalihan Browser"
            else "Simulasi Pop-up"
    }
    val label = runCatching {
        val info = context.packageManager.getApplicationInfo(incident.packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrNull()
    return AppDisplayNameFormatter.format(incident.packageName, label)
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
