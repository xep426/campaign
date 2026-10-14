package io.github.xep426.campaign.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * One row of the widget: a pinned campaign and what it wants next.
 *
 * [stepId] is what makes the row actionable — [ToggleTaskAction] gets
 * nothing but an ActionParameters bundle across the process boundary, so
 * the row has to carry the identity of the thing it will write to. It is
 * null when the campaign has no open step, and the row then opens the app
 * instead, because there is nothing to tick.
 */
data class WidgetSlot(
    val campaignTitle: String,
    val stepId: Long?,
    val stepTitle: String?,
    /** Days since this campaign last moved — the card's quiet reproach. */
    val quietDays: Long,
)

/**
 * Hilt cannot inject a Glance widget — it is constructed by the framework,
 * not by us — so the graph is reached through an entry point instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun campaignRepository(): CampaignRepository
    fun stepRepository(): StepRepository
}

/**
 * The pinned campaigns, positioned.
 *
 * Index is the slot, so a campaign pinned to III draws third even with I
 * and II free — the widget shows the same three places the app does, or
 * the two surfaces disagree about which one is "the second".
 *
 * The day rule this used to carry is gone with the day. The only date left
 * is today's, and only so a row can say how long it has been quiet.
 */
fun pinnedSlotsFlow(context: Context, today: LocalDate): Flow<List<WidgetSlot?>> {
    val repository = EntryPointAccessors
        .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .campaignRepository()

    return repository.active().map { campaigns ->
        (0 until Campaign.SLOTS).map { slot ->
            campaigns.firstOrNull { it.slot == slot }?.let { campaign ->
                val next = campaign.nextStep
                WidgetSlot(
                    campaignTitle = campaign.title,
                    stepId = next?.id,
                    stepTitle = next?.title,
                    quietDays = campaign.daysSinceProgress(today),
                )
            }
        }
    }
}

/** First emission only, for the frame drawn before the flow is collected. */
suspend fun loadPinnedSlots(context: Context, today: LocalDate): List<WidgetSlot?> =
    pinnedSlotsFlow(context, today).first()

/**
 * Manifest-registered entry point — the system talks to this receiver;
 * Glance renders [CampaignWidget].
 */
class CampaignWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CampaignWidget()
}
