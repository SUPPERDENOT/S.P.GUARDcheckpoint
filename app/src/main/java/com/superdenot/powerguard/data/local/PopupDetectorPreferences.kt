package com.superdenot.powerguard.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import com.superdenot.powerguard.domain.security.addPopupSafePackage
import com.superdenot.powerguard.domain.security.capPopupIncidentArchive

internal class PopupDetectorPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun safePackages(): Set<String> = readStringArray(KEY_SAFE_PACKAGES)

    fun markSafe(packageName: String): Boolean {
        val next = addPopupSafePackage(safePackages(), packageName)
        return preferences.edit().putString(KEY_SAFE_PACKAGES, JSONArray(next.toList()).toString()).commit()
    }

    fun restoreSafePackages(): Boolean = preferences.edit().remove(KEY_SAFE_PACKAGES).commit()

    fun archivedIncidents(): List<PopupIncident> {
        val json = runCatching { JSONArray(preferences.getString(KEY_INCIDENT_ARCHIVE, "[]")) }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val reasons = item.optJSONArray("reasons") ?: JSONArray()
                val times = item.optJSONArray("times") ?: JSONArray()
                val parsedReasons = buildList {
                    for (reasonIndex in 0 until reasons.length()) {
                        runCatching { PopupSignalReason.valueOf(reasons.optString(reasonIndex)) }
                            .getOrNull()?.let(::add)
                    }
                }
                val parsedTimes = buildList {
                    for (timeIndex in 0 until times.length()) add(times.optLong(timeIndex))
                }
                val status = runCatching { PopupRiskStatus.valueOf(item.optString("status")) }
                    .getOrDefault(PopupRiskStatus.NEEDS_CHECK)
                val packageName = item.optString("packageName").takeIf { it.isNotBlank() } ?: continue
                add(
                    PopupIncident(
                        packageName = packageName,
                        riskScore = item.optInt("riskScore").coerceIn(0, 100),
                        status = status,
                        reasons = parsedReasons,
                        eventTimesMs = parsedTimes
                    )
                )
            }
        }
    }

    fun appendIncidents(incidents: List<PopupIncident>): Boolean {
        val newItems = incidents.filterNot { it.isSimulated }
        if (newItems.isEmpty()) return true
        val merged = (archivedIncidents() + newItems)
            .distinctBy { listOf(it.packageName, it.lastSeenAtMs.toString(), it.reasons.map { reason -> reason.name }.sorted().joinToString()).joinToString(":") }
            .let(::capPopupIncidentArchive)
        val json = JSONArray()
        merged.forEach { incident ->
            val reasons = JSONArray().apply { incident.reasons.forEach { put(it.name) } }
            val times = JSONArray().apply { incident.eventTimesMs.forEach { put(it) } }
            json.put(
                JSONObject()
                    .put("packageName", incident.packageName)
                    .put("riskScore", incident.riskScore)
                    .put("status", incident.status.name)
                    .put("reasons", reasons)
                    .put("times", times)
            )
        }
        return preferences.edit().putString(KEY_INCIDENT_ARCHIVE, json.toString()).commit()
    }

    private fun readStringArray(key: String): Set<String> {
        val json = runCatching { JSONArray(preferences.getString(key, "[]")) }.getOrNull() ?: return emptySet()
        return buildSet {
            for (index in 0 until json.length()) {
                json.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "spg_popup_detector"
        const val KEY_SAFE_PACKAGES = "safe_packages_json"
        const val KEY_INCIDENT_ARCHIVE = "incident_archive_json"
    }
}
