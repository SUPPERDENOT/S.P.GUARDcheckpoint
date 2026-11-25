package com.superdenot.powerguard.ui.dashboard

fun hasDesignCapacityDraftChanges(parsedInput: Int?, savedCapacityMah: Int?): Boolean =
    parsedInput?.let { it in 1000..20000 && it != savedCapacityMah } ?: false

fun canUseAutomaticDesignCapacity(isManualOverride: Boolean): Boolean = isManualOverride
