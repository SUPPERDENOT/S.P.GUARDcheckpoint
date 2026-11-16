package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.superdenot.powerguard.domain.model.StatusBarParameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_STATUS_BAR_ENABLED = booleanPreferencesKey("status_bar_enabled")
private val KEY_STATUS_BAR_PARAMETER = stringPreferencesKey("status_bar_parameter")

data class StatusBarIndicatorSetting(
    val enabled: Boolean = true,
    val parameter: StatusBarParameter = StatusBarParameter.BATTERY_PERCENTAGE
)

object StatusBarIndicatorSettings {
    internal fun resolveSetting(enabled: Boolean?, parameter: String?): StatusBarIndicatorSetting =
        StatusBarIndicatorSetting(
            enabled = enabled ?: true,
            parameter = StatusBarParameter.fromStoredValue(parameter)
        )

    fun observe(context: Context): Flow<StatusBarIndicatorSetting> =
        context.powerGuardDataStore.data.map { prefs ->
            resolveSetting(
                enabled = prefs[KEY_STATUS_BAR_ENABLED],
                parameter = prefs[KEY_STATUS_BAR_PARAMETER]
            )
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_ENABLED] = enabled }
    }

    suspend fun setParameter(context: Context, parameter: StatusBarParameter) {
        context.powerGuardDataStore.edit { it[KEY_STATUS_BAR_PARAMETER] = parameter.name }
    }
}
