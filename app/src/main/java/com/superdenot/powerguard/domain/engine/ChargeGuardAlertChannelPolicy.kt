package com.superdenot.powerguard.domain.engine

import java.security.MessageDigest

object ChargeGuardAlertChannelPolicy {
    fun channelId(soundUri: String?, vibrate: Boolean): String {
        val configuration = "${soundUri ?: "SILENT"}|vibrate=$vibrate"
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
}
