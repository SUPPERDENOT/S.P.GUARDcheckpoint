package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.powerGuardDataStore by preferencesDataStore(name = "powerguard_prefs")
internal val KEY_DESIGN_CAPACITY = intPreferencesKey("custom_design_capacity_mah")

data class DesignCapacitySetting(
    val capacityMah: Int?,
    val isManualOverride: Boolean
)

object DesignCapacitySettings {
    fun observe(context: Context, engine: BatteryMonitorEngine): Flow<DesignCapacitySetting> =
        context.powerGuardDataStore.data.map { prefs ->
            val manual = prefs[KEY_DESIGN_CAPACITY]?.takeIf { it in 1000..20000 }
            val automatic = engine.designCapacityMah.takeIf { it in 1000..15000 }
            DesignCapacitySetting(
                capacityMah = manual ?: automatic,
                isManualOverride = manual != null
            )
        }

    suspend fun resolve(context: Context, automaticCapacityMah: Int): Int {
        val manual = context.powerGuardDataStore.data.first()[KEY_DESIGN_CAPACITY]
            ?.takeIf { it in 1000..20000 }
        return manual ?: automaticCapacityMah.takeIf { it in 1000..15000 } ?: 0
    }

    suspend fun saveManual(context: Context, capacityMah: Int) {
        require(capacityMah in 1000..20000)
        context.powerGuardDataStore.edit { it[KEY_DESIGN_CAPACITY] = capacityMah }
    }

    suspend fun useAutomatic(context: Context) {
        context.powerGuardDataStore.edit { it.remove(KEY_DESIGN_CAPACITY) }
    }
}
