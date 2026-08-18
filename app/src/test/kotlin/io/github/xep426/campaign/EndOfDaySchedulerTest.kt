package io.github.xep426.campaign

import io.github.xep426.campaign.notify.EndOfDayScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The alarm chain is booked one day at a time, so `nextOccurrence` is the
 * hinge the whole ritual turns on — get it wrong by a day and the app
 * simply stops asking, with nothing to notice.
 */
class EndOfDaySchedulerTest {

    private val nineOClock = LocalTime.of(21, 0)
    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `books today when the time is still ahead`() {
        val next = EndOfDayScheduler.nextOccurrence(
            time = nineOClock,
            now = LocalDateTime.of(today, LocalTime.of(8, 12)),
        )
        assertEquals(LocalDateTime.of(today, nineOClock), next)
    }

    @Test
    fun `books tomorrow when the time has passed`() {
        val next = EndOfDayScheduler.nextOccurrence(
            time = nineOClock,
            now = LocalDateTime.of(today, LocalTime.of(22, 30)),
        )
        assertEquals(LocalDateTime.of(today.plusDays(1), nineOClock), next)
    }

    /**
     * The boundary that matters: changing the setting to 21:00 AT 21:00
     * must book tomorrow. Firing immediately would notify the user about
     * the evening they are already having, and then book tomorrow anyway.
     */
    @Test
    fun `books tomorrow when set to the current minute`() {
        val next = EndOfDayScheduler.nextOccurrence(
            time = nineOClock,
            now = LocalDateTime.of(today, nineOClock),
        )
        assertEquals(LocalDateTime.of(today.plusDays(1), nineOClock), next)
    }

    // ── The cancellation bug, pinned ────────────────────────────────
    //
    //    Found live on 2026-08-17: the 21:00 prompt had not fired yet
    //    (inexact alarms get up to an hour), the user opened the app at
    //    21:04 to see why, and reschedule() replaced the still-pending
    //    alarm with tomorrow's. Checking why it was late is what made it
    //    never arrive. These four cases are that loop, closed.

    private val nineOClockMillis = 1_787_079_600_000L // 2026-08-17 21:00 UTC+2

    @Test
    fun `an alarm still ahead of its target is left alone`() {
        val fiveMinutesEarly = nineOClockMillis - 5 * 60_000L
        assertTrue(EndOfDayScheduler.isStillDeliverable(nineOClockMillis, fiveMinutesEarly))
    }

    @Test
    fun `an alarm minutes past its target is still coming`() {
        val fourMinutesLate = nineOClockMillis + 4 * 60_000L
        assertTrue(EndOfDayScheduler.isStillDeliverable(nineOClockMillis, fourMinutesLate))
    }

    @Test
    fun `an alarm past its whole window has missed its chance`() {
        val overAnHourLate = nineOClockMillis + 61 * 60_000L
        assertFalse(EndOfDayScheduler.isStillDeliverable(nineOClockMillis, overAnHourLate))
    }

    @Test
    fun `nothing booked is not something to protect`() {
        assertFalse(EndOfDayScheduler.isStillDeliverable(0L, nineOClockMillis))
    }

    @Test
    fun `crosses a month boundary`() {
        val lastOfAugust = LocalDate.of(2026, 8, 31)
        val next = EndOfDayScheduler.nextOccurrence(
            time = nineOClock,
            now = LocalDateTime.of(lastOfAugust, LocalTime.of(23, 59)),
        )
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 9, 1), nineOClock), next)
    }
}
