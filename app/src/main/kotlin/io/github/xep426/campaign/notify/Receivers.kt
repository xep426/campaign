package io.github.xep426.campaign.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The alarm landed: post the notification, then book tomorrow's.
 *
 * Rescheduling here rather than in the app is what keeps the ritual alive
 * on a phone the user never opens — the app may not run for days, and the
 * chain only breaks if the device reboots, which [BootReceiver] covers.
 */
@AndroidEntryPoint
class EndOfDayReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: EndOfDayNotifier

    @Inject lateinit var scheduler: EndOfDayScheduler

    @Inject lateinit var widget: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        notifier.post()

        // goAsync: reading the setting is a DataStore hop, and a receiver
        // that returns before its coroutine finishes can have its process
        // killed mid-write — which would silently end the daily chain.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // This alarm fires at the exact instant the day turns, so
                // it is also the moment the widget's list becomes wrong.
                // Without this the card would keep yesterday's three until
                // the next 30-minute tick — visibly disagreeing with the
                // app for up to half an hour, right after the prompt told
                // the user to go and choose.
                widget.refresh()

                // force: the alarm we are standing in the ashes of is the
                // one the scheduler would otherwise treat as still live and
                // decline to replace — which would end the chain here.
                scheduler.reschedule(force = true)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Alarms do not survive a reboot, so the chain is re-started here.
 *
 * Without this the notification stops arriving after the first restart and
 * the app looks abandoned rather than broken — the worst kind of failure
 * for a habit tool, because there is nothing to notice.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: EndOfDayScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // force: the reboot took every alarm with it, so whatever
                // instant is still recorded in settings is a ghost.
                scheduler.reschedule(force = true)
            } finally {
                pending.finish()
            }
        }
    }
}
