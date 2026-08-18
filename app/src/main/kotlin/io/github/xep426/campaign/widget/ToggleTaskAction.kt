package io.github.xep426.campaign.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

/**
 * Completes (or un-completes) a task from the home screen.
 *
 * The write goes through the same [io.github.xep426.campaign.domain.repository.TaskRepository]
 * the app uses, which is what makes the two surfaces agree: the repository
 * fires [io.github.xep426.campaign.domain.repository.WidgetRefresher] after every
 * write, so the card redraws itself and an open Today screen moves at the
 * same moment. There is no widget-specific write path to keep in step,
 * because there is no widget-specific write path.
 *
 * NO CONFIRMATION, and none is wanted. A mis-tap here is undone by tapping
 * again — the act is its own inverse, which is the property that makes it
 * safe to put next to a wall of launcher icons in the first place. A
 * confirm dialog launched from a widget would cost more taps than the
 * mistake it prevents.
 */
class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TASK_ID] ?: return

        // A card drawn before the turn still carries toggle actions in its
        // RemoteViews, and the launcher will happily fire one minutes after
        // the day has rolled over. Refusing here rather than trusting the
        // rendering keeps the rule in one place — and the redraw below
        // replaces the stale card with the planning layout, so the tap
        // fixes what it was about to get wrong.
        if (runCatching { currentCampaignDay(context).isPlanning }.getOrDefault(false)) {
            CampaignWidget().update(context, glanceId)
            return
        }

        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .taskRepository()

        repository.toggleCompleted(taskId)

        // Redraw THIS widget explicitly, even though toggleCompleted has
        // already fired the repository's refresh.
        //
        // The refresh is deliberately not trusted here. It goes out through
        // WidgetRefresher.updateAll(), which is fire-and-forget and, on this
        // Samsung, competes with Freecess freezing the app process the
        // moment the tap is handled — observed live in logcat. When that
        // hop loses, the write lands in the database and the card keeps
        // showing the old state, which reads to the user as "the toggle
        // does nothing" while the app quietly disagrees with its own widget.
        //
        // Inside onAction the process is definitionally awake and we hold
        // the exact [glanceId] that was tapped, so this path cannot lose
        // that race. A redundant redraw costs a frame; a missed one costs
        // trust in the surface.
        CampaignWidget().update(context, glanceId)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<Long>("campaign.task_id")
    }
}
