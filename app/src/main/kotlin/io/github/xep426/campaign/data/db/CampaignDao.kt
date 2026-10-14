package io.github.xep426.campaign.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Query("SELECT * FROM campaigns WHERE status = 'ACTIVE' ORDER BY createdAt ASC, id ASC")
    fun observeActive(): Flow<List<CampaignEntity>>

    /**
     * Oldest first, on purpose. A campaign running since July sits above
     * one started on Tuesday, so the list reads as a queue of standing
     * commitments rather than as a feed of recent enthusiasms.
     */
    @Query("SELECT * FROM campaigns WHERE status != 'ACTIVE' ORDER BY closedAt DESC, id DESC")
    fun observeFinished(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun observeById(id: Long): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun byId(id: Long): CampaignEntity?

    @Insert
    suspend fun insert(campaign: CampaignEntity): Long

    @Query("UPDATE campaigns SET nextStep = :nextStep WHERE id = :id")
    suspend fun setNextStep(id: Long, nextStep: String)

    /**
     * Pin a campaign to a questlog slot, or let it go.
     *
     * The caller owns the rule that at most [Campaign.SLOTS] are pinned —
     * SQLite would happily take four. It is the same division as the old
     * three-a-day limit, which also lived in the repository and not in a
     * unique index, for the same reason: an index that enforces it makes
     * every reorder a two-pass dance.
     */
    @Query("UPDATE campaigns SET slot = :slot WHERE id = :id")
    suspend fun setSlot(id: Long, slot: Int?)

    @Query("SELECT slot FROM campaigns WHERE status = 'ACTIVE' AND slot IS NOT NULL ORDER BY slot ASC")
    suspend fun occupiedSlots(): List<Int>

    @Query("UPDATE campaigns SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE campaigns SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String)

    /** Finishing a campaign frees its slot — see CampaignRepositoryImpl. */
    @Query("UPDATE campaigns SET status = :status, closedAt = :closedAt, slot = NULL WHERE id = :id")
    suspend fun close(id: Long, status: String, closedAt: String)

    @Query("UPDATE campaigns SET status = 'ACTIVE', closedAt = NULL, slot = NULL WHERE id = :id")
    suspend fun reopen(id: Long)

    /** Room clears daily_tasks.campaignId for us — see the entity's ON DELETE. */
    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun delete(id: Long)
}
