package io.github.xep426.campaign.domain.repository

import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Campaigns, plus the two acts that join them to days.
 *
 * [promote] and [pull] live here rather than in [TaskRepository] because
 * both are campaign-shaped: one creates a campaign, the other spends one.
 * Splitting them across two repositories would mean a caller had to hold
 * both and sequence the writes itself — which is precisely how a promoted
 * task ends up existing without its campaign after a crash between the
 * two calls.
 */
interface CampaignRepository {

    fun active(): Flow<List<Campaign>>

    /** Completed and archived, most recently closed first. */
    fun finished(): Flow<List<Campaign>>

    fun byId(id: Long): Flow<Campaign?>

    /** A campaign with no task behind it yet. Returns its id. */
    suspend fun create(title: String, on: LocalDate): Long

    /**
     * Turns a daily task into a campaign under [title] and links the task
     * to it, in one transaction. Returns the new campaign's id.
     *
     * [title] is asked for rather than inherited from the task, and that
     * is the whole point of the parameter existing. The first version
     * inherited it, on the reasoning that the user had already written the
     * name — but a task is a STEP and the campaign is the larger effort it
     * serves. "Update my address" is not the campaign; "sell the property"
     * is. Inheriting produced campaigns that had to be renamed immediately,
     * every time.
     */
    suspend fun promote(taskId: Long, title: String, on: LocalDate): Long

    /**
     * Links [taskId] to an existing campaign, or clears the link with null.
     *
     * Assignment is the general act; [promote] is the special case where
     * the campaign does not exist yet. Keeping them as one concept in the
     * interface would mean either the caller invents a title it does not
     * need, or the campaign gets a name nobody chose.
     */
    suspend fun assign(taskId: Long, campaignId: Long?)

    /**
     * Moves an outstanding task onto [date], into its first free slot.
     *
     * Returns the slot it landed in, or null when the day is already full
     * — the caller says so rather than silently dropping it, and nothing
     * is written in that case.
     */
    suspend fun carryForward(taskId: Long, date: LocalDate): Int?

    suspend fun rename(id: Long, title: String)

    suspend fun setNotes(id: Long, notes: String)

    /** Closes a campaign as [status] — COMPLETED or ARCHIVED. */
    suspend fun close(id: Long, status: CampaignStatus, on: LocalDate)

    /** Back to ACTIVE, for the archive-by-accident case. */
    suspend fun reopen(id: Long)

    /**
     * Gone, as opposed to closed.
     *
     * Distinct from [close] because "I finished this", "I stopped doing
     * this" and "this should never have existed" are three different
     * statements and only the first two belong in history. Days that
     * referenced it survive: the foreign key is ON DELETE SET NULL, so the
     * work still happened, it just stops being attributed. Deleting a
     * campaign must never delete a day.
     */
    suspend fun delete(id: Long)
}
