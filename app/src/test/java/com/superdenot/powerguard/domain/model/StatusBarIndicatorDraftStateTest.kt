package com.superdenot.powerguard.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarIndicatorDraftStateTest {
    private val metrics = setOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT)

    private fun unchanged() = hasStatusBarIndicatorUnsavedChanges(
        draftMode = StatusBarDisplayMode.CUSTOM_ROTATION,
        savedMode = StatusBarDisplayMode.CUSTOM_ROTATION,
        draftMetrics = metrics,
        savedMetrics = metrics,
        draftScale = BatteryCurrentScale.STANDARD,
        savedScale = BatteryCurrentScale.STANDARD,
        draftInterval = 10,
        savedInterval = 10
    )

    @Test
    fun identicalDraftIsNotDirty() {
        assertFalse(unchanged())
    }

    @Test
    fun eachRealConfigurationDifferenceMarksDraftDirty() {
        assertTrue(hasStatusBarIndicatorUnsavedChanges(
            StatusBarDisplayMode.SINGLE_METRIC, StatusBarDisplayMode.CUSTOM_ROTATION,
            metrics, metrics, BatteryCurrentScale.STANDARD, BatteryCurrentScale.STANDARD, 10, 10
        ))
        assertTrue(hasStatusBarIndicatorUnsavedChanges(
            StatusBarDisplayMode.CUSTOM_ROTATION, StatusBarDisplayMode.CUSTOM_ROTATION,
            metrics, setOf(StatusBarParameter.POWER), BatteryCurrentScale.STANDARD, BatteryCurrentScale.STANDARD, 10, 10
        ))
        assertTrue(hasStatusBarIndicatorUnsavedChanges(
            StatusBarDisplayMode.CUSTOM_ROTATION, StatusBarDisplayMode.CUSTOM_ROTATION,
            metrics, metrics, BatteryCurrentScale.OEM_MILLIAMPS_AS_MICROAMPS, BatteryCurrentScale.STANDARD, 10, 10
        ))
        assertTrue(hasStatusBarIndicatorUnsavedChanges(
            StatusBarDisplayMode.CUSTOM_ROTATION, StatusBarDisplayMode.CUSTOM_ROTATION,
            metrics, metrics, BatteryCurrentScale.STANDARD, BatteryCurrentScale.STANDARD, 5, 10
        ))
    }
}
