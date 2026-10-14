package io.github.xep426.campaign.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A long-running undertaking, broken into steps, with no deadline.
 *
 * THE CAMPAIGN IS NOW THE UNIT. It used to be a lens over tasks that lived
 * on days; the days are gone and the campaign is what remains. You name
 * something you want to move — learn the language, sell the flat, get the
 * damp fixed — break it into steps, and it stays on the list until it is
 * finished.
 *
 * THE LIMIT MOVED UP A LEVEL rather than disappearing. It was three tasks
 * a day; it is now [SLOTS] campaigns pinned at once. That is still a
 * forcing function — you may only push three things — but it no longer
 * resets every night, which is what made the old one useless for work that
 * takes months. You may keep as many campaigns as you like; you may only
 * have three in front of you.
 *
 * A campaign still AUTHORS nothing beyond its name and its steps. The
 * `nextStep` field it once had was a second place to plan the same work.
 */
data class Campaign(
    val id: Long = 0,
    val title: String,
    val status: CampaignStatus = CampaignStatus.ACTIVE,
    val createdAt: LocalDate,
    val closedAt: LocalDate? = null,
    val notes: String = "",
    /** 0..2 while pinned to the questlog, null when it is merely kept. */
    val slot: Int? = null,
    /** Not yet done, in [Step.position] order. */
    val openSteps: List<Step> = emptyList(),
    /** Done, most recent first. */
    val doneSteps: List<Step> = emptyList(),
) {
    val isPinned: Boolean get() = slot != null

    /**
     * What this campaign is asking of you right now.
     *
     * The first open step, not a chosen one: a campaign that needs you to
     * decide which of its steps is current has handed the deciding back,
     * and deciding is the work this app is supposed to absorb. Reorder the
     * steps if the order is wrong.
     */
    val nextStep: Step? get() = openSteps.firstOrNull()

    val stepsTaken: Int get() = doneSteps.size

    /** Nothing left to do, but not yet declared finished. */
    val isSpent: Boolean get() = openSteps.isEmpty()

    /** The day this campaign last actually moved, if it ever has. */
    val lastProgress: LocalDate?
        get() = doneSteps.mapNotNull { it.completedOn }.maxOrNull()

    /**
     * Days since the last completed step, or since it began if there has
     * never been one.
     *
     * THIS IS THE PRESSURE THAT REPLACED THE NIGHTLY RESET, and the app
     * needs one. The old model made you re-choose every evening whether
     * you liked it or not; a questlog asks nothing, so a campaign could
     * sit for a year looking exactly as it did on day one. The number of
     * days it has been ignored is the one honest thing the app can say
     * about that, and it says it without inventing a streak to break.
     */
    fun daysSinceProgress(today: LocalDate): Long =
        ChronoUnit.DAYS.between(lastProgress ?: createdAt, today).coerceAtLeast(0)

    /** Days since it began — the "22 days" half of the card's metadata. */
    fun daysRunning(today: LocalDate): Long =
        ChronoUnit.DAYS.between(createdAt, closedAt ?: today) + 1

    companion object {
        /**
         * How many campaigns can be pinned at once.
         *
         * The three that used to be slots in a day. Same number, same
         * argument — three is what fits on a widget and in a head — but it
         * now limits how much you are PUSHING rather than how much a
         * single day may contain.
         */
        const val SLOTS = 3

        private val NUMERALS = listOf("I", "II", "III")

        /** I, II, III — the pinned position as the interface names it. */
        fun numeralFor(slot: Int): String = NUMERALS.getOrElse(slot) { "" }
    }
}

/**
 * A campaign is running or it is done. There is no third state.
 *
 * ARCHIVED existed alongside COMPLETED and wrote the same row with a
 * different word in it: same removal from the list, same place in history,
 * same reversibility. The only visible difference was a tag's colour, and
 * two buttons that differ only by a label are a question the screen asks
 * and never answers. Deleting is what "this should not be in my history"
 * means, and that already exists.
 */
enum class CampaignStatus { ACTIVE, COMPLETED }
