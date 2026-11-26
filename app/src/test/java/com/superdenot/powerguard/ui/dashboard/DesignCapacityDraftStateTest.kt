package com.superdenot.powerguard.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignCapacityDraftStateTest {
    @Test fun unchangedManualCapacityDoesNotEnableSave() {
        assertFalse(hasDesignCapacityDraftChanges(5500, 5500, savedIsManualMode = true))
    }
    @Test fun changedManualCapacityEnablesSave() {
        assertTrue(hasDesignCapacityDraftChanges(5000, 5500, savedIsManualMode = true))
    }
    @Test fun switchingAutomaticToManualIsDirtyEvenWhenNumberMatches() {
        assertTrue(hasDesignCapacityDraftChanges(5000, 5000, savedIsManualMode = false))
    }
    @Test fun openingAutomaticCapacityWithSameValueKeepsSaveClean() {
        assertFalse(
            hasDesignCapacityDraftChanges(
                parsedInput = 5000,
                savedCapacityMah = 5000,
                savedIsManualMode = false,
                draftIsManualMode = false
            )
        )
    }
    @Test fun editingManualCapacityFrom5500To5000RemainsDirty() {
        assertTrue(
            hasDesignCapacityDraftChanges(
                parsedInput = 5000,
                savedCapacityMah = 5500,
                savedIsManualMode = true,
                draftIsManualMode = true
            )
        )
    }
    @Test fun invalidOrMissingCapacityNeverEnablesSave() {
        assertFalse(hasDesignCapacityDraftChanges(null, 5000, savedIsManualMode = true))
        assertFalse(hasDesignCapacityDraftChanges(999, 5000, savedIsManualMode = true))
        assertFalse(hasDesignCapacityDraftChanges(20001, 5000, savedIsManualMode = true))
    }
    @Test fun automaticActionIsEnabledOnlyForManualOverride() {
        assertTrue(canUseAutomaticDesignCapacity(true))
        assertFalse(canUseAutomaticDesignCapacity(false))
    }
}
