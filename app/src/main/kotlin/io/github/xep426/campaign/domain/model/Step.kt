package io.github.xep426.campaign.domain.model

import java.time.LocalDate

/**
 * One step of a campaign. It lives until it is done.
 *
 * This was `DailyTask`, and the rename is the whole change in one word.
 * A task belonged to a day: it had a date and one of that day's three
 * slots, and at 22:00 the day turned and took it with it. A step belongs
 * to a campaign and to nothing else — it waits as long as it has to.
 *
 * Which is the point. The daily model threw away unfinished work every
 * night, and for the things this app turned out to serve — a hobby, a side
 * project, the thing that has been half-done since spring — being thrown
 * away nightly is not pressure, it is just absence. Those lose to everyday
 * life because they are never the most urgent thing, not because a
 * particular Tuesday went badly.
 *
 * [position] orders steps within their campaign, so a campaign can read as
 * a chain: do this, then this. [addedOn] and [completedOn] are what is
 * left of the old `date` column, split into the two facts it was
 * conflating.
 */
data class Step(
    val id: Long = 0,
    val title: String,
    val campaignId: Long? = null,
    /** Joined convenience, not stored — the widget shows which campaign. */
    val campaignTitle: String? = null,
    val position: Int = 0,
    val completed: Boolean = false,
    val addedOn: LocalDate,
    /**
     * Null while open.
     *
     * The one date the app still cares about: a campaign's staleness is
     * the gap since its most recent completed step, and that gap is the
     * pressure the nightly reset used to supply.
     */
    val completedOn: LocalDate? = null,
)
