package io.github.xep426.campaign.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.DailyTask
import java.time.LocalDate

/**
 * A task in one of a day's three slots.
 *
 * The index on (date, slot) is PLAIN, not unique, and that is a
 * correction. It was unique on the theory that the schema should enforce
 * the limit of three — which it never did: there is no CHECK on slot, so
 * the database would take slot = 7 without complaint. The limit lives in
 * TaskRepositoryImpl and always did.
 *
 * What uniqueness did do was make reordering absurd. SQLite checks the
 * index per row, so no task could be written into a position its
 * neighbour still held; every reorder had to park all three rows on
 * negative slots and write the real values in a second pass — negative
 * slots the unique index accepted quite happily, which rather made the
 * point. Dropping it turns a reorder into one write per row.
 *
 * ON DELETE SET NULL for the campaign link: archiving a campaign must not
 * take the days it produced out of history with it. What was done was
 * still done — the task simply stops being attributed.
 */
@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["date", "slot"]),
        Index(value = ["campaignId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
)
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** ISO-8601: "2026-08-19". Sorts and compares correctly as text. */
    val date: String,
    val slot: Int,
    val completed: Boolean = false,
    val campaignId: Long? = null,
)

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** [CampaignStatus] name. Stored as text so the DB stays readable. */
    val status: String = CampaignStatus.ACTIVE.name,
    val createdAt: String,
    val closedAt: String? = null,
    /**
     * DEAD COLUMN. Campaigns no longer author a next step — their content
     * is the set of incomplete tasks assigned to them (see [Campaign]).
     *
     * Kept in the schema rather than migrated away on purpose: dropping a
     * column in SQLite means recreating the table, and there is real user
     * data in this database. An unused column costs a few bytes; a botched
     * migration costs the history the app exists to keep. Remove it the
     * next time the schema changes for a reason that already needs one.
     */
    val nextStep: String = "",
    val notes: String = "",
)

/**
 * A task row with its campaign's title joined on — the shape the Today
 * screen needs, fetched in one query rather than by holding the whole
 * campaign list alongside.
 */
data class TaskWithCampaign(
    @Embedded val task: DailyTaskEntity,
    val campaignTitle: String?,
) {
    fun toDomain() = DailyTask(
        id = task.id,
        title = task.title,
        date = LocalDate.parse(task.date),
        slot = task.slot,
        completed = task.completed,
        campaignId = task.campaignId,
        campaignTitle = campaignTitle,
    )
}

/** Completed steps per campaign, from the group-by in [CampaignDao]. */
data class CampaignStepCount(
    val campaignId: Long,
    val steps: Int,
)

fun CampaignEntity.toDomain(
    stepsTaken: Int = 0,
    openTasks: List<DailyTask> = emptyList(),
) = Campaign(
    id = id,
    title = title,
    // Lenient: an unknown status string means the row predates a rename,
    // and an active campaign is the safer of the two ways to be wrong —
    // it stays visible and the user can close it again.
    status = runCatching { CampaignStatus.valueOf(status) }.getOrDefault(CampaignStatus.ACTIVE),
    createdAt = LocalDate.parse(createdAt),
    closedAt = closedAt?.let(LocalDate::parse),
    notes = notes,
    stepsTaken = stepsTaken,
    openTasks = openTasks,
)
