package com.superdenot.powerguard.data.local

import android.content.Context
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class PopupDetectorPreferencesTest {
    private lateinit var context: Context

    @Before
    fun clearPreferences() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("spg_popup_detector", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun safePackageSurvivesRecreationAndRestoreClearsManualEntries() {
        val first = PopupDetectorPreferences(context)
        assertTrue(first.markSafe("com.example.safe"))
        assertEquals(setOf("com.example.safe"), PopupDetectorPreferences(context).safePackages())

        assertTrue(PopupDetectorPreferences(context).restoreSafePackages())
        assertTrue(PopupDetectorPreferences(context).safePackages().isEmpty())
    }

    @Test
    fun archivePersistsJsonAndKeepsOnlyLatestThirtyIncidents() {
        val now = 1_800_000_000_000L
        val incidents = (1..35).map { index ->
            PopupIncident(
                packageName = "com.example.app$index",
                riskScore = 35,
                status = PopupRiskStatus.NEEDS_CHECK,
                reasons = listOf(PopupSignalReason.SCREEN_HIJACKING),
                eventTimesMs = listOf(now + index)
            )
        }
        assertTrue(PopupDetectorPreferences(context).appendIncidents(incidents))

        val restored = PopupDetectorPreferences(context).archivedIncidents()
        assertEquals(30, restored.size)
        assertEquals("com.example.app6", restored.first().packageName)
        assertEquals("com.example.app35", restored.last().packageName)
        assertEquals(PopupSignalReason.SCREEN_HIJACKING, restored.last().reasons.single())
    }


    @Test
    fun markingSafePurgesMatchingArchivePackageImmediately() {
        val now = 1_800_000_000_000L
        val incident = PopupIncident("com.example.safe", 45, PopupRiskStatus.NEEDS_CHECK,
            listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL), listOf(now))
        assertTrue(PopupDetectorPreferences(context).appendIncidents(listOf(incident)))
        assertEquals(1, PopupDetectorPreferences(context).archivedIncidents().size)
        assertTrue(PopupDetectorPreferences(context).markSafe("com.example.safe"))
        assertTrue(PopupDetectorPreferences(context).archivedIncidents().isEmpty())
    }

    @Test
    fun archiveLoadMigratesLegacyDuplicateExcludedSafeAndInvalidBurstEntries() {
        val now = 1_800_000_000_000L
        val prefs = context.getSharedPreferences("spg_popup_detector", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("safe_packages_json", JSONArray().put("com.example.safe").toString())
            .putString("incident_archive_json", JSONArray().apply {
                put(legacyIncident("com.example.safe", 45, listOf("NO_LAUNCHER_ICON", "RECENT_INSTALL"), listOf(now)))
                put(legacyIncident("com.transsion.itel.launcher", 45, listOf("NO_LAUNCHER_ICON", "RECENT_INSTALL"), listOf(now)))
                put(legacyIncident("com.example.invalidburst", 70,
                    listOf("BURST_LOOP_2", "NO_LAUNCHER_ICON", "RECENT_INSTALL"), listOf(now - 73L * 60_000L, now)))
                put(legacyIncident("com.example.duplicate", 45, listOf("NO_LAUNCHER_ICON", "RECENT_INSTALL"), listOf(now - 2)))
                put(legacyIncident("com.example.duplicate", 45, listOf("NO_LAUNCHER_ICON", "RECENT_INSTALL"), listOf(now - 1)))
            }.toString()).commit()
        val migrated = PopupDetectorPreferences(context).archivedIncidents()
        assertEquals(listOf("com.example.duplicate"), migrated.map { it.packageName })
        assertEquals(now - 1, migrated.single().lastSeenAtMs)
        assertEquals(1, JSONArray(prefs.getString("incident_archive_json", "[]")).length())
    }

    private fun legacyIncident(packageName: String, score: Int, reasons: List<String>, times: List<Long>) =
        JSONObject().put("packageName", packageName).put("riskScore", score)
            .put("status", PopupRiskStatus.SUSPICIOUS.name)
            .put("reasons", JSONArray(reasons)).put("times", JSONArray(times))

    @Test
    fun simulatedIncidentsAreNeverPersisted() {
        val simulated = PopupIncident(
            packageName = "com.superd.simulated.popup",
            riskScore = 70,
            status = PopupRiskStatus.SUSPICIOUS,
            reasons = listOf(PopupSignalReason.BURST_LOOP_3_PLUS),
            eventTimesMs = listOf(1L),
            isSimulated = true
        )
        assertTrue(PopupDetectorPreferences(context).appendIncidents(listOf(simulated)))
        assertTrue(PopupDetectorPreferences(context).archivedIncidents().isEmpty())
    }
}
