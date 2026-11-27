package com.superdenot.powerguard.domain.security

enum class ScreenUsageEventType { ACTIVITY_RESUMED, ACTIVITY_PAUSED, SCREEN_INTERACTIVE, KEYGUARD_HIDDEN, OTHER }
data class ScreenUsageEvent(val packageName: String?, val eventType: ScreenUsageEventType, val timestampMs: Long)

data class AppPackageSignals(
    val packageName: String,
    val isMetadataResolved: Boolean = false,
    val isSystemApp: Boolean = false,
    val hasLauncherActivity: Boolean = false,
    val firstInstallTimeMs: Long? = null,
    val hasOverlayPermission: Boolean = false,
    val isBrowser: Boolean = false,
    val isHomeLauncher: Boolean = false,
    val isKeyboard: Boolean = false,
    val isPackageInstaller: Boolean = false,
    val isPermissionController: Boolean = false,
    val isSystemUi: Boolean = false,
    val isDialer: Boolean = false,
    val isSettings: Boolean = false,
    val isSelf: Boolean = false,
    val isDocumentPicker: Boolean = false
) {
    val isExcluded: Boolean get() = isBrowser || isHomeLauncher || isKeyboard ||
        isPackageInstaller || isPermissionController || isSystemUi || isDialer ||
        isSettings || isSelf || isDocumentPicker
}

enum class PopupRiskStatus { NEEDS_CHECK, SUSPICIOUS }
enum class PopupSignalReason(val points: Int, val userLabel: String) {
    SCREEN_HIJACKING(35, "Muncul menyela aplikasi lain tanpa kembali ke layar utama"),
    BROWSER_REDIRECT_CHAIN(30, "Muncul sebelum aplikasi membuka browser"),
    POST_UNLOCK_AMBUSH(25, "Muncul segera setelah layar dibuka"),
    SANDWICH_LOOP(20, "Muncul singkat lalu kembali ke aplikasi sebelumnya"),
    BURST_LOOP_2(25, "Muncul berulang sedikitnya 2 kali dalam 45 menit"),
    BURST_LOOP_3_PLUS(45, "Muncul berulang sedikitnya 3 kali dalam 45 menit"),
    NO_LAUNCHER_ICON(30, "Tidak memiliki ikon menu aplikasi"),
    RECENT_INSTALL(15, "Baru dipasang dalam 14 hari terakhir"),
    OVERLAY_PERMISSION(15, "Memiliki izin tampil di atas aplikasi lain")
}
data class PopupIncident(
    val packageName: String,
    val riskScore: Int,
    val status: PopupRiskStatus,
    val reasons: List<PopupSignalReason>,
    val eventTimesMs: List<Long>,
    val isSimulated: Boolean = false
) {
    val lastSeenAtMs: Long get() = eventTimesMs.maxOrNull() ?: 0L
    val occurrenceCount: Int get() = eventTimesMs.distinct().size
}
internal const val POPUP_LOOKBACK_MS = 24L * 60L * 60L * 1000L
internal const val POPUP_BURST_WINDOW_MS = 45L * 60L * 1000L
internal const val POPUP_MAX_AVERAGE_GAP_MS = 15L * 60L * 1000L
internal const val POPUP_RECENT_INSTALL_MS = 14L * 24L * 60L * 60L * 1000L

object PopupHijackDetector {
    fun evaluate(
        events: List<ScreenUsageEvent>,
        appSignals: Map<String, AppPackageSignals>,
        safePackages: Set<String>,
        nowMs: Long,
        isSimulated: Boolean = false
    ): List<PopupIncident> {
        val fromMs = nowMs - POPUP_LOOKBACK_MS
        val ordered = events.asSequence().filter { it.timestampMs in fromMs..nowMs }
            .sortedBy { it.timestampMs }.toList()
        val activityEvents = ordered.filter {
            it.eventType == ScreenUsageEventType.ACTIVITY_RESUMED && !it.packageName.isNullOrBlank()
        }.fold(mutableListOf<ScreenUsageEvent>()) { acc, event ->
            if (acc.lastOrNull()?.packageName != event.packageName) acc += event
            acc
        }
        if (activityEvents.isEmpty()) return emptyList()

        fun signals(packageName: String) =
            appSignals[packageName] ?: AppPackageSignals(packageName = packageName)
        fun isExcluded(packageName: String) = packageName in safePackages || signals(packageName).isExcluded
        fun isLauncher(packageName: String?) = packageName != null && signals(packageName).isHomeLauncher
        fun hasSuspiciousStaticSignal(packageName: String): Boolean {
            val metadata = signals(packageName)
            return metadata.isMetadataResolved && !metadata.isSystemApp &&
                (!metadata.hasLauncherActivity || metadata.hasOverlayPermission ||
                    isRecentlyInstalled(metadata.firstInstallTimeMs, nowMs))
        }

        val reasonsByPackage = linkedMapOf<String, MutableSet<PopupSignalReason>>()
        val timesByPackage = linkedMapOf<String, MutableSet<Long>>()
        fun record(packageName: String, reason: PopupSignalReason, at: Long) {
            if (isExcluded(packageName)) return
            reasonsByPackage.getOrPut(packageName) { linkedSetOf() } += reason
            timesByPackage.getOrPut(packageName) { linkedSetOf() } += at
        }

        activityEvents.forEachIndexed { index, current ->
            val packageName = current.packageName ?: return@forEachIndexed
            if (isExcluded(packageName)) return@forEachIndexed
            val previous = activityEvents.getOrNull(index - 1)
            val next = activityEvents.getOrNull(index + 1)
            val metadata = signals(packageName)

            if (previous != null && previous.packageName != packageName &&
                !isLauncher(previous.packageName) && hasSuspiciousStaticSignal(packageName)
            ) record(packageName, PopupSignalReason.SCREEN_HIJACKING, current.timestampMs)

            if (previous != null && next != null && previous.packageName != packageName &&
                next.packageName != packageName && !isLauncher(previous.packageName) &&
                signals(next.packageName.orEmpty()).isBrowser &&
                next.timestampMs - current.timestampMs in 0 until 5_000L
            ) record(packageName, PopupSignalReason.BROWSER_REDIRECT_CHAIN, current.timestampMs)

            if (previous != null && next != null && previous.packageName == next.packageName &&
                previous.packageName != packageName && next.timestampMs - current.timestampMs in 1_000L..15_000L
            ) record(packageName, PopupSignalReason.SANDWICH_LOOP, current.timestampMs)

            val unlockEvent = ordered.lastOrNull {
                (it.eventType == ScreenUsageEventType.SCREEN_INTERACTIVE ||
                    it.eventType == ScreenUsageEventType.KEYGUARD_HIDDEN) && it.timestampMs <= current.timestampMs
            }
            if (unlockEvent != null && current.timestampMs - unlockEvent.timestampMs in 0 until 3_000L &&
                (previous == null || previous.timestampMs < unlockEvent.timestampMs) && !metadata.isHomeLauncher
            ) record(packageName, PopupSignalReason.POST_UNLOCK_AMBUSH, current.timestampMs)
        }

        val interruptions = activityEvents.mapIndexedNotNull { index, event ->
            val pkg = event.packageName ?: return@mapIndexedNotNull null
            val previous = activityEvents.getOrNull(index - 1)
            if (previous == null || previous.packageName == pkg || isLauncher(previous.packageName) ||
                isExcluded(pkg) || (!hasSuspiciousStaticSignal(pkg) && reasonsByPackage[pkg].isNullOrEmpty())
            ) null else pkg to event.timestampMs
        }.groupBy({ it.first }, { it.second })

        interruptions.forEach { (pkg, rawTimes) ->
            val times = rawTimes.distinct().sorted()
            var bestWindow = emptyList<Long>()
            var start = 0
            for (end in times.indices) {
                while (times[end] - times[start] > POPUP_BURST_WINDOW_MS) start++
                val window = times.subList(start, end + 1)
                if (window.size > bestWindow.size) bestWindow = window.toList()
            }
            if (bestWindow.size >= 2) {
                val totalGap = bestWindow.last() - bestWindow.first()
                val maxAllowedTotalGap = POPUP_MAX_AVERAGE_GAP_MS * (bestWindow.size - 1)
                if (totalGap <= maxAllowedTotalGap) {
                    val reason = if (bestWindow.size >= 3) PopupSignalReason.BURST_LOOP_3_PLUS
                        else PopupSignalReason.BURST_LOOP_2
                    bestWindow.forEach { record(pkg, reason, it) }
                }
            }
        }

        activityEvents.mapNotNull { it.packageName }.distinct().forEach { pkg ->
            val metadata = signals(pkg)
            if (!metadata.isMetadataResolved || metadata.isSystemApp || isExcluded(pkg)) return@forEach
            val lastSeen = activityEvents.lastOrNull { it.packageName == pkg }?.timestampMs ?: nowMs
            if (!metadata.hasLauncherActivity) record(pkg, PopupSignalReason.NO_LAUNCHER_ICON, lastSeen)
            if (isRecentlyInstalled(metadata.firstInstallTimeMs, nowMs))
                record(pkg, PopupSignalReason.RECENT_INSTALL, lastSeen)
            if (metadata.hasOverlayPermission) record(pkg, PopupSignalReason.OVERLAY_PERMISSION, lastSeen)
        }

        return reasonsByPackage.mapNotNull { (pkg, reasons) ->
            val score = reasons.sumOf { it.points }.coerceIn(0, 100)
            val burst3 = PopupSignalReason.BURST_LOOP_3_PLUS in reasons
            if (score < 20) return@mapNotNull null
            PopupIncident(
                packageName = pkg,
                riskScore = score,
                status = if (burst3 || score >= 60) PopupRiskStatus.SUSPICIOUS else PopupRiskStatus.NEEDS_CHECK,
                reasons = reasons.toList().sortedByDescending { it.points },
                eventTimesMs = timesByPackage[pkg].orEmpty().toList().sorted(),
                isSimulated = isSimulated
            )
        }.sortedWith(
            compareByDescending<PopupIncident> { it.status == PopupRiskStatus.SUSPICIOUS }
                .thenByDescending { it.riskScore }.thenByDescending { it.lastSeenAtMs }
        )
    }

    private fun isRecentlyInstalled(firstInstallTimeMs: Long?, nowMs: Long): Boolean =
        firstInstallTimeMs != null && firstInstallTimeMs > nowMs - POPUP_RECENT_INSTALL_MS &&
            firstInstallTimeMs <= nowMs
}

internal fun addPopupSafePackage(current: Set<String>, packageName: String): Set<String> {
    val normalized = packageName.trim()
    return if (normalized.isEmpty()) current else current + normalized
}
internal fun restorePopupSafePackages(): Set<String> = emptySet()
internal fun capPopupIncidentArchive(incidents: List<PopupIncident>): List<PopupIncident> =
    incidents.takeLast(30)
internal fun withoutSimulatedPopupIncidents(incidents: List<PopupIncident>): List<PopupIncident> =
    incidents.filterNot { it.isSimulated }
