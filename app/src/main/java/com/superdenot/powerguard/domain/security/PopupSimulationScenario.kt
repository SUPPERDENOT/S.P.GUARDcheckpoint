package com.superdenot.powerguard.domain.security

internal object PopupSimulationScenario {
    fun evaluate(nowMs: Long): List<PopupIncident> {
        val whatsapp = "com.whatsapp"
        val popup = "com.superd.simulated.popup"
        val redirect = "com.superd.simulated.redirect"
        val review = "com.superd.simulated.review"
        val home = "com.superd.simulated.home"
        val chrome = "com.android.chrome"
        val eightMinutes = 8L * 60L * 1000L
        val events = mutableListOf<ScreenUsageEvent>()
        val base = nowMs - 22L * 60L * 1000L
        repeat(3) { index ->
            val at = base + index * eightMinutes
            events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, at)
            events += ScreenUsageEvent(popup, ScreenUsageEventType.ACTIVITY_RESUMED, at + 2_000L)
            events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, at + 7_000L)
        }
        events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, nowMs - 60_000L)
        events += ScreenUsageEvent(redirect, ScreenUsageEventType.ACTIVITY_RESUMED, nowMs - 59_000L)
        events += ScreenUsageEvent(chrome, ScreenUsageEventType.ACTIVITY_RESUMED, nowMs - 57_000L)
        events += ScreenUsageEvent(home, ScreenUsageEventType.ACTIVITY_RESUMED, nowMs - 3_000L)
        events += ScreenUsageEvent(null, ScreenUsageEventType.SCREEN_INTERACTIVE, nowMs - 2_000L)
        events += ScreenUsageEvent(review, ScreenUsageEventType.ACTIVITY_RESUMED, nowMs - 500L)

        val signals = mapOf(
            popup to AppPackageSignals(
                packageName = popup, isMetadataResolved = true, hasLauncherActivity = false,
                firstInstallTimeMs = nowMs - 2L * 24L * 60L * 60L * 1000L,
                hasOverlayPermission = true
            ),
            redirect to AppPackageSignals(
                packageName = redirect, isMetadataResolved = true, hasLauncherActivity = false,
                firstInstallTimeMs = nowMs - 1L * 24L * 60L * 60L * 1000L,
                hasOverlayPermission = true
            ),
            home to AppPackageSignals(packageName = home, isHomeLauncher = true),
            review to AppPackageSignals(
                packageName = review, isMetadataResolved = true, hasLauncherActivity = true,
                firstInstallTimeMs = nowMs - 2L * 24L * 60L * 60L * 1000L,
                hasOverlayPermission = true
            ),
            chrome to AppPackageSignals(packageName = chrome, isBrowser = true)
        )
        return PopupHijackDetector.evaluateEvents(events, signals, emptySet(), nowMs, isSimulated = true)
    }
}
