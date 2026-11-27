package com.superdenot.powerguard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PopupDetectorPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clearPreferences() {
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
