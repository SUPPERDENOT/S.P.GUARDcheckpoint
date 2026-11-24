package com.superdenot.powerguard.domain.engine

import java.security.MessageDigest

object ChargeGuardAlertChannelPolicy {
    fun channelId(soundUri: String?, vibrationPattern: LongArray): String {
        val configuration = "${soundUri ?: "SILENT"}|vibration=${vibrationPattern.joinToString(",")}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(configuration.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        return "charge_guard_alert_${digest.take(16)}"
    }
}

object ChargeGuardAlertSoundUriResolver {
    fun resolve(selection: String, defaultUri: String, canOpenCustomUri: (String) -> Boolean): String? =
        when {
            selection == "SILENT" -> null
            selection == "DEFAULT" || selection.isBlank() -> defaultUri
            runCatching { canOpenCustomUri(selection) }.getOrDefault(false) -> selection
            else -> defaultUri
        }

    /** Resolve a readable custom tone, then the system default, then Android's notification default. */
    fun resolvePlayable(
        selection: String,
        defaultUri: String,
        secondaryDefaultUri: String?,
        canOpenCustomUri: (String) -> Boolean,
        canCreateRingtone: (String) -> Boolean
    ): String? {
        if (selection == "SILENT") return null
        val customCandidate = selection.takeUnless { it == "DEFAULT" || it.isBlank() }
            ?.takeIf { runCatching { canOpenCustomUri(it) }.getOrDefault(false) }
        return listOfNotNull(customCandidate, defaultUri, secondaryDefaultUri)
            .distinct()
            .firstOrNull { uri -> runCatching { canCreateRingtone(uri) }.getOrDefault(false) }
    }
}
