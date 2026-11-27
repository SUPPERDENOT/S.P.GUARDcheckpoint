package com.superdenot.powerguard.domain.security

internal object PopupSimulationScenario {
    fun evaluate(nowMs: Long): List<PopupIncident> {
        val whatsapp = "com.whatsapp"
        val popup = "com.superd.simulated.popup"
        val redirect = "com.superd.simulated.redirect"
        val chrome = "com.android.chrome"
        val twoMinutes = 2L * 60L * 1000L
        val events = mutableListOf<ScreenUsageEvent>()
        val base = nowMs - 22L * 60L * 1000L
        repeat(3) { index ->
            val at = base + index * twoMinutes * 4
            events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, at)
            events += ScreenUsageEvent(popup, ScreenUsageEventType.ACTIVITY_RESUMED, at + 2_000)
            events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, at + 7_000)
        }
        val redirectAt = nowMs - 60_000L
        events += ScreenUsageEvent(whatsapp, ScreenUsageEventType.ACTIVITY_RESUMED, redirectAt)
        events += ScreenUsageEvent(redirect, ScreenUsageEventType.ACTIVITY_RESUMED, redirectAt + 1_000)
        events += ScreenUsageEvent(chrome, ScreenUsageEventType.ACTIVITY_RESUMED, redirectAt + 3_000)
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
            chrome to AppPackageSignals(packageName = chrome, isBrowser = true)
        )
        return PopupHijackDetector.evaluate(events, signals, emptySet(), nowMs, isSimulated = true)
    }
}
