package io.github.xep426.campaign

import io.github.xep426.campaign.domain.model.CampaignDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The day boundary is the hinge the whole app now turns on — get it wrong
 * and tasks land on the wrong date, History mis-groups, and the widget
 * disagrees with the screen.
 */
class CampaignDayTest {

    private val tenPm = LocalTime.of(22, 0)
    private val monday = LocalDate.of(2026, 8, 17)

    @Test
    fun `before the turn, the day is today`() {
        val at = LocalDateTime.of(monday, LocalTime.of(21, 59))
        assertEquals(monday, CampaignDay.of(at, tenPm))
    }

    /** The boundary minute belongs to the NEW day, not the one ending. */
    @Test
    fun `at the turn, the day is already tomorrow`() {
        val at = LocalDateTime.of(monday, tenPm)
        assertEquals(monday.plusDays(1), CampaignDay.of(at, tenPm))
    }

    @Test
    fun `after the turn, the day is tomorrow`() {
        val at = LocalDateTime.of(monday, LocalTime.of(23, 30))
        assertEquals(monday.plusDays(1), CampaignDay.of(at, tenPm))
    }

    /**
     * Past midnight the calendar has caught up: 00:30 on Tuesday is still
     * Tuesday's list, the same one written at 22:30 on Monday.
     */
    @Test
    fun `past midnight it is the same list as before midnight`() {
        val lateMonday = LocalDateTime.of(monday, LocalTime.of(22, 30))
        val earlyTuesday = LocalDateTime.of(monday.plusDays(1), LocalTime.of(0, 30))
        assertEquals(CampaignDay.of(lateMonday, tenPm), CampaignDay.of(earlyTuesday, tenPm))
    }

    @Test
    fun `a morning turn behaves the same way`() {
        val turn = LocalTime.of(8, 0)
        val beforeBreakfast = LocalDateTime.of(monday, LocalTime.of(7, 0))
        val afterBreakfast = LocalDateTime.of(monday, LocalTime.of(9, 0))
        assertEquals(monday, CampaignDay.of(beforeBreakfast, turn))
        assertEquals(monday.plusDays(1), CampaignDay.of(afterBreakfast, turn))
    }

    @Test
    fun `the turn crosses a month boundary`() {
        val lastOfAugust = LocalDate.of(2026, 8, 31)
        val at = LocalDateTime.of(lastOfAugust, LocalTime.of(22, 5))
        assertEquals(LocalDate.of(2026, 9, 1), CampaignDay.of(at, tenPm))
    }
}
