package io.github.xep426.campaign.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Steps, read by campaign rather than by day.
 *
 * Every query that took a date is gone. What is left reads the whole table
 * in two flows — open and done — which the repository groups by campaign.
 * Two queries for the entire app rather than one per card is the same
 * trade the old code made, and it still holds: a step finished anywhere
 * updates every surface at once, and a personal database has hundreds of
 * rows, not millions.
 */
@Dao
interface StepDao {

    /**
     * Everything still to do, in the order campaigns present it.
     *
     * (position, id) rather than position alone: existing rows were all
     * backfilled to position 0 by MIGRATION_3_4, so id is what gives them
     * a stable creation order underneath.
     */
    @Query(
        """
        SELECT t.*, c.title AS campaignTitle
        FROM daily_tasks t
        LEFT JOIN campaigns c ON c.id = t.campaignId
        WHERE t.completed = 0
        ORDER BY t.position ASC, t.id ASC
        """
    )
    fun observeOpen(): Flow<List<StepWithCampaign>>

    /** Everything done, most recently finished first. */
    @Query(
        """
        SELECT t.*, c.title AS campaignTitle
        FROM daily_tasks t
        LEFT JOIN campaigns c ON c.id = t.campaignId
        WHERE t.completed = 1
        ORDER BY t.completedOn DESC, t.id DESC
        """
    )
    fun observeDone(): Flow<List<StepWithCampaign>>

    /**
     * Finished steps whose campaign is gone — the orphans of a deletion.
     *
     * History still shows them, because what was done was still done; the
     * ON DELETE SET NULL on the foreign key is what leaves them here
     * rather than taking them with the campaign.
     */
    @Query(
        """
        SELECT t.*, NULL AS campaignTitle
        FROM daily_tasks t
        WHERE t.campaignId IS NULL AND t.completed = 1
        ORDER BY t.completedOn DESC, t.id DESC
        LIMIT :limitRows
        """
    )
    fun observeCompletedOrphans(limitRows: Int): Flow<List<StepWithCampaign>>

    @Query("SELECT * FROM daily_tasks WHERE id = :id")
    suspend fun byId(id: Long): StepEntity?

    /** Where a newly added step goes: after everything already there. */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM daily_tasks WHERE campaignId = :campaignId")
    suspend fun nextPosition(campaignId: Long): Int

    @Query("SELECT * FROM daily_tasks WHERE campaignId = :campaignId AND completed = 0 ORDER BY position ASC, id ASC")
    suspend fun openFor(campaignId: Long): List<StepEntity>

    @Insert
    suspend fun insert(step: StepEntity): Long

    @Query("UPDATE daily_tasks SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    /**
     * Completion and its date move together, always.
     *
     * Two columns that can disagree is how a step ends up marked done with
     * no date — invisible to the staleness count, which is the one number
     * the app now leans on. Passing null when un-completing is the same
     * rule read backwards.
     */
    @Query("UPDATE daily_tasks SET completed = :completed, completedOn = :completedOn WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedOn: String?)

    @Query("UPDATE daily_tasks SET campaignId = :campaignId WHERE id = :id")
    suspend fun setCampaign(id: Long, campaignId: Long?)

    @Query("UPDATE daily_tasks SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
