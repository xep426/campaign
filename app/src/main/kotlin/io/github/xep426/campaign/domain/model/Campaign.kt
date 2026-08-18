package io.github.xep426.campaign.domain.model

import java.time.LocalDate

/**
 * A longer-running effort: the overview of related tasks still outstanding.
 *
 * A campaign AUTHORS nothing. It has a name and it has whatever incomplete
 * tasks are assigned to it — that is the whole object. The first version
 * gave it a `nextStep` field the user had to write and maintain, which
 * turned out to be a second place to plan the same work: you wrote the
 * step on the campaign, then wrote it again as a task. Tasks are made on
 * Today; a campaign is a lens over the ones that have not landed yet.
 *
 * Both list fields are DERIVED, never stored, so neither can drift out of
 * step with the day records:
 *  - [openTasks]  — assigned and not completed, oldest day first.
 *  - [stepsTaken] — how many assigned tasks were completed.
 */
data class Campaign(
    val id: Long = 0,
    val title: String,
    val status: CampaignStatus = CampaignStatus.ACTIVE,
    val createdAt: LocalDate,
    val closedAt: LocalDate? = null,
    val notes: String = "",
    val stepsTaken: Int = 0,
    val openTasks: List<DailyTask> = emptyList(),
) {
    /** Days since it began — the "22 days" half of the card's metadata. */
    fun daysRunning(today: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(createdAt, closedAt ?: today) + 1

    /** Nothing outstanding — the campaign is quiet, not necessarily done. */
    val isQuiet: Boolean get() = openTasks.isEmpty()
}

/**
 * COMPLETED and ARCHIVED both close a campaign and are deliberately
 * distinct: §8 asks for "archive or complete", and the difference is the
 * only judgement the history screen records. Finished is not the same as
 * abandoned, and a user reading back over a year deserves to see which
 * was which.
 */
enum class CampaignStatus { ACTIVE, COMPLETED, ARCHIVED }
