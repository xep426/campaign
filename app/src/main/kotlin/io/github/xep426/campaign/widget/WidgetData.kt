package io.github.xep426.campaign.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidget
import io.github.xep426.campaign.data.settings.SettingsRepository
import io.github.xep426.campaign.domain.model.CampaignDay
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One row of the widget. Null in a slot list means the slot is empty.
 *
 * [id] is what makes the row actionable — [ToggleTaskAction] gets nothing
 * but an ActionParameters bundle across the process boundary, so the row
 * has to carry the identity of the thing it will write to.
 */
data class WidgetSlot(
    val id: Long,
    val title: String,
    val completed: Boolean,
)

/**
 * Hilt cannot inject a Glance widget — it is constructed by the framework,
 * not by us — so the graph is reached through an entry point instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun settingsRepository(): SettingsRepository
}

/**
 * Which day the widget is showing — by the app's rule, not the calendar's.
 *
 * This exists because the widget got it wrong: it kept using
 * `LocalDate.now()` after the app moved its day boundary to the user's
 * chosen hour, so between the turn and midnight the card showed the day
 * that had just ended while the app showed the new one. Two surfaces, two
 * definitions of "today", and the widget is the one people trust because
 * they see it without asking.
 *
 * There is exactly one rule and it lives in [CampaignDay]; everything that
 * needs to know reads it from there.
 */
suspend fun currentCampaignDay(context: Context): WidgetDay {
    val settings = EntryPointAccessors
        .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .settingsRepository()
    val now = LocalDateTime.now()
    val day = CampaignDay.of(now, settings.endOfDay.first().time)
    return WidgetDay(date = day, isPlanning = day != now.toLocalDate())
}

/**
 * The day the widget is showing, and whether it has started yet.
 *
 * [isPlanning] is the same window the app uses: after the turn and before
 * midnight, the three on the card are a plan. Completing one from the home
 * screen would record having finished something on a day that has not
 * begun — the widget must not offer that any more than the app does.
 */
data class WidgetDay(val date: LocalDate, val isPlanning: Boolean)

/**
 * Today's three slots, positioned. Index is the slot, so a task in slot
 * III with slots I and II empty still draws third — the widget shows the
 * same three positions the app does, or the two surfaces would disagree
 * about which task is "the second one".
 */
suspend fun loadTodaySlots(context: Context, today: LocalDate): List<WidgetSlot?> =
    todaySlotsFlow(context, today).first()

/**
 * The same three slots, but as a LIVE flow.
 *
 * This exists because the one-shot version was the bug. Loading the slots
 * in `provideGlance` and closing over the result means the composition
 * holds a snapshot: when Glance recomposes an already-running session — a
 * tap on a warm widget — it re-renders the value it captured, so the card
 * redraws with identical content and the toggle looks dead. The redraw was
 * never missing; it was faithfully redrawing yesterday's answer.
 *
 * Collected inside the composition, the Room flow emits on every write and
 * recomposition sees the new value, which is what makes the widget
 * genuinely reactive rather than reactive-looking.
 */
fun todaySlotsFlow(context: Context, today: LocalDate): Flow<List<WidgetSlot?>> {
    val repository = EntryPointAccessors
        .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .taskRepository()

    return repository.tasksFor(today).map { tasks ->
        (0 until DailyTask.SLOTS_PER_DAY).map { slot ->
            tasks.firstOrNull { it.slot == slot }
                ?.let { WidgetSlot(it.id, it.title, it.completed) }
        }
    }
}

/**
 * Manifest-registered entry point — the system talks to this receiver;
 * Glance renders [CampaignWidget].
 */
class CampaignWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CampaignWidget()
}
