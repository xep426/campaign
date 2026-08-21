package io.github.xep426.campaign.domain.model

import java.time.LocalDate

/**
 * A longer-running effort, and the record of what it has actually moved.
 *
 * A campaign AUTHORS nothing. It has a name and it has the tasks assigned
 * to it — that is the whole object. The first version gave it a `nextStep`
 * field the user had to write and maintain, which turned out to be a
 * second place to plan the same work: you wrote the step on the campaign,
 * then wrote it again as a task.
 *
 * WHAT IT SHOWS IS THE FINISHED WORK, not the outstanding work. The
 * campaign card listed open tasks first, which put the same information in
 * two places: an open task that matters is in today's three, and Today is
 * where you look at those. What no other screen can answer is "how far has
 * this actually come" — so that is this screen's job, and the list of
 * completed steps is the answer.
 *
 * [openTasks] is still carried, because Today's carry-forward picker needs
 * somewhere to read outstanding campaign work from. It is simply not
 * displayed here any more.
 *
 * Both lists are DERIVED, never stored, so neither can drift out of step
 * with the day records:
 *  - [openTasks] — assigned and not completed, oldest day first.
 *  - [doneTasks] — assigned and completed, most recent day first.
 */
data class Campaign(
    val id: Long = 0,
    val title: String,
    val status: CampaignStatus = CampaignStatus.ACTIVE,
    val createdAt: LocalDate,
    val closedAt: LocalDate? = null,
    val notes: String = "",
    val openTasks: List<DailyTask> = emptyList(),
    val doneTasks: List<DailyTask> = emptyList(),
) {
    /** Days since it began — the "22 days" half of the card's metadata. */
    fun daysRunning(today: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(createdAt, closedAt ?: today) + 1

    /**
     * How many assigned tasks were completed.
     *
     * Derived from [doneTasks] rather than counted separately in SQL. It
     * was its own group-by query, which meant the number and the list it
     * summarised could disagree — and a count that contradicts the rows
     * under it is worse than no count.
     */
    val stepsTaken: Int get() = doneTasks.size

    /** Nothing outstanding — the campaign is quiet, not necessarily done. */
    val isQuiet: Boolean get() = openTasks.isEmpty()
}

/**
 * A campaign is running or it is done. There is no third state.
 *
 * ARCHIVED existed alongside COMPLETED because §8 asked for "archive or
 * complete", and the intent was to separate finishing something from
 * giving up on it. In practice both wrote the same row with a different
 * word in it: same removal from the list, same place in history, same
 * reversibility, counted the same in the history tally. The only
 * difference a user could see was the colour of a tag — and two buttons
 * side by side that differ only by a label are a question the screen asks
 * and never answers.
 *
 * Deleting a campaign is what "this should not be in my history" means,
 * and that already exists.
 */
enum class CampaignStatus { ACTIVE, COMPLETED }
