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

    /**
     * Completed tasks per campaign — the "9 steps taken" on each card.
     *
     * Counting only completed rows is the point: pulling a step in and
     * never doing it is not progress, and a card that says otherwise
     * would be flattering the user rather than informing them.
     */
    @Query(
        """
        SELECT campaignId AS campaignId, COUNT(*) AS steps
        FROM daily_tasks
        WHERE campaignId IS NOT NULL AND completed = 1
        GROUP BY campaignId
        """
    )
    fun observeStepCounts(): Flow<List<CampaignStepCount>>

    @Insert
    suspend fun insert(campaign: CampaignEntity): Long

    @Query("UPDATE campaigns SET nextStep = :nextStep WHERE id = :id")
    suspend fun setNextStep(id: Long, nextStep: String)

    @Query("UPDATE campaigns SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE campaigns SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String)

    @Query("UPDATE campaigns SET status = :status, closedAt = :closedAt WHERE id = :id")
    suspend fun close(id: Long, status: String, closedAt: String)

    @Query("UPDATE campaigns SET status = 'ACTIVE', closedAt = NULL WHERE id = :id")
    suspend fun reopen(id: Long)

    /** Room clears daily_tasks.campaignId for us — see the entity's ON DELETE. */
    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun delete(id: Long)
}
