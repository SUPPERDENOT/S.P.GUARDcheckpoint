package com.superdenot.powerguard.domain.analytics

import com.superdenot.powerguard.data.local.entity.DailyRollupEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCycleTrendTest {
    @Test fun aggregatesSevenCalendarDaysAndMarksHighTemperature() {
        val today = LocalDate.of(2026, 11, 24)
        val rollups = listOf(
            DailyRollupEntity("2026-11-24", 0.4f, 500.0, 36f, 41f, 1),
            DailyRollupEntity("2026-11-22", 0.2f, 250.0, 35f, 38f, 0)
        )
        val points = DailyCycleTrend.aggregate(rollups, today)
        assertEquals(7, points.size)
        assertEquals(LocalDate.of(2026, 11, 18), points.first().date)
        assertEquals(0.4, points.last().totalCycles, 0.0001)
        assertTrue(points.last().peakTemperatureCelsius!! >= 40.0)
        assertFalse(points[5].hasData)
        assertEquals(500.0, points.last().totalMahCharged, 0.0001)
        assertEquals(1, points.last().anomalyCount)
    }

    @Test fun formatsThreeLetterWeekdayLabelsWithoutMondayTuesdayCollision() {
        assertEquals("Rab", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 25)))
        assertEquals("Kam", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 26)))
        assertEquals("Jum", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 27)))
        assertEquals("Sab", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 28)))
        assertEquals("Min", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 29)))
        assertEquals("Sen", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 11, 30)))
        assertEquals("Sel", DailyCycleTrend.formatWeekdayShort(LocalDate.of(2026, 12, 1)))
    }

    @Test fun formatsTodayYesterdayAndOlderDates() {
        val today = LocalDate.of(2026, 11, 24)
        assertEquals("Hari Ini · 24 Nov 2026", DailyCycleTrend.formatDateLabel("2026-11-24", today))
        assertEquals("Kemarin · 23 Nov 2026", DailyCycleTrend.formatDateLabel("2026-11-23", today))
        assertEquals("22 Nov 2026", DailyCycleTrend.formatDateLabel("2026-11-22", today))
    }
}
