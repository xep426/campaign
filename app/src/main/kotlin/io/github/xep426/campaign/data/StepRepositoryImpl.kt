package io.github.xep426.campaign.data

import io.github.xep426.campaign.data.db.StepDao
import io.github.xep426.campaign.data.db.StepEntity
import io.github.xep426.campaign.domain.model.Step
import io.github.xep426.campaign.domain.repository.StepRepository
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepRepositoryImpl @Inject constructor(
    private val dao: StepDao,
    private val widget: WidgetRefresher,
) : StepRepository {

    override fun completedOrphans(limit: Int): Flow<List<Step>> =
        dao.observeCompletedOrphans(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(campaignId: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insert(
            StepEntity(
                title = trimmed,
                // The dead `date` column still has to hold something, and
                // the day it was written is the only honest value for it.
                // Step.addedOn reads it back under its real name.
                date = LocalDate.now().toString(),
                campaignId = campaignId,
                position = dao.nextPosition(campaignId),
            )
        )
        widget.refresh()
    }

    override suspend fun rename(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.updateTitle(id, trimmed)
        widget.refresh()
    }

    override suspend fun setCompleted(id: Long, completed: Boolean, on: LocalDate) {
        dao.setCompleted(id, completed, if (completed) on.toString() else null)
        // Finishing a step is what makes the next one appear on the card,
        // so the widget is wrong until this lands.
        widget.refresh()
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
        widget.refresh()
    }

    override suspend fun move(campaignId: Long, from: Int, to: Int) {
        val open = dao.openFor(campaignId)
        if (from !in open.indices || to !in open.indices || from == to) return

        // Rewrite every position rather than swapping two. The list is a
        // handful of rows, and a full pass cannot leave a gap or a
        // duplicate behind the way a partial one can.
        val reordered = open.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { index, step ->
            if (step.position != index) dao.setPosition(step.id, index)
        }
        widget.refresh()
    }
}
