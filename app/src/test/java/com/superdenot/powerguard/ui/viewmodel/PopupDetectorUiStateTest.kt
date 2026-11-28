package com.superdenot.powerguard.ui.viewmodel

import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupDetectorUiStateTest {
    private fun incident(
        packageName: String,
        status: PopupRiskStatus,
        simulated: Boolean = false
    ) = PopupIncident(
        packageName = packageName,
        riskScore = if (status == PopupRiskStatus.SUSPICIOUS) 80 else 45,
        status = status,
        reasons = if (status == PopupRiskStatus.SUSPICIOUS) {
            listOf(
                PopupSignalReason.SCREEN_HIJACKING,
                PopupSignalReason.NO_LAUNCHER_ICON,
                PopupSignalReason.RECENT_INSTALL
            )
        } else {
            listOf(PopupSignalReason.NO_LAUNCHER_ICON, PopupSignalReason.RECENT_INSTALL)
        },
        eventTimesMs = listOf(1_000L),
        isSimulated = simulated
    )

    @Test
    fun timelineRiskFiltersExposeAccurateCountsAndFilteredEntries() {
        val suspicious = incident("com.example.suspicious", PopupRiskStatus.SUSPICIOUS)
        val review = incident("com.example.review", PopupRiskStatus.NEEDS_REVIEW)
        val mock = incident("com.example.mock", PopupRiskStatus.SUSPICIOUS, simulated = true)
        val safe = incident("com.example.safe", PopupRiskStatus.NEEDS_REVIEW)
        val state = PopupDetectorUiState(
            currentIncidents = listOf(suspicious, review, mock),
            archivedIncidents = listOf(suspicious.copy(eventTimesMs = listOf(1_200L)), safe),
            safePackages = setOf("com.example.safe")
        )

        assertEquals(2, state.timelineEvents.size)
        assertEquals(1, state.suspiciousCount)
        assertEquals(1, state.needsReviewCount)
        assertEquals(2, state.filteredTimelineEvents.size)

        val suspiciousState = state.copy(timelineFilter = TimelineRiskFilter.SUSPICIOUS)
        assertEquals(listOf("com.example.suspicious"), suspiciousState.filteredTimelineEvents.map { it.packageName })

        val reviewState = state.copy(timelineFilter = TimelineRiskFilter.NEEDS_REVIEW)
        assertEquals(listOf("com.example.review"), reviewState.filteredTimelineEvents.map { it.packageName })

        val allState = reviewState.copy(timelineFilter = TimelineRiskFilter.ALL)
        assertEquals(2, allState.filteredTimelineEvents.size)
        assertFalse(allState.timelineEvents.any { it.isSimulated })
    }

    @Test
    fun concurrentSafeListMutationsAtomicallyReconcileSuspectsAndTimeline() = runBlocking {
        val packageA = "com.example.safe.a"
        val packageB = "com.example.safe.b"
        val rawIncidents = listOf(
            incident(packageA, PopupRiskStatus.NEEDS_REVIEW),
            incident(packageB, PopupRiskStatus.SUSPICIOUS),
            incident("com.example.visible", PopupRiskStatus.NEEDS_REVIEW)
        )
        val state = MutableStateFlow(PopupDetectorUiState(
            currentIncidents = rawIncidents,
            archivedIncidents = rawIncidents
        ))
        val mutex = Mutex()
        val snapshots = mutableListOf<PopupDetectorUiState>()

        coroutineScope {
            launch {
                mutex.withLock {
                    state.update {
                        reconcilePopupDetectorSafeList(it, it.safePackages + packageA, rawIncidents)
                    }
                    snapshots += state.value
                    yield()
                }
            }
            launch {
                mutex.withLock {
                    state.update {
                        reconcilePopupDetectorSafeList(it, it.safePackages + packageB, rawIncidents)
                    }
                    snapshots += state.value
                    yield()
                }
            }
        }

        assertEquals(setOf(packageA, packageB), state.value.safePackages)
        assertEquals(listOf("com.example.visible"), state.value.suspects.map { it.packageName })
        assertEquals(listOf("com.example.visible"), state.value.timelineEvents.map { it.packageName })
        snapshots.forEach { snapshot ->
            assertTrue(snapshot.suspects.none { it.packageName in snapshot.safePackages })
            assertTrue(snapshot.timelineEvents.none { it.packageName in snapshot.safePackages })
        }
    }

    @Test
    fun removingSafePackageImmediatelyRestoresCachedRawScanEntry() {
        val safePackage = "com.example.restored"
        val rawIncidents = listOf(incident(safePackage, PopupRiskStatus.NEEDS_REVIEW))
        val safeState = PopupDetectorUiState(
            currentIncidents = emptyList(),
            archivedIncidents = emptyList(),
            safePackages = setOf(safePackage)
        )

        val restored = reconcilePopupDetectorSafeList(
            state = safeState,
            safePackages = emptySet(),
            latestRawIncidents = rawIncidents
        )

        assertEquals(listOf(safePackage), restored.suspects.map { it.packageName })
        assertEquals(listOf(safePackage), restored.timelineEvents.map { it.packageName })
    }
}
