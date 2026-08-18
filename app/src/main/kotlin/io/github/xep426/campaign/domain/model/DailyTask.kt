package io.github.xep426.campaign.domain.model

import java.time.LocalDate

/**
 * One of at most three tasks for a given day.
 *
 * [slot] is not in the PRD's data model and is load-bearing anyway. The
 * three slots are the product — "the second thing" keeps its position when
 * the first is completed, deleted or re-typed, and the widget has to show
 * the same order the app does. Ordering by id would reshuffle the moment
 * anything is replaced; ordering by title is nonsense. The hard limit of
 * three is then enforceable in the schema rather than in three separate
 * call sites: a unique index on (date, slot) with slot in 0..2.
 *
 * [campaignTitle] is a joined convenience, not a stored column — the Today
 * screen shows which campaign a task came from, and threading the whole
 * Campaign object through for one string would be heavier than the string.
 */
data class DailyTask(
    val id: Long = 0,
    val title: String,
    val date: LocalDate,
    val slot: Int,
    val completed: Boolean = false,
    val campaignId: Long? = null,
    val campaignTitle: String? = null,
) {
    /** I, II, III — the slot as the interface names it. */
    val numeral: String get() = numeralFor(slot)

    companion object {
        /**
         * The hard limit, in the one place that defines it.
         *
         * A CEILING, not a quota — confirmed with the author. §8's "exactly
         * three Daily Tasks per day" reads like a requirement to fill all
         * three and is not one: a day with one task set is a complete day,
         * and the app says so rather than showing two blanks as debts. Do
         * not "fix" the empty states to demand three.
         */
        const val SLOTS_PER_DAY = 3

        private val NUMERALS = listOf("I", "II", "III")

        fun numeralFor(slot: Int): String = NUMERALS.getOrElse(slot) { "" }
    }
}
