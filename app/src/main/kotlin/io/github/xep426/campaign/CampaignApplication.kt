package io.github.xep426.campaign

import android.app.Application
import io.github.xep426.campaign.notify.EndOfDayNotifier
import io.github.xep426.campaign.notify.EndOfDayScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CampaignApplication : Application() {

    @Inject lateinit var scheduler: EndOfDayScheduler

    @Inject lateinit var notifier: EndOfDayNotifier

    override fun onCreate() {
        super.onCreate()

        notifier.ensureChannel()

        // Re-book on every launch, not just on first install.
        //
        // The alarm chain is one link at a time (see EndOfDayScheduler), so
        // anything that can break a link — a force-stop, a battery
        // optimiser, an OEM's idea of housekeeping — would otherwise end
        // the ritual permanently and silently. Opening the app is the one
        // signal we reliably get that the user still wants it, and
        // re-booking is cheap enough to do unconditionally.
        CoroutineScope(Dispatchers.Default).launch {
            scheduler.reschedule()
        }
    }
}
