package io.github.xep426.campaign.domain.model

/**
 * Efficiency: how much of the recent past was actually spent.
 *
 * THE DENOMINATOR IS THREE A DAY, and it means something because of what
 * the three slots are. They hold the three most important things available
 * on a day — that is the premise the whole app is built on. So a slot left
 * empty is capacity for important work that went unused, and a slot filled
 * but unfinished is a thing you named as one of the three and did not do.
 * Both belong in the denominator.
 *
 * This was argued the other way first: three is a ceiling rather than a
 * quota, so measuring against it looked like punishing someone who picks
 * two on purpose. The premise settles it. If the three ARE the most
 * important things, there is no virtue in leaving one of them unnamed.
 *
 * [daysCounted] is what keeps it from being insulting on day three:
 * measuring against thirty days of slots that did not exist yet would put
 * a new user at 10% for having done everything they set themselves. The
 * window only counts days from the first task onward, so it grows into the
 * full thirty rather than starting there.
 *
 * A stretch of days with the app untouched IS counted once those days are
 * inside the window and the first task predates them. That is deliberate:
 * the number is a record of the last thirty days, and going quiet for two
 * weeks is something that happened.
 */
data class Progress(
    /** Every task ever finished — the count that only goes up. */
    val completedAllTime: Int = 0,
    /** Finished inside the window. */
    val completedInWindow: Int = 0,
    /** Days of the window that could have held anything at all. */
    val daysCounted: Int = 0,
    /** The window's full width, for the caption. */
    val windowDays: Int = WINDOW_DAYS,
) {
    /** Slots the window could have held. */
    val possible: Int get() = daysCounted * DailyTask.SLOTS_PER_DAY

    /**
     * Truncated, and never reaching 100 from anything short of every slot —
     * a screen that says 100% while a slot sits empty is the one number
     * here that would be a lie.
     */
    val percent: Int
        get() = when {
            possible == 0 -> 0
            completedInWindow >= possible -> 100
            else -> ((completedInWindow * 100.0) / possible).toInt()
        }

    companion object {
        /**
         * Thirty days: long enough that one bad week does not define it,
         * short enough that it still describes now rather than the year.
         */
        const val WINDOW_DAYS = 30
    }
}
