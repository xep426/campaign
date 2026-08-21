package io.github.xep426.campaign.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {

    @Query(
        """
        SELECT t.*, c.title AS campaignTitle
        FROM daily_tasks t
        LEFT JOIN campaigns c ON c.id = t.campaignId
        WHERE t.date = :date
        ORDER BY t.slot ASC
        """
    )
    fun observeDay(date: String): Flow<List<TaskWithCampaign>>

    /**
     * Finished tasks that belong to no campaign — the standalone wins.
     *
     * Excludes campaign tasks on purpose: those are already represented by
     * the campaign they served, and listing them twice would make a single
     * effort look like a pile of separate achievements.
     */
    @Query(
        """
        SELECT t.*, NULL AS campaignTitle
        FROM daily_tasks t
        WHERE t.completed = 1 AND t.campaignId IS NULL
        ORDER BY t.date DESC, t.slot ASC
        LIMIT :limitRows
        """
    )
    fun observeCompletedStandalone(limitRows: Int): Flow<List<TaskWithCampaign>>

    @Query("SELECT * FROM daily_tasks WHERE id = :id")
    suspend fun byId(id: Long): DailyTaskEntity?

    @Query("SELECT * FROM daily_tasks WHERE date = :date AND slot = :slot")
    suspend fun atSlot(date: String, slot: Int): DailyTaskEntity?

    @Query("SELECT slot FROM daily_tasks WHERE date = :date ORDER BY slot ASC")
    suspend fun occupiedSlots(date: String): List<Int>

    @Query("SELECT * FROM daily_tasks WHERE date = :date ORDER BY slot ASC")
    suspend fun tasksOn(date: String): List<DailyTaskEntity>

    /**
     * Slot only — the one field a reorder touches.
     *
     * Separate from [updateTitle] because reordering says nothing about
     * what a task is or whether it is done, and a write that carried those
     * along would let a stale copy overwrite the real one.
     */
    @Query("UPDATE daily_tasks SET slot = :slot WHERE id = :id")
    suspend fun setSlotOnly(id: Long, slot: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DailyTaskEntity): Long

    /**
     * Title and campaign link only — deliberately NOT completion.
     *
     * Editing the wording of a task must not silently un-complete it, and
     * a single "update everything" write is how that happens: the entry
     * screen holds a stale `completed` from whenever it composed, and
     * saves it back over the tap the user made a second ago.
     */
    @Query("UPDATE daily_tasks SET title = :title, campaignId = :campaignId WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, campaignId: Long?)

    @Query("UPDATE daily_tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    /**
     * The campaign link alone — title and completion untouched.
     *
     * Assigning a task to a campaign says nothing about its wording or
     * whether it is done, and a write that carried those along would let a
     * stale copy of either overwrite the real one.
     */
    @Query("UPDATE daily_tasks SET campaignId = :campaignId WHERE id = :id")
    suspend fun setCampaign(id: Long, campaignId: Long?)

    /**
     * Every incomplete task that belongs to a campaign, oldest day first.
     *
     * This is what a campaign IS now — its outstanding work — so it is one
     * query for all of them rather than one per card, and the campaign
     * repository stitches them on by id.
     */
    @Query(
        """
        SELECT t.*, c.title AS campaignTitle
        FROM daily_tasks t
        LEFT JOIN campaigns c ON c.id = t.campaignId
        WHERE t.campaignId IS NOT NULL AND t.completed = 0
        ORDER BY t.date ASC, t.slot ASC
        """
    )
    fun observeOpen(): Flow<List<TaskWithCampaign>>

    /**
     * Completed tasks per campaign — what the campaign card now shows.
     *
     * Only completed rows, which is the point: pulling a step in and never
     * doing it is not progress, and a list that said otherwise would be
     * flattering the user rather than informing them.
     *
     * Newest first, the opposite of [observeOpen]. Outstanding work is
     * read oldest-first because age is the argument for doing it; finished
     * work is read newest-first because the recent steps are the ones that
     * say where the campaign stands.
     */
    @Query(
        """
        SELECT t.*, c.title AS campaignTitle
        FROM daily_tasks t
        LEFT JOIN campaigns c ON c.id = t.campaignId
        WHERE t.campaignId IS NOT NULL AND t.completed = 1
        ORDER BY t.date DESC, t.slot ASC
        """
    )
    fun observeDone(): Flow<List<TaskWithCampaign>>

    /** Every task ever finished. The counter that only goes up. */
    @Query("SELECT COUNT(*) FROM daily_tasks WHERE completed = 1")
    fun observeCompletedTotal(): Flow<Int>

    /**
     * Finished on or after [from].
     *
     * A string comparison, which works because date is ISO-8601 and
     * therefore sorts as text — see [DailyTaskEntity].
     */
    @Query("SELECT COUNT(*) FROM daily_tasks WHERE completed = 1 AND date >= :from")
    fun observeCompletedSince(from: String): Flow<Int>

    /**
     * The first day the app was ever used, or null on an empty database.
     *
     * This is what stops the thirty-day window from measuring a new user
     * against slots that did not exist yet.
     */
    @Query("SELECT MIN(date) FROM daily_tasks")
    fun observeFirstDate(): Flow<String?>

    /**
     * Re-slots a task onto another day.
     *
     * Carrying an unfinished task forward MOVES it — one task is slotted to
     * one day at a time (author's decision). The day it came from stops
     * listing it, which is the cost of not accumulating duplicates.
     */
    @Query("UPDATE daily_tasks SET date = :date, slot = :slot WHERE id = :id")
    suspend fun moveTo(id: Long, date: String, slot: Int)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM daily_tasks WHERE date = :date AND slot = :slot")
    suspend fun clearSlot(date: String, slot: Int)
}
