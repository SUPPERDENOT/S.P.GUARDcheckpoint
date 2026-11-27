package com.superdenot.powerguard.domain.security

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

    private fun resolved(
        pkg: String,
        launcher: Boolean = true,
        overlay: Boolean = false,
        installAt: Long? = now - 60L * 24L * 60L * 60L * 1000L,
        browser: Boolean = false,
        system: Boolean = false
    ) = AppPackageSignals(pkg, true, system, launcher, installAt, overlay, isBrowser = browser)

    @Test fun screenHijackAddsExpectedPoints() {
        val result = PopupHijackDetector.evaluate(
            listOf(event(appA, now - 5_000), event(suspect, now - 2_000)),
            mapOf(appA to resolved(appA), suspect to resolved(suspect, overlay = true)),
            emptySet(), now
        )
        val incident = result.single { it.packageName == suspect }
        assertTrue(PopupSignalReason.SCREEN_HIJACKING in incident.reasons)
        assertEquals(50, incident.riskScore)
    }

    @Test fun browserRedirectScoresMiddlePackageNotBrowser() {
        val result = PopupHijackDetector.evaluate(
            listOf(event(appA, now - 10_000), event(suspect, now - 4_000), event(browser, now - 1_500)),
            mapOf(
                suspect to resolved(suspect, overlay = true),
                browser to resolved(browser, browser = true)
            ), emptySet(), now
        )
        assertEquals(suspect, result.single().packageName)
        assertEquals(80, result.single().riskScore)
        assertEquals(PopupRiskStatus.SUSPICIOUS, result.single().status)
        assertFalse(result.any { it.packageName == browser })
    }

    @Test fun postUnlockAmbushWithinThreeSecondsIsDetected() {
        val result = PopupHijackDetector.evaluate(
            listOf(
                event(null, now - 2_000, ScreenUsageEventType.SCREEN_INTERACTIVE),
                event(suspect, now - 500)
            ), mapOf(suspect to resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)),
            emptySet(), now
        )
        assertEquals(55, result.single().riskScore)
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in result.single().reasons)
    }

    @Test fun sandwichLoopRequiresOneToFifteenSeconds() {
        val result = PopupHijackDetector.evaluate(
            listOf(event(appA, now - 10_000), event(suspect, now - 6_000), event(appA, now - 1_000)),
            mapOf(suspect to resolved(suspect, overlay = true)), emptySet(), now
        )
        assertEquals(70, result.single().riskScore)
        assertTrue(PopupSignalReason.SANDWICH_LOOP in result.single().reasons)
    }

    @Test fun twoInterruptionsAddTwentyFiveBurstPointsAndNeedCheck() {
        val incident = PopupHijackDetector.evaluate(sandwichEvents(2, 10L * 60L * 1000L), emptyMap(), emptySet(), now).single()
        assertEquals(45, incident.riskScore)
        assertEquals(PopupRiskStatus.NEEDS_CHECK, incident.status)
        assertTrue(PopupSignalReason.BURST_LOOP_2 in incident.reasons)
    }

    @Test fun threeInterruptionsAddFortyFiveBurstPointsAndAreSuspicious() {
        val incident = PopupHijackDetector.evaluate(
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
        val resolvedResult = PopupHijackDetector.evaluate(
            events, mapOf(suspect to resolved(suspect, launcher = false)), emptySet(), now
        )
        assertTrue(resolvedResult.isEmpty())
        val needsCheck = PopupHijackDetector.evaluate(
            events, mapOf(suspect to resolved(suspect, launcher = false, overlay = true)), emptySet(), now
        )
        assertEquals(45, needsCheck.single().riskScore)
        val unresolved = resolved(suspect, launcher = false).copy(isMetadataResolved = false)
        assertTrue(PopupHijackDetector.evaluate(events, mapOf(suspect to unresolved), emptySet(), now).isEmpty())
    }

    @Test fun recentInstallAndOverlayAreSeparateSignals() {
        val signal = resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)
        val incident = PopupHijackDetector.evaluate(
            listOf(
                event(null, now - 2_000, ScreenUsageEventType.KEYGUARD_HIDDEN),
                event(suspect, now - 500)
            ), mapOf(suspect to signal), emptySet(), now
        ).single()
        assertEquals(55, incident.riskScore)
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in incident.reasons)
        assertTrue(PopupSignalReason.RECENT_INSTALL in incident.reasons)
        assertTrue(PopupSignalReason.OVERLAY_PERMISSION in incident.reasons)
    }

    @Test fun chatBrowserReturnDoesNotAccuseBrowser() {
        val result = PopupHijackDetector.evaluate(
            listOf(event(appA, now - 8_000), event(browser, now - 4_000), event(appA, now - 500)),
            mapOf(browser to resolved(browser, browser = true)), emptySet(), now
        )
        assertTrue(result.isEmpty())
    }

    @Test fun whitelistCanBeAddedAndRestored() {
        val safe = addPopupSafePackage(setOf("com.example.one"), "com.example.two")
        assertEquals(setOf("com.example.one", "com.example.two"), safe)
        assertTrue(restorePopupSafePackages().isEmpty())
        assertTrue(PopupHijackDetector.evaluate(
            listOf(event(suspect, now - 1_000)),
            mapOf(suspect to resolved(suspect, launcher = false)), safe + suspect, now
        ).isEmpty())
    }

    @Test fun archiveRetainsOnlyLatestThirtyIncidents() {
        val incidents = (1..35).map { index ->
            PopupIncident("pkg.$index", 30, PopupRiskStatus.NEEDS_CHECK,
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
        val result = PopupHijackDetector.evaluate(
            listOf(
                event(null, now - 2_000, ScreenUsageEventType.KEYGUARD_HIDDEN),
                event(suspect, now - 500)
            ), mapOf(suspect to resolved(suspect, overlay = true, installAt = now - 2L * 24L * 60L * 60L * 1000L)),
            emptySet(), now
        )
        assertTrue(PopupSignalReason.POST_UNLOCK_AMBUSH in result.single().reasons)
    }

    @Test fun launcherBetweenAppsPreventsScreenHijackTransitionScore() {
        val launcher = "com.example.home"
        val result = PopupHijackDetector.evaluate(
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
        val incident = PopupHijackDetector.evaluate(
            sandwichEvents(3, 15L * 60L * 1000L), emptyMap(), emptySet(), now
        ).single()
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(PopupRiskStatus.NEEDS_CHECK, incident.status)
    }

    @Test fun launcherOpenedAppWithOnlyWeakInAppSignalsIsNotFlagged() {
        val launcher = "com.example.home"
        val recentInstall = now - 2L * 24L * 60L * 60L * 1000L
        val result = PopupHijackDetector.evaluate(
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
            val result = PopupHijackDetector.evaluate(
                sandwichEvents(2, minutes * 60_000L), emptyMap(), emptySet(), now
            )
            assertTrue("Unexpected burst for $minutes minutes", result.none {
                PopupSignalReason.BURST_LOOP_2 in it.reasons || PopupSignalReason.BURST_LOOP_3_PLUS in it.reasons
            })
            assertTrue("Unexpected incident for $minutes minutes", result.isEmpty())
        }
    }

    @Test fun fourEventsAcrossExactlyFortyFiveMinutesAreValidBurst() {
        val incident = PopupHijackDetector.evaluate(
            sandwichEvents(4, 15L * 60_000L), emptyMap(), emptySet(), now
        ).single()
        assertTrue(PopupSignalReason.BURST_LOOP_3_PLUS in incident.reasons)
        assertEquals(PopupRiskStatus.NEEDS_CHECK, incident.status)
    }

    @Test fun ordinaryOldLauncherAppDoesNotReachRiskThresholdFromScreenSwitching() {
        val result = PopupHijackDetector.evaluate(
            listOf(event(appA, now - 5_000), event(suspect, now - 2_000)),
            mapOf(appA to resolved(appA), suspect to resolved(suspect)),
            emptySet(), now
        )
        assertTrue(result.isEmpty())
    }

    @Test fun explicitOemAndGooglePackagesAreAlwaysExcluded() {
        val excluded = listOf(
            "com.transsion.itel.launcher", "com.transsion.hilauncher", "com.transsion.XOSLauncher",
            "com.transsion.resolver", "com.transsion.screencapture", "com.transsion.camera",
            "com.transsion.applock", "com.transsion.filemanagerx",
            "com.google.android.providers.media.module", "com.google.android.documentsui",
            "com.android.documentsui", "com.google.android.gms", "com.android.vending",
            "com.google.android.googlequicksearchbox", "com.vendor.photopicker"
        )
        excluded.forEach { pkg ->
            val result = PopupHijackDetector.evaluate(
                listOf(event(appA, now - 5_000), event(pkg, now - 2_000), event(appA, now - 1_000)),
                mapOf(pkg to resolved(pkg, launcher = false, overlay = true)), emptySet(), now
            )
            assertFalse("Should exclude $pkg", result.any { it.packageName == pkg })
        }
    }

    @Test fun systemPackageIsOnlyAllowedForLiteralThreeStepBrowserRedirect() {
        val systemPackage = "com.example.system.redirect"
        val events = listOf(
            event(appA, now - 10_000),
            event(systemPackage, now - 4_000),
            event(browser, now - 1_500)
        )
        val result = PopupHijackDetector.evaluate(
            events,
            mapOf(
                systemPackage to resolved(systemPackage, system = true),
                browser to resolved(browser, browser = true)
            ),
            emptySet(), now
        )
        assertEquals(systemPackage, result.single().packageName)
        assertEquals(listOf(PopupSignalReason.BROWSER_REDIRECT_CHAIN), result.single().reasons)
    }

    @Test fun archiveMigrationRemovesExcludedSafeDuplicateAndInvalidBurstEntries() {
        fun archived(pkg: String, times: List<Long>, reasons: List<PopupSignalReason>) = PopupIncident(
            pkg, reasons.sumOf { it.points }, PopupRiskStatus.SUSPICIOUS, reasons, times
        )
        val migrated = sanitizePopupIncidentArchive(
            listOf(
                archived("com.example.safe", listOf(now), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
                archived("com.transsion.itel.launcher", listOf(now), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
                archived("com.example.invalidburst", listOf(now - 73L * 60_000L, now), listOf(PopupSignalReason.BURST_LOOP_2, PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
                archived("com.example.duplicate", listOf(now - 2), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)),
                archived("com.example.duplicate", listOf(now - 1), listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL))
            ),
            setOf("com.example.safe")
        )
        assertEquals(listOf("com.example.duplicate"), migrated.map { it.packageName })
        assertEquals(now - 1, migrated.single().lastSeenAtMs)
        assertEquals(45, migrated.single().riskScore)
    }

    @Test fun simulationContainsBothNewStatusBands() {
        val simulated = PopupSimulationScenario.evaluate(now)
        assertTrue(simulated.any { it.status == PopupRiskStatus.SUSPICIOUS && it.riskScore >= 75 })
        assertTrue(simulated.any { it.status == PopupRiskStatus.NEEDS_CHECK && it.riskScore in 45..74 })
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
