package io.github.xep426.campaign.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Glance side of [WidgetRefresher]. Repositories call `refresh()`
 * after every write, so the home screen never shows a state the app has
 * already moved past — §7 asks for a widget that "updates when tasks
 * change", and 30-minute polling alone would make that a lie for up to
 * half an hour after every tap.
 */
@Singleton
class GlanceWidgetRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WidgetRefresher {

    override suspend fun refresh() {
        // Still swallowed, because a user with no widget placed must not
        // see an app write fail — but LOGGED, which it was not.
        //
        // The silent version cost a debugging session: taps on the widget
        // wrote to the database and the card did not redraw, and because
        // this was `runCatching { }` with an empty else, there was nothing
        // in logcat to say whether the refresh had even been attempted.
        // A swallowed exception is a decision to never find out.
        runCatching { CampaignWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "Widget refresh failed", it) }
    }

    private companion object {
        const val TAG = "CampaignWidget"
    }
}
