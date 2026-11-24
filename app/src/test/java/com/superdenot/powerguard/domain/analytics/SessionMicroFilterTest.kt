package com.superdenot.powerguard.domain.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMicroFilterTest {
    @Test
    fun hidesSessionsShorterThanOneMinute() {
        assertTrue(SessionMicroFilter.shouldHide(59, 1, 25.0))
        assertFalse(SessionMicroFilter.shouldHide(60, 1, 25.0))
    }

    @Test
    fun doesNotHideLongZeroGainSessionsThatNeedAnomalyReview() {
        assertFalse(SessionMicroFilter.shouldHide(600, 0, 0.5))
        assertFalse(SessionMicroFilter.shouldHide(600, 0, 0.0))
        assertFalse(SessionMicroFilter.shouldHide(600, 0, 0.6))
    }

    @Test
    fun doesNotHideLongSessionWithSocGain() {
        assertFalse(SessionMicroFilter.shouldHide(600, 5, 120.0))
    }

    @Test
    fun shortDurationRuleIsIndependentOfSocAndCharge() {
        assertTrue(SessionMicroFilter.shouldHide(10, 10, 300.0))
    }
}