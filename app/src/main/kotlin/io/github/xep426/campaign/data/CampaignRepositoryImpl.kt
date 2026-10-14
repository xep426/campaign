package io.github.xep426.campaign.data

import androidx.room.withTransaction
import io.github.xep426.campaign.data.db.CampaignDao
import io.github.xep426.campaign.data.db.CampaignDatabase
import io.github.xep426.campaign.data.db.CampaignEntity
import io.github.xep426.campaign.data.db.StepDao
import io.github.xep426.campaign.data.db.toDomain
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepositoryImpl @Inject constructor(
    private val db: CampaignDatabase,
    private val campaigns: CampaignDao,
    private val steps: StepDao,
    private val widget: WidgetRefresher,
) : CampaignRepository {

    override fun active(): Flow<List<Campaign>> = assemble(campaigns.observeActive())

    override fun finished(): Flow<List<Campaign>> = assemble(campaigns.observeFinished())

    /**
     * Both step lists arrive as their own flows and are stitched on here
     * rather than fetched per card. Two queries over the whole table cost
     * what two per campaign would cost for a single one, and this way a
     * step finished on the widget updates every card at once.
     */
    private fun assemble(source: Flow<List<CampaignEntity>>): Flow<List<Campaign>> =
        combine(
            source,
            steps.observeOpen(),
            steps.observeDone(),
        ) { entities, open, done ->
            val openBy = open.map { it.toDomain() }.groupBy { it.campaignId }
            val doneBy = done.map { it.toDomain() }.groupBy { it.campaignId }
            entities
                .map { it.toDomain(openBy[it.id].orEmpty(), doneBy[it.id].orEmpty()) }
                // Pinned first, in slot order, then the rest as the query
                // gave them. Sorting here rather than in SQL because the
                // slot is on the campaign but the ordering the screens
                // want is one list, not two queries stitched together.
                .sortedBy { it.slot ?: Int.MAX_VALUE }
        }

    override suspend fun create(title: String, on: LocalDate): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0L
        val id = campaigns.insert(
            CampaignEntity(
                title = trimmed,
                createdAt = on.toString(),
                nextStep = "",
                // Not pinned on creation. Making room is a decision about
                // what to stop pushing, and taking it on the user's behalf
                // at the moment they are typing a name is the wrong time
                // to ask — the campaigns screen offers it afterwards.
                slot = null,
            )
        )
        return id
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

    override suspend fun pin(id: Long): Int? {
        val slot = db.withTransaction {
            val taken = campaigns.occupiedSlots().toSet()
            val free = (0 until Campaign.SLOTS).firstOrNull { it !in taken }
                ?: return@withTransaction null
            campaigns.setSlot(id, free)
            free
        }
        if (slot != null) widget.refresh()
        return slot
    }

    override suspend fun unpin(id: Long) {
        campaigns.setSlot(id, null)
        widget.refresh()
    }

    override suspend fun complete(id: Long, on: LocalDate) {
        campaigns.close(id, CampaignStatus.COMPLETED.name, on.toString())
        // Closing frees a slot, so the questlog and the widget both have
        // a gap where this was.
        widget.refresh()
    }

    override suspend fun reopen(id: Long) {
        campaigns.reopen(id)
        widget.refresh()
    }

    override suspend fun delete(id: Long) {
        campaigns.delete(id)
        // Finished steps survive the deletion with a null campaignId — the
        // foreign key sets rather than cascades — so history keeps them
        // while the widget loses a card.
        widget.refresh()
    }
}
