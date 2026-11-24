package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode
import com.superdenot.powerguard.domain.model.StatusBarParameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_STATUS_BAR_ENABLED = booleanPreferencesKey("status_bar_enabled")
private val KEY_STATUS_BAR_PARAMETER = stringPreferencesKey("status_bar_parameter")
private val KEY_STATUS_BAR_DISPLAY_MODE = stringPreferencesKey("status_bar_display_mode")
private val KEY_STATUS_BAR_ROTATION_PARAMETERS = stringSetPreferencesKey("status_bar_rotation_parameters")
private val KEY_STATUS_BAR_ROTATION_INTERVAL_SECONDS = intPreferencesKey("status_bar_rotation_interval_seconds")
private val DEFAULT_ROTATION_PARAMETERS = listOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT)

data class StatusBarIndicatorSetting(
    val enabled: Boolean = false,
    val parameter: StatusBarParameter = StatusBarParameter.POWER,
    val displayMode: StatusBarDisplayMode = StatusBarDisplayMode.SINGLE_METRIC,
    val rotationParameters: List<StatusBarParameter> = DEFAULT_ROTATION_PARAMETERS,
    val rotationIntervalSeconds: Int = 3
) {
    fun parameterAtElapsedRealtime(elapsedRealtimeMs: Long): StatusBarParameter {
        if (displayMode != StatusBarDisplayMode.CUSTOM_ROTATION || rotationParameters.size < 2) return parameter
        val intervalMs = rotationIntervalSeconds.coerceIn(1, 60).toLong() * 1_000L
        val index = ((elapsedRealtimeMs.coerceAtLeast(0L) / intervalMs) % rotationParameters.size).toInt()
        return rotationParameters[index]
    }
}

object StatusBarIndicatorSettings {
    internal fun resolveSetting(
        enabled: Boolean?,
        parameter: String?,
        displayMode: String? = null,
        rotationParameters: Set<String>? = null,
        rotationIntervalSeconds: Int? = null
    ): StatusBarIndicatorSetting {
        val selected = StatusBarParameter.selectableValues.filter { candidate ->
            candidate.name in rotationParameters.orEmpty() ||
                (candidate == StatusBarParameter.RESOURCE_SOURCE && "CHARGING_STATUS" in rotationParameters.orEmpty())
        }.let { it.takeIf { values -> values.size >= 2 } ?: DEFAULT_ROTATION_PARAMETERS }
        return StatusBarIndicatorSetting(
            enabled = enabled ?: false,
            parameter = StatusBarParameter.fromStoredValue(parameter),
            displayMode = StatusBarDisplayMode.fromStoredValue(displayMode),
            rotationParameters = selected,
            rotationIntervalSeconds = (rotationIntervalSeconds ?: 3).coerceIn(1, 60)
        )
    }

    fun observe(context: Context): Flow<StatusBarIndicatorSetting> =
        context.powerGuardDataStore.data.map { prefs ->
            resolveSetting(
                enabled = prefs[KEY_STATUS_BAR_ENABLED],
                parameter = prefs[KEY_STATUS_BAR_PARAMETER],
                displayMode = prefs[KEY_STATUS_BAR_DISPLAY_MODE],
                rotationParameters = prefs[KEY_STATUS_BAR_ROTATION_PARAMETERS],
                rotationIntervalSeconds = prefs[KEY_STATUS_BAR_ROTATION_INTERVAL_SECONDS]
            )
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_ENABLED] = enabled }
    }

    suspend fun setParameter(context: Context, parameter: StatusBarParameter) {
        if (parameter !in StatusBarParameter.selectableValues) return
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_PARAMETER] = parameter.name }
    }

    suspend fun setDisplayMode(context: Context, mode: StatusBarDisplayMode) {
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_DISPLAY_MODE] = mode.name }
    }

    suspend fun setRotationParameters(context: Context, parameters: List<StatusBarParameter>) {
        val normalized = StatusBarParameter.selectableValues.filter { it in parameters }
        if (normalized.size < 2) return
        context.powerGuardDataStore.edit {
            it[KEY_STATUS_BAR_ROTATION_PARAMETERS] = normalized.mapTo(mutableSetOf()) { parameter -> parameter.name }
        }
    }

    suspend fun setRotationIntervalSeconds(context: Context, seconds: Int) {
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_ROTATION_INTERVAL_SECONDS] = seconds.coerceIn(1, 60) }
    }

    suspend fun setConfiguration(
        context: Context,
        parameter: StatusBarParameter,
        displayMode: StatusBarDisplayMode,
        rotationParameters: List<StatusBarParameter>,
        rotationIntervalSeconds: Int
    ) {
        if (parameter !in StatusBarParameter.selectableValues) return
        val normalizedParameters = StatusBarParameter.selectableValues.filter { it in rotationParameters }
        if (normalizedParameters.size < 2) return
        context.powerGuardDataStore.edit {
            it[KEY_STATUS_BAR_PARAMETER] = parameter.name
            it[KEY_STATUS_BAR_DISPLAY_MODE] = displayMode.name
            it[KEY_STATUS_BAR_ROTATION_PARAMETERS] =
                normalizedParameters.mapTo(mutableSetOf()) { selected -> selected.name }
            it[KEY_STATUS_BAR_ROTATION_INTERVAL_SECONDS] = rotationIntervalSeconds.coerceIn(1, 60)
        }
    }
}
