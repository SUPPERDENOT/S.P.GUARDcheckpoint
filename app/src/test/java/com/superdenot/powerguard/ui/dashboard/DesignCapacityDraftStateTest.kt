package com.superdenot.powerguard.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignCapacityDraftStateTest {
    @Test
    fun unchangedCapacityDoesNotEnableSave() {
        assertFalse(hasDesignCapacityDraftChanges(parsedInput = 5000, savedCapacityMah = 5000))
    }

    @Test
    fun changedValidCapacityEnablesSave() {
        assertTrue(hasDesignCapacityDraftChanges(parsedInput = 5100, savedCapacityMah = 5000))
        assertTrue(hasDesignCapacityDraftChanges(parsedInput = 5000, savedCapacityMah = null))
    }

    @Test
    fun invalidOrMissingCapacityNeverEnablesSave() {
        assertFalse(hasDesignCapacityDraftChanges(parsedInput = null, savedCapacityMah = 5000))
        assertFalse(hasDesignCapacityDraftChanges(parsedInput = 999, savedCapacityMah = 5000))
        assertFalse(hasDesignCapacityDraftChanges(parsedInput = 20001, savedCapacityMah = 5000))
    }

    @Test
    fun automaticActionIsEnabledOnlyForManualOverride() {
        assertTrue(canUseAutomaticDesignCapacity(isManualOverride = true))
        assertFalse(canUseAutomaticDesignCapacity(isManualOverride = false))
    }
}
