package com.superdenot.powerguard.ui.analytics

import java.util.Locale

internal object AppDisplayNameFormatter {
    private val knownLabels = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.mobile.legends" to "Mobile Legends",
        "com.brave.browser" to "Brave",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube"
    )

    fun format(packageName: String, systemLabel: String?): String {
        val label = systemLabel?.trim().orEmpty()
        if (label.isNotEmpty() && label != packageName && !label.startsWith("com.")) return label
        return knownLabels[packageName] ?: readablePackageFallback(packageName)
    }

    private fun readablePackageFallback(packageName: String): String {
        val segment = packageName.substringAfterLast('.').ifBlank { packageName }
        val words = segment
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
        return words.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
        }.ifBlank { "Aplikasi tidak dikenal" }
    }
}
