package com.superdenot.powerguard.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDisplayNameFormatterTest {
    @Test
    fun systemApplicationLabelTakesPrecedenceWhenReadable() {
        assertEquals("WhatsApp Messenger", AppDisplayNameFormatter.format("com.whatsapp", "WhatsApp Messenger"))
    }

    @Test
    fun knownPackagesHaveHumanReadableFallbackLabels() {
        assertEquals("WhatsApp", AppDisplayNameFormatter.format("com.whatsapp", null))
        assertEquals("Mobile Legends", AppDisplayNameFormatter.format("com.mobile.legends", null))
        assertEquals("Brave", AppDisplayNameFormatter.format("com.brave.browser", null))
        assertEquals("TikTok", AppDisplayNameFormatter.format("com.zhiliaoapp.musically", null))
        assertEquals("Facebook", AppDisplayNameFormatter.format("com.facebook.katana", null))
        assertEquals("Google", AppDisplayNameFormatter.format("com.google.android.googlequicksearchbox", null))
        assertEquals("YouTube", AppDisplayNameFormatter.format("com.google.android.youtube", null))
        assertEquals("Gmail", AppDisplayNameFormatter.format("com.google.android.gm", null))
        assertEquals("Google Maps", AppDisplayNameFormatter.format("com.google.android.apps.maps", null))
        assertEquals("Chrome", AppDisplayNameFormatter.format("com.android.chrome", null))
        assertEquals("Google Play Store", AppDisplayNameFormatter.format("com.android.vending", null))
        assertEquals("Shopee", AppDisplayNameFormatter.format("com.shopee.id", null))
        assertEquals("Gojek", AppDisplayNameFormatter.format("com.gojek.app", null))
        assertEquals("Tokopedia", AppDisplayNameFormatter.format("com.tokopedia.tkpd", null))
    }

    @Test
    fun packageLikeOrBlankSystemLabelFallsBackToCleanName() {
        assertEquals("WhatsApp", AppDisplayNameFormatter.format("com.whatsapp", "com.whatsapp"))
        assertEquals("Browser", AppDisplayNameFormatter.format("com.example.browser", " "))
    }
}
