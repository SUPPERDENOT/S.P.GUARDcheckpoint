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
    BROWSER_REDIRECT_CHAIN(45, "Muncul sebelum aplikasi membuka browser"),
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
    val isSimulated: Boolean = false,
    val eventDurationsMs: List<Long> = emptyList(),
    val hasValidatedBrowserRedirectEvidence: Boolean = false
) {
    val lastSeenAtMs: Long get() = eventTimesMs.maxOrNull() ?: 0L
    val occurrenceCount: Int get() = eventTimesMs.distinct().size
}
internal const val POPUP_LOOKBACK_MS = 24L * 60L * 60L * 1000L
internal const val POPUP_BURST_WINDOW_MS = 45L * 60L * 1000L
internal const val POPUP_MAX_AVERAGE_GAP_MS = 15L * 60L * 1000L
internal const val POPUP_FAST_BURST_WINDOW_MS = 30L * 60L * 1000L
internal const val POPUP_FAST_MAX_AVERAGE_GAP_MS = 10L * 60L * 1000L
internal const val POPUP_FAST_MIN_DURATION_MS = 1_000L
internal const val POPUP_FAST_MAX_DURATION_MS = 6_000L
internal const val POPUP_FAST_RETURN_GRACE_MS = 2_000L
internal const val POPUP_RECENT_INSTALL_MS = 14L * 24L * 60L * 60L * 1000L
internal const val POPUP_NEEDS_CHECK_THRESHOLD = 45
internal const val POPUP_SUSPICIOUS_THRESHOLD = 75
private data class PopupInterruptionEvidence(val timestampMs: Long, val durationMs: Long?)
private val POPUP_ALWAYS_EXCLUDED_PACKAGES = setOf(
    "com.transsion.itel.launcher", "com.transsion.hilauncher", "com.transsion.XOSLauncher",
    "com.transsion.resolver", "com.transsion.screencapture", "com.transsion.camera",
    "com.transsion.applock", "com.transsion.filemanagerx",
    "com.google.android.providers.media.module", "com.google.android.documentsui",
    "com.android.documentsui", "com.google.android.gms", "com.android.vending",
    "com.google.android.googlequicksearchbox",
    "com.android.systemui", "com.android.settings", "com.android.packageinstaller",
    "com.google.android.packageinstaller", "com.android.permissioncontroller",
    "com.google.android.permissioncontroller", "com.android.dialer", "com.google.android.dialer",
    "com.android.chrome", "com.brave.browser", "org.mozilla.firefox", "com.opera.browser",
    "com.sec.android.app.sbrowser", "com.microsoft.emmx", "com.kiwibrowser.browser"
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
                previous.packageName != packageName && !isLauncher(previous.packageName) &&
                !signals(previous.packageName!!).isBrowser &&
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
        val durationsByPackage = linkedMapOf<String, MutableMap<Long, Long>>()
        fun record(
            packageName: String,
            reason: PopupSignalReason,
            at: Long,
            allowSystemBrowserRedirect: Boolean = false,
            durationMs: Long? = null
        ) {
            if (isExcluded(packageName, allowSystemBrowserRedirect)) return
            reasonsByPackage.getOrPut(packageName) { linkedSetOf() } += reason
            timesByPackage.getOrPut(packageName) { linkedSetOf() } += at
            if (durationMs != null) durationsByPackage.getOrPut(packageName) { linkedMapOf() }[at] = durationMs
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

            if (literalBrowserRedirect) {
                record(packageName, PopupSignalReason.BROWSER_REDIRECT_CHAIN, current.timestampMs, allowSystemBrowserRedirect = true)
                if (metadata.isSystemApp && metadata.hasOverlayPermission) {
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
            val next = activityEvents.getOrNull(index + 1)
            val literalBrowserRedirect = isLiteralBrowserRedirect(index, pkg)
            val fastDurationMs = if (
                previous != null && next != null &&
                previous.packageName != pkg && next.packageName == previous.packageName &&
                !isLauncher(previous.packageName)
            ) {
                val pauseEvent = ordered.firstOrNull {
                    it.packageName == pkg &&
                        it.eventType == ScreenUsageEventType.ACTIVITY_PAUSED &&
                        it.timestampMs > event.timestampMs &&
                        it.timestampMs <= next.timestampMs
                }
                pauseEvent?.takeIf {
                    next.timestampMs - it.timestampMs in 0L..POPUP_FAST_RETURN_GRACE_MS
                }?.timestampMs?.minus(event.timestampMs)?.takeIf {
                    it in POPUP_FAST_MIN_DURATION_MS..POPUP_FAST_MAX_DURATION_MS
                }
            } else null

            if (previous == null || previous.packageName == pkg || isLauncher(previous.packageName) ||
                isExcluded(pkg, allowSystemBrowserRedirect = literalBrowserRedirect) ||
                (!hasSuspiciousStaticSignal(pkg) && reasonsByPackage[pkg].isNullOrEmpty() && fastDurationMs == null)
            ) null else pkg to PopupInterruptionEvidence(event.timestampMs, fastDurationMs)
        }.groupBy({ it.first }, { it.second })

        interruptions.forEach { (pkg, evidence) ->
            val orderedEvidence = evidence.distinctBy { it.timestampMs }.sortedBy { it.timestampMs }
            val metadata = signals(pkg)
            val establishedLauncherApp = isEstablishedLauncherApp(metadata, nowMs)
            var bestValidWindow = emptyList<PopupInterruptionEvidence>()
            for (end in orderedEvidence.indices) {
                var start = end
                while (start >= 0) {
                    val window = orderedEvidence.subList(start, end + 1)
                    val spanMs = window.last().timestampMs - window.first().timestampMs
                    val averageGapMs = if (window.size > 1) {
                        spanMs.toDouble() / (window.size - 1)
                    } else {
                        Double.MAX_VALUE
                    }
                    val qualifies = if (establishedLauncherApp) {
                        window.size >= 3 &&
                            spanMs <= POPUP_FAST_BURST_WINDOW_MS &&
                            averageGapMs <= POPUP_FAST_MAX_AVERAGE_GAP_MS &&
                            window.all {
                                it.durationMs != null &&
                                    it.durationMs in POPUP_FAST_MIN_DURATION_MS..POPUP_FAST_MAX_DURATION_MS
                            }
                    } else {
                        window.size >= 2 &&
                            spanMs <= POPUP_BURST_WINDOW_MS &&
                            averageGapMs <= POPUP_MAX_AVERAGE_GAP_MS
                    }
                    if (qualifies && window.size > bestValidWindow.size) bestValidWindow = window.toList()
                    if (spanMs > if (establishedLauncherApp) POPUP_FAST_BURST_WINDOW_MS else POPUP_BURST_WINDOW_MS) break
                    start--
                }
            }
            if (bestValidWindow.isNotEmpty()) {
                val reason = if (bestValidWindow.size >= 3) PopupSignalReason.BURST_LOOP_3_PLUS
                    else PopupSignalReason.BURST_LOOP_2
                bestValidWindow.forEach {
                    record(pkg, reason, it.timestampMs, durationMs = it.durationMs)
                }
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
            val metadata = signals(pkg)
            val establishedLauncherApp = isEstablishedLauncherApp(metadata, nowMs)
            val hasStrongPattern = PopupSignalReason.BROWSER_REDIRECT_CHAIN in reasons ||
                if (establishedLauncherApp) {
                    PopupSignalReason.BURST_LOOP_3_PLUS in reasons
                } else {
                    PopupSignalReason.BURST_LOOP_2 in reasons ||
                        PopupSignalReason.BURST_LOOP_3_PLUS in reasons
                }
            if (establishedLauncherApp && !hasStrongPattern) return@mapNotNull null

            val score = reasons.sumOf { it.points }.coerceIn(0, 100)
            if (score < POPUP_NEEDS_CHECK_THRESHOLD) return@mapNotNull null
            val incidentTimes = timesByPackage[pkg].orEmpty().toList().sorted()
            val incidentDurations = incidentTimes.map { durationsByPackage[pkg]?.get(it) ?: 0L }
            PopupIncident(
                packageName = pkg,
                riskScore = score,
                status = if (score >= POPUP_SUSPICIOUS_THRESHOLD) PopupRiskStatus.SUSPICIOUS else PopupRiskStatus.NEEDS_CHECK,
                reasons = reasons.toList().sortedByDescending { it.points },
                eventTimesMs = incidentTimes,
                isSimulated = isSimulated,
                eventDurationsMs = incidentDurations,
                hasValidatedBrowserRedirectEvidence =
                    PopupSignalReason.BROWSER_REDIRECT_CHAIN in reasons
            )
        }.sortedWith(
            compareByDescending<PopupIncident> { it.status == PopupRiskStatus.SUSPICIOUS }
                .thenByDescending { it.riskScore }.thenByDescending { it.lastSeenAtMs }
        )
    }

    private fun isEstablishedLauncherApp(metadata: AppPackageSignals, nowMs: Long): Boolean =
        metadata.isMetadataResolved &&
            metadata.hasLauncherActivity &&
            !metadata.isSystemApp &&
            !metadata.hasOverlayPermission &&
            metadata.firstInstallTimeMs?.let {
                it > 0L && it <= nowMs - POPUP_RECENT_INSTALL_MS
            } == true

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
    safePackages: Set<String>,
    systemPackages: Set<String> = emptySet(),
    establishedLauncherPackages: Set<String> = emptySet()
): List<PopupIncident> {
    val cleaned = incidents.mapNotNull { incident ->
        if (incident.packageName in safePackages || isPopupAlwaysExcludedPackage(incident.packageName)) return@mapNotNull null
        val reasons = incident.reasons.toMutableSet()
        val hasValidatedBrowserRedirectEvidence =
            incident.hasValidatedBrowserRedirectEvidence &&
                PopupSignalReason.BROWSER_REDIRECT_CHAIN in reasons
        if (PopupSignalReason.BROWSER_REDIRECT_CHAIN in reasons && !hasValidatedBrowserRedirectEvidence) {
            reasons.remove(PopupSignalReason.BROWSER_REDIRECT_CHAIN)
        }
        if (incident.packageName in systemPackages && !hasValidatedBrowserRedirectEvidence) return@mapNotNull null

        val times = incident.eventTimesMs.distinct().sorted()
        val durationByTime = incident.eventTimesMs.zip(incident.eventDurationsMs).toMap()
        val fastTimes = times.filter { time ->
            (durationByTime[time] ?: 0L) in POPUP_FAST_MIN_DURATION_MS..POPUP_FAST_MAX_DURATION_MS
        }
        val validFastBurst = fastTimes.size >= 3 &&
            fastTimes.last() - fastTimes.first() <= POPUP_FAST_BURST_WINDOW_MS &&
            (fastTimes.last() - fastTimes.first()).toDouble() / (fastTimes.size - 1) <= POPUP_FAST_MAX_AVERAGE_GAP_MS
        val isEstablishedLauncher = incident.packageName in establishedLauncherPackages
        val burstReasons = setOf(PopupSignalReason.BURST_LOOP_2, PopupSignalReason.BURST_LOOP_3_PLUS)

        if (isEstablishedLauncher) {
            if (!hasValidatedBrowserRedirectEvidence &&
                !(PopupSignalReason.BURST_LOOP_3_PLUS in reasons && validFastBurst)
            ) return@mapNotNull null
            if (reasons.any { it in burstReasons } && !validFastBurst) {
                reasons.remove(PopupSignalReason.BURST_LOOP_2)
                reasons.remove(PopupSignalReason.BURST_LOOP_3_PLUS)
            }
        } else if (reasons.any { it in burstReasons }) {
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
            reasons = reasons.toList().sortedByDescending { it.points },
            hasValidatedBrowserRedirectEvidence = hasValidatedBrowserRedirectEvidence
        )
    }
    return cleaned.groupBy { it.packageName }
        .mapNotNull { (_, duplicates) -> duplicates.maxByOrNull { it.lastSeenAtMs } }
        .sortedBy { it.lastSeenAtMs }
}
