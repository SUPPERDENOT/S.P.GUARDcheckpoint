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
    }

    @Test
    fun packageLikeOrBlankSystemLabelFallsBackToCleanName() {
        assertEquals("WhatsApp", AppDisplayNameFormatter.format("com.whatsapp", "com.whatsapp"))
        assertEquals("Browser", AppDisplayNameFormatter.format("com.example.browser", " "))
    }
}
