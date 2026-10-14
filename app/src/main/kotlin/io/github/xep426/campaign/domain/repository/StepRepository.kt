package io.github.xep426.campaign.domain.repository

import io.github.xep426.campaign.domain.model.Step
import kotlinx.coroutines.flow.Flow

/**
 * The steps inside campaigns.
 *
 * Small, because a step barely has a life of its own any more: it is
 * written, it is ordered, it is finished. Everything that used to make
 * this interface big — which day it sat on, which of that day's three
 * slots it held, whether it could be carried to tomorrow — went away with
 * the day.
 */
interface StepRepository {

    /**
     * Finished steps whose campaign was deleted.
     *
     * History shows them anyway. The foreign key sets campaignId to null
     * rather than cascading, precisely so that deleting a campaign does
     * not rewrite what you actually did.
     */
    fun completedOrphans(limit: Int): Flow<List<Step>>

    /** Appends a step to a campaign. Blank titles are ignored. */
    suspend fun add(campaignId: Long, title: String)

    suspend fun rename(id: Long, title: String)

    /**
     * Toggles a step, stamping or clearing its completion date with it.
     *
     * [on] is passed in rather than read from the clock here so the caller
     * decides what "today" means — and so the repository stays testable
     * without a clock.
     */
    suspend fun setCompleted(id: Long, completed: Boolean, on: java.time.LocalDate)

    suspend fun delete(id: Long)

    /**
     * Moves a step within its campaign.
     *
     * Positions are rewritten for the whole campaign in one pass. The
     * index on (position) is not unique — see the note in StepEntity about
     * what uniqueness did to reordering last time.
     */
    suspend fun move(campaignId: Long, from: Int, to: Int)
}
