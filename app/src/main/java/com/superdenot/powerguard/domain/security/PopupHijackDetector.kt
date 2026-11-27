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
    BURST_LOOP_2(25, "Siklus berulang dalam 45 menit"),
    BURST_LOOP_3_PLUS(45, "Siklus berulang dalam 45 menit"),
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
internal const val POPUP_NEEDS_CHECK_THRESHOLD = 45
internal const val POPUP_SUSPICIOUS_THRESHOLD = 75
private val POPUP_ALWAYS_EXCLUDED_PACKAGES = setOf(
    "com.transsion.itel.launcher", "com.transsion.hilauncher", "com.transsion.XOSLauncher",
    "com.transsion.resolver", "com.transsion.screencapture", "com.transsion.camera",
    "com.transsion.applock", "com.transsion.filemanagerx",
    "com.google.android.providers.media.module", "com.google.android.documentsui",
    "com.android.documentsui", "com.google.android.gms", "com.android.vending",
    "com.google.android.googlequicksearchbox"
)

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
        fun isExplicitlyExcluded(packageName: String) =
            packageName in POPUP_ALWAYS_EXCLUDED_PACKAGES || packageName.contains("photopicker", ignoreCase = true)
        fun isExcluded(packageName: String, allowSystemBrowserRedirect: Boolean = false): Boolean {
            val metadata = signals(packageName)
            return packageName in safePackages || metadata.isExcluded || isExplicitlyExcluded(packageName) ||
                (metadata.isSystemApp && !allowSystemBrowserRedirect)
        }
        fun isLauncher(packageName: String?) = packageName != null && signals(packageName).isHomeLauncher
        fun isLiteralBrowserRedirect(index: Int, packageName: String): Boolean {
            val previous = activityEvents.getOrNull(index - 1)
            val current = activityEvents.getOrNull(index)
            val next = activityEvents.getOrNull(index + 1)
            return current?.packageName == packageName && previous?.packageName != null &&
                previous.packageName != packageName && !signals(previous.packageName!!).isBrowser &&
                next?.packageName != null &&
                signals(next.packageName!!).isBrowser &&
                next.timestampMs - current.timestampMs in 0 until 5_000L
        }
        fun hasSuspiciousStaticSignal(packageName: String): Boolean {
            val metadata = signals(packageName)
            return metadata.isMetadataResolved && !metadata.isSystemApp &&
                (!metadata.hasLauncherActivity || metadata.hasOverlayPermission ||
                    isRecentlyInstalled(metadata.firstInstallTimeMs, nowMs))
        }

        val reasonsByPackage = linkedMapOf<String, MutableSet<PopupSignalReason>>()
        val timesByPackage = linkedMapOf<String, MutableSet<Long>>()
        fun record(packageName: String, reason: PopupSignalReason, at: Long, allowSystemBrowserRedirect: Boolean = false) {
            if (isExcluded(packageName, allowSystemBrowserRedirect)) return
            reasonsByPackage.getOrPut(packageName) { linkedSetOf() } += reason
            timesByPackage.getOrPut(packageName) { linkedSetOf() } += at
        }

        activityEvents.forEachIndexed { index, current ->
            val packageName = current.packageName ?: return@forEachIndexed
            val literalBrowserRedirect = isLiteralBrowserRedirect(index, packageName)
            if (isExcluded(packageName, allowSystemBrowserRedirect = literalBrowserRedirect)) return@forEachIndexed
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
            ) {
                record(packageName, PopupSignalReason.BROWSER_REDIRECT_CHAIN, current.timestampMs, allowSystemBrowserRedirect = true)
                if (literalBrowserRedirect && metadata.isSystemApp && metadata.hasOverlayPermission) {
                    record(packageName, PopupSignalReason.OVERLAY_PERMISSION, current.timestampMs, allowSystemBrowserRedirect = true)
                }
            }

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
            val literalBrowserRedirect = isLiteralBrowserRedirect(index, pkg)
            if (previous == null || previous.packageName == pkg || isLauncher(previous.packageName) ||
                isExcluded(pkg, allowSystemBrowserRedirect = literalBrowserRedirect) ||
                (!hasSuspiciousStaticSignal(pkg) && reasonsByPackage[pkg].isNullOrEmpty())
            ) null else pkg to event.timestampMs
        }.groupBy({ it.first }, { it.second })

        interruptions.forEach { (pkg, rawTimes) ->
            val times = rawTimes.distinct().sorted()
            var bestValidWindow = emptyList<Long>()
            for (end in times.indices) {
                var start = end
                while (start >= 0 && times[end] - times[start] <= POPUP_BURST_WINDOW_MS) {
                    val window = times.subList(start, end + 1)
                    if (window.size >= 2) {
                        val totalGap = window.last() - window.first()
                        val averageGap = totalGap.toDouble() / (window.size - 1)
                        if (averageGap <= POPUP_MAX_AVERAGE_GAP_MS && window.size > bestValidWindow.size) {
                            bestValidWindow = window.toList()
                        }
                    }
                    start--
                }
            }
            if (bestValidWindow.size >= 2) {
                val reason = if (bestValidWindow.size >= 3) PopupSignalReason.BURST_LOOP_3_PLUS
                    else PopupSignalReason.BURST_LOOP_2
                bestValidWindow.forEach { record(pkg, reason, it) }
            }
        }

        activityEvents.mapNotNull { it.packageName }.distinct().forEach { pkg ->
            val metadata = signals(pkg)
            if (!metadata.isMetadataResolved || metadata.isSystemApp || isExcluded(pkg)) return@forEach
            val packageIndex = activityEvents.indexOfFirst { it.packageName == pkg }
            val previousPackage = activityEvents.getOrNull(packageIndex - 1)?.packageName
            val launchedDirectlyFromHome = isLauncher(previousPackage)
            val hasBehavioralEvidence = reasonsByPackage[pkg].orEmpty().any { reason ->
                reason == PopupSignalReason.SCREEN_HIJACKING ||
                    reason == PopupSignalReason.BROWSER_REDIRECT_CHAIN ||
                    reason == PopupSignalReason.POST_UNLOCK_AMBUSH ||
                    reason == PopupSignalReason.SANDWICH_LOOP ||
                    reason == PopupSignalReason.BURST_LOOP_2 ||
                    reason == PopupSignalReason.BURST_LOOP_3_PLUS
            }
            val lastSeen = activityEvents.lastOrNull { it.packageName == pkg }?.timestampMs ?: nowMs
            if (!metadata.hasLauncherActivity) record(pkg, PopupSignalReason.NO_LAUNCHER_ICON, lastSeen)
            if (launchedDirectlyFromHome && metadata.hasLauncherActivity && !hasBehavioralEvidence) {
                return@forEach
            }
            if (isRecentlyInstalled(metadata.firstInstallTimeMs, nowMs)) {
                record(pkg, PopupSignalReason.RECENT_INSTALL, lastSeen)
            }
            if (metadata.hasOverlayPermission) record(pkg, PopupSignalReason.OVERLAY_PERMISSION, lastSeen)
        }

        return reasonsByPackage.mapNotNull { (pkg, reasons) ->
            val score = reasons.sumOf { it.points }.coerceIn(0, 100)
            if (score < POPUP_NEEDS_CHECK_THRESHOLD) return@mapNotNull null
            PopupIncident(
                packageName = pkg,
                riskScore = score,
                status = if (score >= POPUP_SUSPICIOUS_THRESHOLD) PopupRiskStatus.SUSPICIOUS else PopupRiskStatus.NEEDS_CHECK,
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


internal fun isPopupAlwaysExcludedPackage(packageName: String): Boolean =
    packageName in POPUP_ALWAYS_EXCLUDED_PACKAGES || packageName.contains("photopicker", ignoreCase = true)

internal fun sanitizePopupIncidentArchive(
    incidents: List<PopupIncident>,
    safePackages: Set<String>
): List<PopupIncident> {
    val cleaned = incidents.mapNotNull { incident ->
        if (incident.packageName in safePackages || isPopupAlwaysExcludedPackage(incident.packageName)) return@mapNotNull null
        val reasons = incident.reasons.toMutableSet()
        val burstReasons = setOf(PopupSignalReason.BURST_LOOP_2, PopupSignalReason.BURST_LOOP_3_PLUS)
        if (reasons.any { it in burstReasons }) {
            val times = incident.eventTimesMs.distinct().sorted()
            val validBurst = times.size >= 2 &&
                times.last() - times.first() <= POPUP_BURST_WINDOW_MS &&
                (times.last() - times.first()).toDouble() / (times.size - 1) <= POPUP_MAX_AVERAGE_GAP_MS
            if (!validBurst) return@mapNotNull null
        }
        val score = reasons.sumOf { it.points }.coerceIn(0, 100)
        if (score < POPUP_NEEDS_CHECK_THRESHOLD) return@mapNotNull null
        incident.copy(
            riskScore = score,
            status = if (score >= POPUP_SUSPICIOUS_THRESHOLD) PopupRiskStatus.SUSPICIOUS else PopupRiskStatus.NEEDS_CHECK,
            reasons = reasons.toList().sortedByDescending { it.points }
        )
    }
    return cleaned.groupBy { it.packageName }
        .mapNotNull { (_, duplicates) -> duplicates.maxByOrNull { it.lastSeenAtMs } }
        .sortedBy { it.lastSeenAtMs }
}
