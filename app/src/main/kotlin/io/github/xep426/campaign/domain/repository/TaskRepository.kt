package io.github.xep426.campaign.domain.repository

import io.github.xep426.campaign.domain.model.DailyTask

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The three slots of a day, and the record of the days already spent. */
interface TaskRepository {

    /** Today's (or any day's) tasks, campaign titles already joined on. */
    fun tasksFor(date: LocalDate): Flow<List<DailyTask>>

    /**
     * Finished tasks belonging to no campaign, newest first.
     *
     * The history used to be a day-by-day ledger. It stopped meaning
     * anything once carrying a task forward MOVES it: the day it was first
     * set on no longer lists it, so "what I chose on Tuesday" is not a
     * question the data can answer. What survives an unfinished task being
     * re-slotted is the finishing itself, so that is what is recorded.
     */
    fun completedStandalone(limit: Int): Flow<List<DailyTask>>

    /**
     * Writes [title] into [slot] of [date], replacing whatever was there.
     *
     * Upsert rather than insert-or-update-by-id because the entry screen
     * edits three text fields that may or may not already exist as rows —
     * and (date, slot) is the real identity of a task anyway.
     * A blank [title] clears the slot; that is how the entry screen deletes.
     */
    suspend fun setSlot(date: LocalDate, slot: Int, title: String, campaignId: Long? = null)

    /**
     * Writes title AND campaign together, campaign explicitly.
     *
     * Distinct from [setSlot], where a null campaign means "leave the link
     * alone" so that re-typing a task on Today does not silently detach it.
     * Here null means "no campaign", because the caller is committing a
     * whole draft and its cleared link is a decision, not an omission.
     */
    suspend fun setSlotAndCampaign(
        date: LocalDate,
        slot: Int,
        title: String,
        campaignId: Long?,
    )

    /** Flips completion. Returns the new state, for the undo copy. */
    suspend fun toggleCompleted(id: Long): Boolean

    suspend fun delete(id: Long)

    /** Lowest slot with nothing in it, or null when the day is full. */
    suspend fun firstFreeSlot(date: LocalDate): Int?

    /**
     * Moves the task at [from] to position [to], shifting the rest.
     *
     * Operates on the three POSITIONS, empty ones included, so dragging
     * into a gap does what it looks like it does rather than silently
     * compacting the day.
     */
    suspend fun reorder(date: LocalDate, from: Int, to: Int)
}
