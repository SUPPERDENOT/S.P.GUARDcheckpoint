package com.superdenot.powerguard.service

import com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode
import com.superdenot.powerguard.domain.model.StatusBarParameter
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarRotationRefreshPolicyTest {
    private val rotation = StatusBarIndicatorSetting(
        enabled = true, parameter = StatusBarParameter.POWER,
        displayMode = StatusBarDisplayMode.CUSTOM_ROTATION,
        rotationParameters = listOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT),
        rotationIntervalSeconds = 5
    )
    @Test fun customRotationUsesOneSecondWhenInteractive() {
        assertEquals(1_000L, StatusBarRotationRefreshPolicy.intervalMs(rotation, true))
    }
    @Test fun screenOffUsesPowerSavingInterval() {
        assertEquals(3_000L, StatusBarRotationRefreshPolicy.intervalMs(rotation, false))
    }
    @Test fun disabledOrSingleMetricUsesPowerSavingInterval() {
        assertEquals(3_000L, StatusBarRotationRefreshPolicy.intervalMs(rotation.copy(enabled = false), true))
        assertEquals(3_000L, StatusBarRotationRefreshPolicy.intervalMs(rotation.copy(displayMode = StatusBarDisplayMode.SINGLE_METRIC), true))
    }
}