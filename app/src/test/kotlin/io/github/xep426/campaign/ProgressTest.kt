package io.github.xep426.campaign

import io.github.xep426.campaign.domain.model.Progress
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The tally is the one number in the app that passes judgement, so its
 * arithmetic is worth pinning down. Every case here is one where a
 * plausible implementation says something untrue to the user.
 */
class ProgressTest {

    @Test
    fun `full window is the ratio the user asked for`() {
        val p = Progress(completedInWindow = 45, daysCounted = 30)
        assertEquals(90, p.possible)
        assertEquals(50, p.percent)
    }

    @Test
    fun `a new user is measured against the days they have had`() {
        // Three days in, three tasks a day, all of them done. Against a
        // fixed 90 this reads 10% — which would tell someone doing
        // everything right that they are failing.
        val p = Progress(completedInWindow = 9, daysCounted = 3)
        assertEquals(9, p.possible)
        assertEquals(100, p.percent)
    }

    @Test
    fun `an empty database is not zero per cent`() {
        val p = Progress()
        assertEquals(0, p.possible)
        assertEquals(0, p.percent)
    }

    @Test
    fun `partial progress rounds down, never up to a full hundred`() {
        // 89 of 90 is 98.9%. Rounding to nearest would print 99, which is
        // fine — but the case that matters is that nothing short of every
        // slot can reach 100.
        val p = Progress(completedInWindow = 89, daysCounted = 30)
        assertEquals(98, p.percent)
    }

    @Test
    fun `exactly full is a hundred`() {
        val p = Progress(completedInWindow = 90, daysCounted = 30)
        assertEquals(100, p.percent)
    }

    @Test
    fun `all-time count is independent of the window`() {
        val p = Progress(completedAllTime = 812, completedInWindow = 12, daysCounted = 30)
        assertEquals(812, p.completedAllTime)
        assertEquals(13, p.percent)
    }
}
