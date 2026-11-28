package com.superdenot.powerguard.ui.analytics

import java.util.Locale

internal object AppDisplayNameFormatter {
    private val fallbackAliases = mapOf("github" to "GitHub")

    private val knownLabels = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.mobile.legends" to "Mobile Legends",
        "com.brave.browser" to "Brave",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram",
        "com.google.android.googlequicksearchbox" to "Google",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.maps" to "Google Maps",
        "com.android.chrome" to "Chrome",
        "com.android.vending" to "Google Play Store",
        "com.shopee.id" to "Shopee",
        "com.gojek.app" to "Gojek",
        "com.tokopedia.tkpd" to "Tokopedia"
    )

    fun format(packageName: String, systemLabel: String?): String {
        val label = systemLabel?.trim().orEmpty()
        if (label.isNotEmpty() && label != packageName && !label.startsWith("com.")) return label
        return knownLabels[packageName] ?: readablePackageFallback(packageName)
    }

    private fun readablePackageFallback(packageName: String): String {
        val genericTrailingSegments = setOf(
            "android", "app", "mobile", "client", "id", "co", "lite", "pro",
            "free", "plus", "ui", "viewer", "player", "service"
        )
        val segments = packageName.split('.').filter(String::isNotBlank)
        val segment = segments.asReversed().firstOrNull {
            it.lowercase(Locale.ROOT) !in genericTrailingSegments
        } ?: packageName.substringAfterLast('.').ifBlank { packageName }
        val words = segment
            .replace(Regex("([a-z])([A-Z])")) { match ->
                match.groupValues[1] + " " + match.groupValues[2]
            }
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
        val normalizedSegment = segment.lowercase(Locale.ROOT)
        return fallbackAliases[normalizedSegment] ?: words.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
        }.ifBlank { "Aplikasi tidak dikenal" }
    }
}
