package com.superdenot.powerguard.ui.dashboard

fun hasDesignCapacityDraftChanges(
    parsedInput: Int?,
    savedCapacityMah: Int?,
    savedIsManualMode: Boolean,
    draftIsManualMode: Boolean = true
): Boolean = parsedInput?.let {
    it in 1000..20000 && (it != savedCapacityMah || draftIsManualMode != savedIsManualMode)
} ?: false

fun canUseAutomaticDesignCapacity(isManualOverride: Boolean): Boolean = isManualOverride
