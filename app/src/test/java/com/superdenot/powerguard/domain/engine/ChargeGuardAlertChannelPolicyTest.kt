package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChargeGuardAlertChannelPolicyTest {
    @Test fun channelIdIsStableForTheSameEffectiveConfiguration() {
        assertEquals(
            ChargeGuardAlertChannelPolicy.channelId("content://tone/1", longArrayOf(0L, 200L, 120L, 200L)),
            ChargeGuardAlertChannelPolicy.channelId("content://tone/1", longArrayOf(0L, 200L, 120L, 200L))
        )
    }

    @Test fun channelIdChangesWhenSoundOrVibrationChanges() {
        val doublePattern = longArrayOf(0L, 200L, 120L, 200L)
        val baseline = ChargeGuardAlertChannelPolicy.channelId("content://tone/1", doublePattern)
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId("content://tone/2", doublePattern))
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId("content://tone/1", longArrayOf(0L, 250L)))
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId("content://tone/1", longArrayOf()))
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId(null, doublePattern))
    }

    @Test fun invalidCustomUriFallsBackToSystemDefault() {
        assertEquals(
            "content://settings/system/notification_sound",
            ChargeGuardAlertSoundUriResolver.resolve(
                "content://missing/audio/404",
                "content://settings/system/notification_sound"
            ) { false }
        )
    }


    @Test fun inaccessibleCustomAudioFallsBackToSystemDefault() {
        assertEquals(
            "content://settings/system/notification_sound",
            ChargeGuardAlertSoundUriResolver.resolvePlayable(
                selection = "content://media/external/audio/media/404",
                defaultUri = "content://settings/system/notification_sound",
                secondaryDefaultUri = "content://settings/system/notification_sound_fallback",
                canOpenCustomUri = { throw SecurityException("permission denied") },
                canCreateRingtone = { it == "content://settings/system/notification_sound" }
            )
        )
    }

    @Test fun unavailableSystemRingtoneFallsBackToAndroidNotificationDefault() {
        assertEquals(
            "content://settings/system/notification_sound_fallback",
            ChargeGuardAlertSoundUriResolver.resolvePlayable(
                selection = "content://media/external/audio/media/404",
                defaultUri = "content://settings/system/notification_sound",
                secondaryDefaultUri = "content://settings/system/notification_sound_fallback",
                canOpenCustomUri = { false },
                canCreateRingtone = { it.endsWith("fallback") }
            )
        )
    }

    @Test fun silentSelectionDoesNotUseAudioFallback() {
        assertEquals(
            null,
            ChargeGuardAlertSoundUriResolver.resolvePlayable(
                selection = "SILENT",
                defaultUri = "content://settings/system/notification_sound",
                secondaryDefaultUri = "content://settings/system/notification_sound_fallback",
                canOpenCustomUri = { false },
                canCreateRingtone = { true }
            )
        )
    }

    @Test fun accessibleCustomUriIsPreservedAndSilentRemainsSilent() {
        assertEquals(
            "content://media/audio/7",
            ChargeGuardAlertSoundUriResolver.resolve(
                "content://media/audio/7",
                "content://settings/system/notification_sound"
            ) { true }
        )
        assertEquals(
            null,
            ChargeGuardAlertSoundUriResolver.resolve(
                "SILENT",
                "content://settings/system/notification_sound"
            ) { false }
        )
    }
}
