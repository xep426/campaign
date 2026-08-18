package io.github.xep426.campaign.data

import androidx.room.withTransaction
import io.github.xep426.campaign.data.db.CampaignDatabase
import io.github.xep426.campaign.data.db.DailyTaskDao
import io.github.xep426.campaign.data.db.DailyTaskEntity
import io.github.xep426.campaign.domain.model.DailyTask

import io.github.xep426.campaign.domain.repository.TaskRepository
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val db: CampaignDatabase,
    private val dao: DailyTaskDao,
    private val widget: WidgetRefresher,
) : TaskRepository {

    override fun tasksFor(date: LocalDate): Flow<List<DailyTask>> =
        dao.observeDay(date.toString()).map { rows -> rows.map { it.toDomain() } }

    override fun completedStandalone(limit: Int): Flow<List<DailyTask>> =
        dao.observeCompletedStandalone(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun setSlot(
        date: LocalDate,
        slot: Int,
        title: String,
        campaignId: Long?,
    ) {
        require(slot in 0 until DailyTask.SLOTS_PER_DAY) { "slot out of range: $slot" }
        val iso = date.toString()
        val trimmed = title.trim()
        val existing = dao.atSlot(iso, slot)

        when {
            // Blank clears. The entry screen has no delete button — emptying
            // the field IS the delete, because a row of three text fields
            // with three little bins beside them is a form, and this screen
            // is trying not to be one.
            trimmed.isEmpty() -> if (existing != null) dao.clearSlot(iso, slot)

            existing == null -> dao.insert(
                DailyTaskEntity(
                    title = trimmed,
                    date = iso,
                    slot = slot,
                    campaignId = campaignId,
                )
            )

            // Re-typing an existing slot keeps its completion (see
            // DailyTaskDao.updateTitle) and its id, so history and any
            // campaign attribution survive an edit.
            else -> dao.updateTitle(existing.id, trimmed, campaignId ?: existing.campaignId)
        }
        widget.refresh()
    }

    override suspend fun setSlotAndCampaign(
        date: LocalDate,
        slot: Int,
        title: String,
        campaignId: Long?,
    ) {
        require(slot in 0 until DailyTask.SLOTS_PER_DAY) { "slot out of range: $slot" }
        val iso = date.toString()
        val trimmed = title.trim()
        val existing = dao.atSlot(iso, slot)

        when {
            trimmed.isEmpty() -> if (existing != null) dao.clearSlot(iso, slot)

            existing == null -> dao.insert(
                DailyTaskEntity(
                    title = trimmed,
                    date = iso,
                    slot = slot,
                    campaignId = campaignId,
                )
            )

            else -> dao.updateTitle(existing.id, trimmed, campaignId)
        }
        widget.refresh()
    }

    override suspend fun toggleCompleted(id: Long): Boolean {
        val task = dao.byId(id) ?: return false
        val next = !task.completed
        dao.setCompleted(id, next)
        widget.refresh()
        return next
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
        widget.refresh()
    }

    override suspend fun firstFreeSlot(date: LocalDate): Int? {
        val taken = dao.occupiedSlots(date.toString()).toSet()
        return (0 until DailyTask.SLOTS_PER_DAY).firstOrNull { it !in taken }
    }

    override suspend fun reorder(date: LocalDate, from: Int, to: Int) {
        val range = 0 until DailyTask.SLOTS_PER_DAY
        if (from !in range || to !in range || from == to) return
        val iso = date.toString()

        db.withTransaction {
            // The three positions as they stand, gaps and all.
            val positions = arrayOfNulls<DailyTaskEntity>(DailyTask.SLOTS_PER_DAY)
            dao.tasksOn(iso).forEach { if (it.slot in range) positions[it.slot] = it }

            val moved = positions.toMutableList()
            moved.add(to, moved.removeAt(from))

            moved.forEachIndexed { index, task ->
                task?.let { if (it.slot != index) dao.setSlotOnly(it.id, index) }
            }
        }
        widget.refresh()
    }
}
