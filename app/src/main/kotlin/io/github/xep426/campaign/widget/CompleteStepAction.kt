package io.github.xep426.campaign.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate

/**
 * Finishes a campaign's next step from the home screen.
 *
 * The write goes through the same [io.github.xep426.campaign.domain.repository.StepRepository]
 * the app uses, which is what makes the two surfaces agree: the repository
 * fires [io.github.xep426.campaign.domain.repository.WidgetRefresher] after every
 * write, so the card redraws itself and an open questlog moves at the same
 * moment. There is no widget-specific write path to keep in step, because
 * there is no widget-specific write path.
 *
 * ONE-WAY, unlike the toggle it replaced. A step's successor takes its
 * place on the card the instant it is finished, so tapping again would act
 * on a different step rather than undo the last one — the act stopped
 * being its own inverse when the row stopped being a fixed task. Undo
 * lives in the app, where the step is still visible and nameable.
 *
 * That makes a mis-tap cost more than it used to, which is the honest
 * price of the row showing a queue instead of a fixed three.
 */
class CompleteStepAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val stepId = parameters[STEP_ID] ?: return

        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .stepRepository()

        repository.setCompleted(stepId, completed = true, on = LocalDate.now())

        // Redrawn here rather than left to the repository's refresh, which
        // loses a race against Samsung's Freecess freezing the app process
        // — observed in logcat, with the write landing in the database
        // while the card kept the old state.
        CampaignWidget().update(context, glanceId)
    }

    companion object {
        val STEP_ID = ActionParameters.Key<Long>("stepId")
    }
}
