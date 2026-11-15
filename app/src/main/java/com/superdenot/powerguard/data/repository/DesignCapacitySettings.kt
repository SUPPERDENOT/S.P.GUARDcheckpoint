package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.powerGuardDataStore by preferencesDataStore(name = "powerguard_prefs")
internal val KEY_DESIGN_CAPACITY = intPreferencesKey("custom_design_capacity_mah")
private val KEY_AUTO_MONITORING_ENABLED = booleanPreferencesKey("auto_monitoring_enabled")

data class DesignCapacitySetting(
    val capacityMah: Int?,
    val isManualOverride: Boolean
)

object DesignCapacitySettings {
    internal fun resolveSetting(
        manualCapacityMah: Int?,
        automaticCapacityMah: Int?
    ): DesignCapacitySetting {
        val manual = manualCapacityMah?.takeIf { it in 1000..20000 }
        val automatic = automaticCapacityMah?.takeIf { it in 1000..15000 }
        return DesignCapacitySetting(
            capacityMah = manual ?: automatic,
            isManualOverride = manual != null
        )
    }

    fun observe(context: Context, engine: BatteryMonitorEngine): Flow<DesignCapacitySetting> =
        context.powerGuardDataStore.data.map { prefs ->
            resolveSetting(
                manualCapacityMah = prefs[KEY_DESIGN_CAPACITY],
                automaticCapacityMah = engine.designCapacityMah
            )
        }

    suspend fun resolve(context: Context, automaticCapacityMah: Int): Int {
        return resolveSetting(
            manualCapacityMah = context.powerGuardDataStore.data.first()[KEY_DESIGN_CAPACITY],
            automaticCapacityMah = automaticCapacityMah
        ).capacityMah ?: 0
    }

    suspend fun saveManual(context: Context, capacityMah: Int) {
        require(capacityMah in 1000..20000)
        context.powerGuardDataStore.edit { it[KEY_DESIGN_CAPACITY] = capacityMah }
    }

    suspend fun useAutomatic(context: Context) {
        context.powerGuardDataStore.edit { it.remove(KEY_DESIGN_CAPACITY) }
    }
}


/** Preferensi pemantauan otomatis saat perangkat mulai mengisi daya. Default ON menjaga perilaku versi sebelumnya. */
object AutoMonitoringSettings {
    internal fun resolveEnabled(preferenceValue: Boolean?): Boolean = preferenceValue ?: true

    internal fun canStartFromPowerConnected(
        autoMonitoringEnabled: Boolean,
        batteryOptimizationIgnored: Boolean
    ): Boolean = autoMonitoringEnabled && batteryOptimizationIgnored

    fun observe(context: Context): Flow<Boolean> =
        context.powerGuardDataStore.data.map { resolveEnabled(it[KEY_AUTO_MONITORING_ENABLED]) }

    suspend fun isEnabled(context: Context): Boolean =
        resolveEnabled(context.powerGuardDataStore.data.first()[KEY_AUTO_MONITORING_ENABLED])

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.powerGuardDataStore.edit { it[KEY_AUTO_MONITORING_ENABLED] = enabled }
    }
}
