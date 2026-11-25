package com.superdenot.powerguard.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingEtaSanityCheckTest {
    @Test fun rejectsSubTenMinuteOsEtaBelowNinetyPercent() {
        assertEquals(63L, ChargingEtaSanityCheck.resolveMinutes(3L*60_000L,79,5_000,1_000,true,70L))
    }
    @Test fun keepsShortOsEtaAtOrAboveNinetyPercent() {
        assertEquals(3L, ChargingEtaSanityCheck.resolveMinutes(3L*60_000L,95,5_000,1_000,true,12L))
    }
    @Test fun keepsSaneOsEtaBelowNinetyPercent() {
        assertEquals(24L, ChargingEtaSanityCheck.resolveMinutes(24L*60_000L,79,5_000,1_000,true,63L))
    }
    @Test fun usesEstimatorWhenOsEtaOrCurrentFallbackIsUnavailable() {
        assertEquals(42L, ChargingEtaSanityCheck.resolveMinutes(null,79,5_000,1_000,true,42L))
        assertEquals(42L, ChargingEtaSanityCheck.resolveMinutes(3L*60_000L,79,5_000,0,true,42L))
        assertEquals(42L, ChargingEtaSanityCheck.resolveMinutes(3L*60_000L,79,0,1_000,true,42L))
    }
    @Test fun invalidStateAndMissingFallbackRemainUnknown() {
        assertNull(ChargingEtaSanityCheck.resolveMinutes(3L*60_000L,79,5_000,1_000,false,42L))
        assertNull(ChargingEtaSanityCheck.resolveMinutes(null,79,5_000,0,true,null))
    }
}
