package com.superdenot.powerguard.domain.security

import com.superdenot.powerguard.ui.analytics.AppDisplayNameFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupHijackDetectorTest {
    private val now = 1_800_000_000_000L
    private val appA = "com.whatsapp"
    private val suspect = "com.example.suspect"
    private val browser = "com.android.chrome"

    private fun event(pkg: String?, at: Long, type: ScreenUsageEventType = ScreenUsageEventType.ACTIVITY_RESUMED) =
        ScreenUsageEvent(pkg, type, at)

    private fun pause(pkg: String, at: Long) =
        ScreenUsageEvent(pkg, ScreenUsageEventType.ACTIVITY_PAUSED, at)

    private fun resolved(
        pkg: String,
        launcher: Boolean = true,
        overlay: Boolean = false,
        installAt: Long? = now - 60L * 24L * 60L * 60L * 1000L,
        browser: Boolean = false,
        system: Boolean = false
    ) = AppPackageSignals(pkg, true, system, launcher, installAt, overlay, isBrowser = browser)

    @Test fun screenHijackAddsExpectedPoints() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 5_000), event(suspect, now - 2_000)),
            mapOf(appA to resolved(appA), suspect to resolved(suspect, overlay = true)),
            emptySet(), now
        )
        val incident = result.single { it.packageName == suspect }
        assertTrue(PopupSignalReason.SCREEN_HIJACKING in incident.reasons)
        assertEquals(50, incident.riskScore)
    }

    @Test fun browserRedirectScoresMiddlePackageNotBrowser() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 10_000), event(suspect, now - 4_000), event(browser, now - 1_500)),
            mapOf(suspect to resolved(suspect, overlay = true), browser to resolved(browser, browser = true)), emptySet(), now
        )
        assertEquals(suspect, result.single().packageName)
        assertEquals(95, result.single().riskScore)
        assertEquals(PopupRiskStatus.SUSPICIOUS, result.single().status)
        assertFalse(result.any { it.packageName == browser })
    }

    @Test fun postUnlockAmbushWithinThreeSecondsIsDetected() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(null, now - 2_000, ScreenUsageEventType.SCREEN_INTERACTIVE), event(suspect, now - 500)),
            mapOf(suspect to resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)),
            emptySet(), now
        )
        assertEquals(55, result.single().riskScore)
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in result.single().reasons)
    }

    @Test fun sandwichLoopRequiresOneToFifteenSeconds() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 10_000), event(suspect, now - 6_000), event(appA, now - 1_000)),
            mapOf(suspect to resolved(suspect, overlay = true)), emptySet(), now
        )
        assertEquals(70, result.single().riskScore)
        assertTrue(PopupSignalReason.SANDWICH_LOOP in result.single().reasons)
    }

    @Test fun twoInterruptionsAddTwentyFiveBurstPointsAndNeedCheck() {
        val incident = PopupHijackDetector.evaluateEvents(sandwichEvents(2, 10L * 60L * 1000L), emptyMap(), emptySet(), now).single()
        assertEquals(45, incident.riskScore)
        assertEquals(PopupRiskStatus.NEEDS_REVIEW, incident.status)
        assertTrue(PopupSignalReason.BURST_LOOP_2 in incident.reasons)
    }

    @Test fun threeInterruptionsAddFortyFiveBurstPointsAndAreSuspicious() {
        val incident = PopupHijackDetector.evaluateEvents(
            sandwichEvents(3, 5L * 60L * 1000L),
            mapOf(suspect to resolved(suspect, overlay = true)), emptySet(), now
        ).first()
        assertEquals(suspect, incident.packageName)
        assertEquals(PopupRiskStatus.SUSPICIOUS, incident.status)
        assertTrue(incident.riskScore >= 75)
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(3, incident.occurrenceCount)
    }

    @Test fun missingLauncherIconRequiresResolvedMetadata() {
        val events = listOf(event(suspect, now - 1_000))
        val resolvedResult = PopupHijackDetector.evaluateEvents(
            events, mapOf(suspect to resolved(suspect, launcher = false)), emptySet(), now
        )
        assertTrue(resolvedResult.isEmpty())
        val needsCheck = PopupHijackDetector.evaluateEvents(
            events, mapOf(suspect to resolved(suspect, launcher = false, overlay = true)), emptySet(), now
        )
        assertEquals(45, needsCheck.single().riskScore)
        val unresolved = resolved(suspect, launcher = false).copy(isMetadataResolved = false)
        assertTrue(PopupHijackDetector.evaluateEvents(events, mapOf(suspect to unresolved), emptySet(), now).isEmpty())
    }

    @Test fun recentInstallAndOverlayAreSeparateSignals() {
        val signal = resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)
        val incident = PopupHijackDetector.evaluateEvents(
            listOf(event(null, now - 2_000, ScreenUsageEventType.KEYGUARD_HIDDEN), event(suspect, now - 500)),
            mapOf(suspect to signal), emptySet(), now
        ).single()
        assertEquals(55, incident.riskScore)
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in incident.reasons)
        assertTrue(PopupSignalReason.RECENT_INSTALL in incident.reasons)
        assertTrue(PopupSignalReason.OVERLAY_PERMISSION in incident.reasons)
    }

    @Test fun chatBrowserReturnDoesNotAccuseBrowser() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 8_000), event(browser, now - 4_000), event(appA, now - 500)),
            mapOf(browser to resolved(browser, browser = true)), emptySet(), now
        )
        assertTrue(result.isEmpty())
    }

    @Test fun whitelistCanBeAddedAndRestored() {
        val safe = addPopupSafePackage(setOf("com.example.one"), "com.example.two")
        assertEquals(setOf("com.example.one", "com.example.two"), safe)
        assertTrue(restorePopupSafePackages().isEmpty())
        assertTrue(PopupHijackDetector.evaluateEvents(
            listOf(event(suspect, now - 1_000)),
            mapOf(suspect to resolved(suspect, launcher = false)), safe + suspect, now
        ).isEmpty())
    }

    @Test fun archiveRetainsOnlyLatestThirtyIncidents() {
        val incidents = (1..35).map { index ->
            PopupIncident("pkg.$index", 30, PopupRiskStatus.NEEDS_REVIEW,
                listOf(PopupSignalReason.NO_LAUNCHER_ICON), listOf(now + index))
        }
        val capped = capPopupIncidentArchive(incidents)
        assertEquals(30, capped.size)
        assertEquals("pkg.6", capped.first().packageName)
        assertEquals("pkg.35", capped.last().packageName)
    }

    @Test fun simulationCanBeEvaluatedAndCleared() {
        val simulated = PopupSimulationScenario.evaluate(now)
        assertTrue(simulated.isNotEmpty())
        assertTrue(simulated.all { it.isSimulated })
        assertTrue(withoutSimulatedPopupIncidents(simulated).isEmpty())
    }

    @Test fun keyguardHiddenIsAlsoAnUnlockSignal() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(null, now - 2_000, ScreenUsageEventType.KEYGUARD_HIDDEN), event(suspect, now - 500)),
            mapOf(suspect to resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)),
            emptySet(), now
        )
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in result.single().reasons)
    }

    @Test fun launcherBetweenAppsPreventsScreenHijackTransitionScore() {
        val launcher = "com.example.home"
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 8_000), event(launcher, now - 4_000), event(suspect, now - 1_000)),
            mapOf(
                launcher to AppPackageSignals(launcher, isHomeLauncher = true),
                suspect to resolved(suspect, overlay = true)
            ),
            emptySet(),
            now
        )
        assertFalse(result.any { PopupSignalReason.SCREEN_HIJACKING in it.reasons })
    }

    @Test fun burstAverageGapAtFifteenMinutesIsIncluded() {
        val incident = PopupHijackDetector.evaluateEvents(
            sandwichEvents(3, 15L * 60L * 1000L), emptyMap(), emptySet(), now
        ).single()
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(PopupRiskStatus.NEEDS_REVIEW, incident.status)
    }

    @Test fun launcherOpenedAppWithOnlyWeakInAppSignalsIsNotFlagged() {
        val launcher = "com.example.home"
        val recentInstall = now - 2L * 24L * 60L * 60L * 1000L
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(launcher, now - 5_000), event(suspect, now - 1_000)),
            mapOf(
                launcher to AppPackageSignals(launcher, isHomeLauncher = true),
                suspect to resolved(suspect, launcher = true, overlay = true, installAt = recentInstall)
            ),
            emptySet(),
            now
        )
        assertTrue(result.isEmpty())
    }


    @Test fun burstIntervalsOverFifteenMinutesNeverReceiveBurstReason() {
        listOf(44L, 73L, 466L).forEach { minutes ->
            val result = PopupHijackDetector.evaluateEvents(sandwichEvents(2, minutes * 60_000L), emptyMap(), emptySet(), now)
            assertTrue("Unexpected burst for $minutes minutes", result.none {
                PopupSignalReason.BURST_LOOP_2 in it.reasons || PopupSignalReason.BURST_LOOP_3_PLUS in it.reasons
            })
            assertTrue("Unexpected incident for $minutes minutes", result.isEmpty())
        }
    }

    @Test fun fourEventsAcrossExactlyFortyFiveMinutesAreValidBurst() {
        val incident = PopupHijackDetector.evaluateEvents(sandwichEvents(4, 15L * 60_000L), emptyMap(), emptySet(), now).single()
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(PopupRiskStatus.NEEDS_REVIEW, incident.status)
    }

    @Test fun ordinaryOldLauncherAppDoesNotReachRiskThresholdFromScreenSwitching() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 5_000), event(suspect, now - 2_000)),
            mapOf(appA to resolved(appA), suspect to resolved(suspect)), emptySet(), now
        )
        assertTrue(result.isEmpty())
    }

    @Test fun ordinaryOldLauncherAppWithUnlockAndSandwichSignalsStillStaysBelowVisibleRisk() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(
                event(appA, now - 10_000),
                event(null, now - 5_000, ScreenUsageEventType.SCREEN_INTERACTIVE),
                event(suspect, now - 3_000),
                event(appA, now - 1_000)
            ),
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(), now
        )
        assertTrue(result.isEmpty())
    }

    @Test fun explicitOemAndGooglePackagesAreAlwaysExcluded() {
        val excluded = listOf(
            "com.transsion.itel.launcher", "com.transsion.hilauncher", "com.transsion.XOSLauncher",
            "com.transsion.resolver", "com.transsion.screencapture", "com.transsion.camera",
            "com.transsion.applock", "com.transsion.filemanagerx", "com.google.android.providers.media.module",
            "com.google.android.documentsui", "com.android.documentsui", "com.google.android.gms",
            "com.android.vending", "com.google.android.googlequicksearchbox", "com.vendor.photopicker",
            "com.android.systemui", "com.android.settings", "com.android.chrome"
        )
        excluded.forEach { pkg ->
            val result = PopupHijackDetector.evaluateEvents(
                listOf(event(appA, now - 5_000), event(pkg, now - 2_000), event(appA, now - 1_000)),
                mapOf(pkg to resolved(pkg, launcher = false, overlay = true)), emptySet(), now
            )
            assertFalse("Should exclude $pkg", result.any { it.packageName == pkg })
        }
    }

    @Test fun systemPackageIsOnlyAllowedForLiteralThreeStepBrowserRedirect() {
        val systemPackage = "com.example.system.redirect"
        val result = PopupHijackDetector.evaluateEvents(
            listOf(event(appA, now - 10_000), event(systemPackage, now - 4_000), event(browser, now - 1_500)),
            mapOf(systemPackage to resolved(systemPackage, system = true, overlay = true),
                browser to resolved(browser, browser = true)), emptySet(), now
        )
        assertEquals(systemPackage, result.single().packageName)
        assertEquals(60, result.single().riskScore)
        assertTrue(PopupSignalReason.BROWSER_REDIRECT_CHAIN in result.single().reasons)
    }

    @Test fun archiveMigrationDropsExcludedSafeDuplicateAndInvalidBurstEntries() {
        fun old(pkg: String, times: List<Long>, reasons: List<PopupSignalReason>) = PopupIncident(
            pkg, reasons.sumOf { it.points }, PopupRiskStatus.SUSPICIOUS, reasons, times
        )
        val migrated = sanitizePopupIncidentArchive(listOf(
            old("com.example.safe", listOf(now), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
            old("com.transsion.itel.launcher", listOf(now), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
            old("com.example.invalidburst", listOf(now - 73L * 60_000L, now),
                listOf(PopupSignalReason.BURST_LOOP_2, PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
            old("com.example.duplicate", listOf(now - 2), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
            old("com.example.duplicate", listOf(now - 1), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL))
        ), setOf("com.example.safe"))
        assertEquals(listOf("com.example.duplicate"), migrated.map { it.packageName })
        assertEquals(now - 1, migrated.single().lastSeenAtMs)
        assertEquals(45, migrated.single().riskScore)
    }


    @Test fun archiveMigrationRetainsSystemPackageWithLiteralRedirectWithoutOverlayPermission() {
        val valid = PopupIncident(
            "com.example.system.redirect", 45, PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.BROWSER_REDIRECT_CHAIN, PopupSignalReason.NO_LAUNCHER_ICON),
            listOf(now),
            hasValidatedBrowserRedirectEvidence = true
        )
        val invalid = PopupIncident(
            "com.example.system.other", 45, PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL),
            listOf(now)
        )
        val result = sanitizePopupIncidentArchive(
            listOf(valid, invalid), emptySet(),
            setOf("com.example.system.redirect", "com.example.system.other")
        )
        assertEquals(listOf("com.example.system.redirect"), result.map { it.packageName })
    }

    @Test fun simulationContainsBothNewStatusBands() {
        val simulated = PopupSimulationScenario.evaluate(now)
        assertTrue(simulated.any { it.status == PopupRiskStatus.SUSPICIOUS && it.riskScore >= 75 })
        assertTrue(simulated.any { it.status == PopupRiskStatus.NEEDS_REVIEW && it.riskScore in 45..74 })
    }

    @Test
    fun internalActivityTransitionsUseLastPauseBeforeReturningToPreviousApp() {
        val events = listOf(
            event(appA, now - 20_000L),
            event(suspect, now - 19_000L),
            pause(suspect, now - 18_000L),
            event(suspect, now - 17_000L),
            pause(suspect, now - 14_000L),
            event(appA, now - 13_000L),
            event(suspect, now - 8_000L),
            pause(suspect, now - 6_000L),
            event(appA, now - 5_000L)
        )
        val incident = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(suspect to resolved(suspect, overlay = true)),
            emptySet(),
            now
        ).single { it.packageName == suspect }

        // The first pause is too early; the final pause is only 1 second before returning.
        assertEquals(listOf(5_000L, 2_000L), incident.eventDurationsMs.filter { it > 0L })
        assertTrue(PopupSignalReason.BURST_LOOP_2 in incident.reasons)
    }

    @Test
    fun establishedLauncherAppsWithNormalMultitaskingStayBelowVisibleRisk() {
        val home = "com.example.home"
        val packages = listOf("com.whatsapp", "com.github.android", "com.example.hrms")
        val events = mutableListOf<ScreenUsageEvent>()
        var timestamp = now - 20L * 60_000L
        events += event(home, timestamp)
        listOf(
            "com.whatsapp", "com.github.android", "com.example.hrms",
            "com.whatsapp", "com.github.android", "com.example.hrms"
        ).forEach { packageName ->
            val resumedAt = timestamp + 1_000L
            events += event(packageName, resumedAt)
            events += pause(packageName, resumedAt + 30_000L)
            timestamp = resumedAt + 60_000L
        }
        val signals = packages.associateWith { resolved(it) } +
            (home to AppPackageSignals(home, isHomeLauncher = true))
        val result = PopupHijackDetector.evaluateEvents(events, signals, emptySet(), now)
        assertTrue(result.none { it.packageName in packages && it.riskScore >= 45 })
    }

    @Test
    fun establishedLauncherAppNeedsThreeFastInterruptionsWithinThirtyMinutes() {
        val durations = listOf(1_000L, 3_000L, 6_000L)
        val events = mutableListOf<ScreenUsageEvent>()
        durations.forEachIndexed { index, duration ->
            val previousAt = now - (durations.size - index) * 5L * 60_000L
            val popupAt = previousAt + 1_000L
            events += event(appA, previousAt)
            events += event(suspect, popupAt)
            events += pause(suspect, popupAt + duration)
            events += event(appA, popupAt + duration + 100L)
        }
        val incident = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(),
            now
        ).single { it.packageName == suspect }
        assertTrue(incident.riskScore >= 45)
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(durations, incident.eventDurationsMs.filter { it > 0L })
    }

    @Test
    fun establishedLauncherAppWithThreeEightSecondInterruptionsIsNotFlagged() {
        val events = mutableListOf<ScreenUsageEvent>()
        repeat(3) { index ->
            val previousAt = now - (3 - index) * 5L * 60_000L
            val popupAt = previousAt + 1_000L
            events += event(appA, previousAt)
            events += event(suspect, popupAt)
            events += pause(suspect, popupAt + 8_000L)
            events += event(appA, popupAt + 8_100L)
        }
        val result = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(),
            now
        )
        assertFalse(result.any { it.packageName == suspect && it.riskScore >= 45 })
    }

    @Test
    fun establishedLauncherAppFastBurstIncludesExactThirtyMinuteAndTenMinuteBoundaries() {
        val events = mutableListOf<ScreenUsageEvent>()
        repeat(4) { index ->
            val popupAt = now - (3 - index) * 10L * 60_000L - 3_000L
            events += event(appA, popupAt - 1_000L)
            events += event(suspect, popupAt)
            events += pause(suspect, popupAt + 2_000L)
            events += event(appA, popupAt + 2_100L)
        }
        val incident = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(),
            now
        ).single { it.packageName == suspect }
        assertTrue(incident.riskScore >= 45)
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
    }

    @Test
    fun establishedLauncherAppFastBurstAboveTenMinuteAverageIsNotFlagged() {
        val events = mutableListOf<ScreenUsageEvent>()
        repeat(3) { index ->
            val popupAt = now - (2 - index) * (10L * 60_000L + 1_000L) - 3_000L
            events += event(appA, popupAt - 1_000L)
            events += event(suspect, popupAt)
            events += pause(suspect, popupAt + 2_000L)
            events += event(appA, popupAt + 2_100L)
        }
        val result = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(),
            now
        )
        assertFalse(result.any { it.packageName == suspect && it.riskScore >= 45 })
    }

    @Test
    fun unknownInstallAgeDoesNotUseEstablishedLauncherSuppression() {
        val events = mutableListOf<ScreenUsageEvent>()
        repeat(3) { index ->
            val previousAt = now - (3 - index) * 5L * 60_000L
            val popupAt = previousAt + 1_000L
            events += event(appA, previousAt)
            events += event(suspect, popupAt)
            events += pause(suspect, popupAt + 8_000L)
            events += event(appA, popupAt + 8_100L)
        }
        val result = PopupHijackDetector.evaluateEvents(
            events,
            mapOf(appA to resolved(appA), suspect to resolved(suspect, installAt = null)),
            emptySet(),
            now
        )
        assertTrue(result.any { it.packageName == suspect && it.riskScore >= 45 })
    }

    @Test
    fun establishedLauncherAppBrowserRedirectMeetsVisibleThreshold() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(
                event(appA, now - 10_000L),
                event(suspect, now - 4_000L),
                event(browser, now - 1_500L)
            ),
            mapOf(
                appA to resolved(appA),
                suspect to resolved(suspect),
                browser to resolved(browser, browser = true)
            ),
            emptySet(),
            now
        )
        val incident = result.single { it.packageName == suspect }
        assertEquals(45, incident.riskScore)
        assertEquals(PopupRiskStatus.NEEDS_REVIEW, incident.status)
        assertTrue(PopupSignalReason.BROWSER_REDIRECT_CHAIN in incident.reasons)
    }

    @Test
    fun archiveSanitizationRemovesLegacyNormalLauncherFalsePositives() {
        val normal = PopupIncident(
            "com.whatsapp",
            45,
            PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.SANDWICH_LOOP, PopupSignalReason.POST_UNLOCK_AMBUSH),
            listOf(now)
        )
        val legacyBurstWithoutDurations = PopupIncident(
            "com.github.android",
            45,
            PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.BURST_LOOP_3_PLUS),
            listOf(now - 10L * 60_000L, now - 5L * 60_000L, now)
        )
        val validFastBurst = PopupIncident(
            "com.example.hrms",
            45,
            PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.BURST_LOOP_3_PLUS),
            listOf(now - 10L * 60_000L, now - 5L * 60_000L, now),
            eventDurationsMs = listOf(1_000L, 4_000L, 6_000L)
        )
        val legacyRedirect = PopupIncident(
            "com.example.legacyredirect",
            45,
            PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.BROWSER_REDIRECT_CHAIN),
            listOf(now)
        )
        val validatedRedirect = PopupIncident(
            "com.example.validredirect",
            45,
            PopupRiskStatus.NEEDS_REVIEW,
            listOf(PopupSignalReason.BROWSER_REDIRECT_CHAIN),
            listOf(now),
            hasValidatedBrowserRedirectEvidence = true
        )
        val cleaned = sanitizePopupIncidentArchive(
            listOf(normal, legacyBurstWithoutDurations, validFastBurst, legacyRedirect, validatedRedirect),
            emptySet(),
            establishedLauncherPackages = setOf(
                "com.whatsapp", "com.github.android", "com.example.hrms",
                "com.example.legacyredirect", "com.example.validredirect"
            )
        )
        assertEquals(
            listOf("com.example.hrms", "com.example.validredirect"),
            cleaned.map { it.packageName }
        )
    }

    @Test
    fun githubAndroidPackageUsesGitHubFallbackLabel() {
        assertEquals("GitHub", AppDisplayNameFormatter.format("com.github.android", null))
        assertEquals("GitHub", AppDisplayNameFormatter.format("com.github.android", "Android"))
    }

    @Test
    fun browserToSuspectToBrowserIsNotAnAppToBrowserRedirectChain() {
        val result = PopupHijackDetector.evaluateEvents(
            listOf(
                event(browser, now - 10_000L),
                event(suspect, now - 4_000L),
                event(browser, now - 1_500L)
            ),
            mapOf(
                browser to resolved(browser, browser = true),
                suspect to resolved(suspect)
            ),
            emptySet(),
            now
        )
        assertFalse(result.any { it.packageName == suspect && it.riskScore >= 45 })
    }

    private fun sandwichEvents(count: Int, intervalMs: Long): List<ScreenUsageEvent> {
        val output = mutableListOf<ScreenUsageEvent>()
        repeat(count) { index ->
            val base = now - (count - index) * intervalMs
            output += event(appA, base)
            output += event(suspect, base + 2_000)
            output += event(appA, base + 7_000)
        }
        return output
    }
}
