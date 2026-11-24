package com.superdenot.powerguard.service

import com.superdenot.powerguard.data.repository.StatusBarIndicatorSetting
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode

internal object StatusBarRotationRefreshPolicy {
    const val ACTIVE_SCREEN_INTERVAL_MS = 1_000L
    const val POWER_SAVING_INTERVAL_MS = 3_000L

    fun intervalMs(setting: StatusBarIndicatorSetting, isInteractive: Boolean): Long =
        if (setting.enabled && setting.displayMode == StatusBarDisplayMode.CUSTOM_ROTATION &&
            setting.rotationParameters.size >= 2 && isInteractive
        ) ACTIVE_SCREEN_INTERVAL_MS else POWER_SAVING_INTERVAL_MS
}