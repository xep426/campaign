package io.github.xep426.campaign.domain.repository

import io.github.xep426.campaign.domain.model.Campaign
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Campaigns, which are now the whole model.
 *
 * `promote`, `assign` and `carryForward` are gone with the day. They
 * existed to move a task between a day and a campaign; there is nowhere
 * else for a step to be any more, so there is nothing to move it from.
 */
interface CampaignRepository {

    /** Running campaigns: pinned ones first, in slot order. */
    fun active(): Flow<List<Campaign>>

    /** Completed campaigns, most recently closed first. */
    fun finished(): Flow<List<Campaign>>

    /** A campaign with no steps behind it yet. Returns its id. */
    suspend fun create(title: String, on: LocalDate): Long

    suspend fun rename(id: Long, title: String)

    suspend fun setNotes(id: Long, notes: String)

    /**
     * Pins a campaign to the first free questlog slot.
     *
     * Returns the slot it landed in, or null when all [Campaign.SLOTS] are
     * taken — the caller says so rather than silently evicting someone
     * else's campaign, and nothing is written in that case. The limit is
     * the product; quietly making room for a fourth would dissolve it.
     */
    suspend fun pin(id: Long): Int?

    /** Lets a campaign go back to being merely kept. */
    suspend fun unpin(id: Long)

    /**
     * Marks a campaign done on [on], freeing its slot.
     *
     * The only way to close one. Archiving used to sit beside this and
     * wrote the same row with a different word in it.
     */
    suspend fun complete(id: Long, on: LocalDate)

    /** Back to ACTIVE and unpinned, for the closed-by-accident case. */
    suspend fun reopen(id: Long)

    suspend fun delete(id: Long)
}
