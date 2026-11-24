package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChargeGuardAlertChannelPolicyTest {
    @Test fun channelIdIsStableForTheSameEffectiveConfiguration() {
        assertEquals(
            ChargeGuardAlertChannelPolicy.channelId("content://tone/1", true),
            ChargeGuardAlertChannelPolicy.channelId("content://tone/1", true)
        )
    }

    @Test fun channelIdChangesWhenSoundOrVibrationChanges() {
        val baseline = ChargeGuardAlertChannelPolicy.channelId("content://tone/1", true)
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId("content://tone/2", true))
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId("content://tone/1", false))
        assertNotEquals(baseline, ChargeGuardAlertChannelPolicy.channelId(null, true))
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
