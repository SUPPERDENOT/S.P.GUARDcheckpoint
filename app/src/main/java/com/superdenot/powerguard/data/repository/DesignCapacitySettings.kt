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
private val KEY_BATTERY_MONITORING_ENABLED = booleanPreferencesKey("battery_monitoring_enabled")
private val KEY_LEGACY_AUTO_MONITORING_ENABLED = booleanPreferencesKey("auto_monitoring_enabled")
private val KEY_LEGACY_MANUAL_MONITORING_SUPPRESSED = booleanPreferencesKey("manual_monitoring_suppressed")

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


/**
 * Satu-satunya preferensi kontrol Battery Monitoring.
 * Nilai eksplisit versi lama dipertahankan; instalasi baru default OFF.
 */
object BatteryMonitoringSettings {
    internal fun resolveEnabled(currentValue: Boolean?, legacyValue: Boolean?): Boolean =
        currentValue ?: legacyValue ?: false

    internal fun canStartFromPowerConnected(enabled: Boolean, batteryOptimizationIgnored: Boolean): Boolean =
        enabled && batteryOptimizationIgnored

    private suspend fun migrateIfNeeded(context: Context) {
        context.powerGuardDataStore.edit { prefs ->
            if (KEY_BATTERY_MONITORING_ENABLED !in prefs) {
                prefs[KEY_BATTERY_MONITORING_ENABLED] =
                    resolveEnabled(null, prefs[KEY_LEGACY_AUTO_MONITORING_ENABLED])
            }
            prefs.remove(KEY_LEGACY_AUTO_MONITORING_ENABLED)
            prefs.remove(KEY_LEGACY_MANUAL_MONITORING_SUPPRESSED)
        }
    }

    fun observe(context: Context): Flow<Boolean> =
        context.powerGuardDataStore.data.map { prefs ->
            resolveEnabled(prefs[KEY_BATTERY_MONITORING_ENABLED], prefs[KEY_LEGACY_AUTO_MONITORING_ENABLED])
        }

    suspend fun isEnabled(context: Context): Boolean {
        migrateIfNeeded(context)
        return context.powerGuardDataStore.data.first()[KEY_BATTERY_MONITORING_ENABLED] ?: false
    }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        migrateIfNeeded(context)
        context.powerGuardDataStore.edit { it[KEY_BATTERY_MONITORING_ENABLED] = enabled }
    }
}
