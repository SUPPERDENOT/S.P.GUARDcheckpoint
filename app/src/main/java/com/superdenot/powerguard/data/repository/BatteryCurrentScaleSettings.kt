package com.superdenot.powerguard.data.repository
import android.content.Context
import android.content.SharedPreferences
import com.superdenot.powerguard.domain.model.BatteryCurrentScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
/** Synchronous engine reads share the persistent preference observed by Settings. */
object BatteryCurrentScaleSettings {
    private const val PREFERENCES_NAME = "powerguard_sensor_calibration"
    private const val KEY_CURRENT_SCALE = "battery_current_scale"
    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    internal fun resolveStoredValue(value: String?): BatteryCurrentScale =
        BatteryCurrentScale.fromStoredValue(value)
    fun read(context: Context): BatteryCurrentScale =
        resolveStoredValue(preferences(context).getString(KEY_CURRENT_SCALE, null))
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(context: Context): Flow<BatteryCurrentScale> = callbackFlow {
        val prefs = preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
            if (key == KEY_CURRENT_SCALE) trySend(resolveStoredValue(shared?.getString(KEY_CURRENT_SCALE, null)))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(resolveStoredValue(prefs.getString(KEY_CURRENT_SCALE, null)))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun set(context: Context, scale: BatteryCurrentScale) {
        preferences(context).edit().putString(KEY_CURRENT_SCALE, scale.name).apply()
    }
}
