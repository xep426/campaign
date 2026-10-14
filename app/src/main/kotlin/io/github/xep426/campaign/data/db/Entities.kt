package io.github.xep426.campaign.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.Step
import java.time.LocalDate

/**
 * A step of a campaign.
 *
 * THE TABLE IS STILL CALLED `daily_tasks` and the class is not. Renaming a
 * table in SQLite means recreating it and copying real history across, to
 * buy a word; the Kotlin name is where the word matters, because that is
 * what the rest of the code reads. The mismatch is deliberate and this
 * comment is the reason it is not a bug.
 *
 * [date] and [slot] are DEAD COLUMNS, kept for the same reason as
 * [CampaignEntity.nextStep]: dropping a column needs a table rebuild. They
 * were the day this task belonged to and its position in that day's three
 * — both meaningless now that steps outlive days. What [date] held is now
 * split in two: [addedOn] took its honest half and [completedOn] the half
 * it was guessing at. Remove all three at the next migration that already
 * needs a rebuild.
 *
 * ON DELETE SET NULL for the campaign link: deleting a campaign must not
 * take finished steps out of history with it. What was done was still
 * done — the step simply stops being attributed.
 */
@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["campaignId"]),
        Index(value = ["completedOn"]),
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
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** DEAD. See the class doc. Kept non-null so old rows still parse. */
    val date: String,
    /** DEAD. See the class doc. */
    val slot: Int = 0,
    val completed: Boolean = false,
    val campaignId: Long? = null,
    /** Order within the campaign. Ties break on id, so 0 means "oldest". */
    val position: Int = 0,
    /** ISO-8601, null while open. Sorts and compares correctly as text. */
    val completedOn: String? = null,
) {
    /** The day it was added — the surviving half of [date]. */
    val addedOn: String get() = date
}

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** [CampaignStatus] name. Stored as text so the DB stays readable. */
    val status: String = CampaignStatus.ACTIVE.name,
    val createdAt: String,
    val closedAt: String? = null,
    /**
     * DEAD COLUMN. Campaigns no longer author a next step — the next step
     * is simply the first open one (see [Campaign.nextStep]).
     *
     * Kept rather than migrated away: dropping a column in SQLite means
     * recreating the table, and there is real user data here. An unused
     * column costs a few bytes; a botched migration costs the history the
     * app exists to keep.
     */
    val nextStep: String = "",
    val notes: String = "",
    /**
     * 0..2 while pinned to the questlog, null when merely kept.
     *
     * Nullable rather than a boolean plus an order, because the position
     * IS the state: there is no such thing as pinned-without-a-place, and
     * two columns would let the database hold that contradiction.
     */
    val slot: Int? = null,
)

/**
 * A step with its campaign's title joined on — the shape the widget needs,
 * fetched in one query rather than by holding the campaign list alongside.
 */
data class StepWithCampaign(
    @Embedded val step: StepEntity,
    val campaignTitle: String?,
) {
    fun toDomain() = Step(
        id = step.id,
        title = step.title,
        campaignId = step.campaignId,
        campaignTitle = campaignTitle,
        position = step.position,
        completed = step.completed,
        addedOn = LocalDate.parse(step.addedOn),
        completedOn = step.completedOn?.let(LocalDate::parse),
    )
}

fun CampaignEntity.toDomain(
    openSteps: List<Step> = emptyList(),
    doneSteps: List<Step> = emptyList(),
) = Campaign(
    id = id,
    title = title,
    // Lenient: an unknown status string means the row predates a rename,
    // and an active campaign is the safer of the two ways to be wrong —
    // it stays visible and the user can close it again.
    //
    // This leniency is why dropping ARCHIVED needed a migration rather
    // than nothing: without one, every archived row would have failed to
    // parse and quietly come back to life as an active campaign.
    status = runCatching { CampaignStatus.valueOf(status) }.getOrDefault(CampaignStatus.ACTIVE),
    createdAt = LocalDate.parse(createdAt),
    closedAt = closedAt?.let(LocalDate::parse),
    notes = notes,
    slot = slot,
    openSteps = openSteps,
    doneSteps = doneSteps,
)
