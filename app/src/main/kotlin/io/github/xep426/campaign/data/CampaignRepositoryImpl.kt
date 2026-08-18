package io.github.xep426.campaign.data

import androidx.room.withTransaction
import io.github.xep426.campaign.data.db.CampaignDao
import io.github.xep426.campaign.data.db.CampaignDatabase
import io.github.xep426.campaign.data.db.CampaignEntity
import io.github.xep426.campaign.data.db.DailyTaskDao
import io.github.xep426.campaign.data.db.DailyTaskEntity
import io.github.xep426.campaign.data.db.toDomain
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepositoryImpl @Inject constructor(
    private val db: CampaignDatabase,
    private val campaigns: CampaignDao,
    private val tasks: DailyTaskDao,
    private val widget: WidgetRefresher,
) : CampaignRepository {

    override fun active(): Flow<List<Campaign>> = withSteps(campaigns.observeActive())

    override fun finished(): Flow<List<Campaign>> = withSteps(campaigns.observeFinished())

    override fun byId(id: Long): Flow<Campaign?> =
        combine(campaigns.observeById(id), campaigns.observeStepCounts()) { entity, counts ->
            entity?.toDomain(counts.firstOrNull { it.campaignId == entity.id }?.steps ?: 0)
        }

    /**
     * Step counts arrive as their own flow and are stitched on here rather
     * than fetched per card. One group-by over the whole table costs the
     * same as one count for a single campaign, and this way a task
     * completed on the Today screen updates every card's tally at once.
     */
    private fun withSteps(source: Flow<List<CampaignEntity>>): Flow<List<Campaign>> =
        combine(
            source,
            campaigns.observeStepCounts(),
            tasks.observeOpen(),
        ) { entities, counts, open ->
            val bySteps = counts.associate { it.campaignId to it.steps }
            val byCampaign = open.map { it.toDomain() }.groupBy { it.campaignId }
            entities.map { it.toDomain(bySteps[it.id] ?: 0, byCampaign[it.id].orEmpty()) }
        }

    override suspend fun create(title: String, on: LocalDate): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0L
        return campaigns.insert(
            CampaignEntity(
                title = trimmed,
                createdAt = on.toString(),
                // No next step yet, deliberately. The card asks for one at
                // the moment the user is looking at the campaign, not while
                // they are still naming it.
                nextStep = "",
            )
        )
    }

    override suspend fun promote(taskId: Long, title: String, on: LocalDate): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0L

        val newId = db.withTransaction {
            val task = tasks.byId(taskId) ?: return@withTransaction null
            val id = campaigns.insert(
                CampaignEntity(title = trimmed, createdAt = on.toString(), nextStep = "")
            )
            // Re-links even if the task already belonged somewhere: the
            // user picked "new campaign" while looking at this task, which
            // is a clearer statement of intent than the old link.
            tasks.setCampaign(task.id, id)
            id
        }
        widget.refresh()
        return newId ?: 0L
    }

    override suspend fun assign(taskId: Long, campaignId: Long?) {
        tasks.setCampaign(taskId, campaignId)
        widget.refresh()
    }

    override suspend fun carryForward(taskId: Long, date: LocalDate): Int? {
        val slot = db.withTransaction {
            val task = tasks.byId(taskId) ?: return@withTransaction null

            // Already there. Returning its current slot rather than null
            // keeps "nothing happened" distinct from "it would not fit",
            // which the caller phrases very differently.
            if (task.date == date.toString()) return@withTransaction task.slot

            val taken = tasks.occupiedSlots(date.toString()).toSet()
            val free = (0 until DailyTask.SLOTS_PER_DAY).firstOrNull { it !in taken }
                ?: return@withTransaction null

            tasks.moveTo(taskId, date.toString(), free)
            free
        }
        if (slot != null) widget.refresh()
        return slot
    }

    override suspend fun rename(id: Long, title: String) {
        val trimmed = title.trim()
        // A campaign with no name is unfindable in a list that shows only
        // names, so an empty rename is simply not a rename.
        if (trimmed.isNotEmpty()) {
            campaigns.rename(id, trimmed)
            widget.refresh()
        }
    }

    override suspend fun setNotes(id: Long, notes: String) = campaigns.setNotes(id, notes)

    override suspend fun close(id: Long, status: CampaignStatus, on: LocalDate) {
        require(status != CampaignStatus.ACTIVE) { "close() cannot set ACTIVE — use reopen()" }
        campaigns.close(id, status.name, on.toString())
    }

    override suspend fun reopen(id: Long) = campaigns.reopen(id)

    override suspend fun delete(id: Long) {
        campaigns.delete(id)
        // The days it produced keep their tasks and lose only the tag, so
        // the widget and Today may both be showing a chip that no longer
        // has anything behind it.
        widget.refresh()
    }
}
