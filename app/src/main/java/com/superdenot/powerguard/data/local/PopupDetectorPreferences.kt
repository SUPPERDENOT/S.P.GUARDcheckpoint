package com.superdenot.powerguard.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import com.superdenot.powerguard.domain.security.PopupIncident
import com.superdenot.powerguard.domain.security.PopupRiskStatus
import com.superdenot.powerguard.domain.security.PopupSignalReason
import com.superdenot.powerguard.domain.security.addPopupSafePackage
import com.superdenot.powerguard.domain.security.capPopupIncidentArchive
import com.superdenot.powerguard.domain.security.sanitizePopupIncidentArchive

internal class PopupDetectorPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun safePackages(): Set<String> = readStringArray(KEY_SAFE_PACKAGES)

    fun markSafe(packageName: String): Boolean {
        val next = addPopupSafePackage(safePackages(), packageName)
        val cleanedArchive = sanitizePopupIncidentArchive(archivedIncidents(), next)
        return preferences.edit()
            .putString(KEY_SAFE_PACKAGES, JSONArray(next.toList()).toString())
            .putString(KEY_INCIDENT_ARCHIVE, serializeArchive(cleanedArchive))
            .commit()
    }

    fun restoreSafePackage(packageName: String): Boolean {
        val next = safePackages() - packageName
        return preferences.edit().putString(KEY_SAFE_PACKAGES, JSONArray(next.toList()).toString()).commit()
    }

    fun restoreSafePackages(): Boolean = preferences.edit().remove(KEY_SAFE_PACKAGES).commit()

    fun archivedIncidents(): List<PopupIncident> {
        val json = runCatching { JSONArray(preferences.getString(KEY_INCIDENT_ARCHIVE, "[]")) }.getOrNull()
            ?: return emptyList()
        val parsed = buildList {
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
        val systemPackages = parsed.asSequence()
            .filter { isSystemOrUpdatedSystemPackage(it.packageName) }
            .map { it.packageName }
            .toSet()
        val excludedPackages = safePackages() + listOfNotNull(resolveCurrentHomePackage())
        val cleaned = capPopupIncidentArchive(
            sanitizePopupIncidentArchive(parsed, excludedPackages, systemPackages)
        )
        if (cleaned != parsed) preferences.edit().putString(KEY_INCIDENT_ARCHIVE, serializeArchive(cleaned)).commit()
        return cleaned
    }

    fun appendIncidents(incidents: List<PopupIncident>): Boolean {
        val newItems = incidents.filterNot { it.isSimulated }
        if (newItems.isEmpty()) return true
        val merged = capPopupIncidentArchive(sanitizePopupIncidentArchive(archivedIncidents() + newItems, safePackages()))
        return preferences.edit().putString(KEY_INCIDENT_ARCHIVE, serializeArchive(merged)).commit()
    }

    private fun serializeArchive(incidents: List<PopupIncident>): String {
        val json = JSONArray()
        incidents.forEach { incident ->
            val reasons = JSONArray().apply { incident.reasons.forEach { put(it.name) } }
            val times = JSONArray().apply { incident.eventTimesMs.forEach { put(it) } }
            json.put(JSONObject()
                .put("packageName", incident.packageName)
                .put("riskScore", incident.riskScore)
                .put("status", incident.status.name)
                .put("reasons", reasons)
                .put("times", times))
        }
        return json.toString()
    }

    private fun resolveCurrentHomePackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }?.activityInfo?.packageName
        }.getOrNull()
    }

    private fun isSystemOrUpdatedSystemPackage(packageName: String): Boolean {
        val flags = runCatching {
            appContext.packageManager.getApplicationInfo(packageName, 0).flags
        }.getOrNull() ?: return false
        return flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
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
