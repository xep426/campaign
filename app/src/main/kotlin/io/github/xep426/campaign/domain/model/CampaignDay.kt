package io.github.xep426.campaign.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When one day's three things become the next day's.
 *
 * THE DAY DOES NOT TURN AT MIDNIGHT. It turns at the hour the user set —
 * 22:00 by default — and that single decision is what collapsed the app
 * back to one screen.
 *
 * Before this, "today" ran to midnight, so choosing tomorrow's three
 * things in the evening meant editing a day you were not in yet. That
 * needed a Tomorrow pane, a draft-and-confirm button, a separate
 * end-of-day screen, and a rule for what happened when the two disagreed.
 * All of it existed to bridge the gap between when you decide and when the
 * calendar agrees with you.
 *
 * Move the boundary to the moment you actually decide and the gap closes.
 * At 22:00 the list is simply empty and you fill it; what you write is
 * "today's" by definition, because today started two minutes ago. There is
 * nothing to switch between, because there is only ever one list.
 *
 * The cost, stated plainly: after the turn, the day just ended is gone
 * from the app. A task finished at 22:15 cannot be ticked off — it is
 * already history. That is the honest consequence of a hard boundary, and
 * it is the price of not having two lists.
 */
object CampaignDay {

    /** The default turn — see [io.github.xep426.campaign.data.settings.EndOfDaySetting]. */
    val DEFAULT_TURN: LocalTime = LocalTime.of(22, 0)

    /**
     * Which day the app is currently in.
     *
     * At or after [turnsAt], the date is TOMORROW's — the tasks written at
     * 22:30 on Monday belong to Tuesday, which is both what the user means
     * and what History should show. `!isBefore` rather than `isAfter` so
     * the boundary minute itself belongs to the new day, matching
     * `EndOfDayScheduler.nextOccurrence`, which treats the same instant as
     * already spent.
     */
    fun of(now: LocalDateTime, turnsAt: LocalTime): LocalDate =
        if (!now.toLocalTime().isBefore(turnsAt)) now.toLocalDate().plusDays(1)
        else now.toLocalDate()

    /** Convenience for call sites that only hold a clock time. */
    fun of(now: LocalDateTime, turnsAt: LocalTime, offsetDays: Long): LocalDate =
        of(now, turnsAt).plusDays(offsetDays)
}
