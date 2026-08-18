package io.github.xep426.campaign.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.xep426.campaign.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Books the next end-of-day alarm, one day at a time.
 *
 * TWO BUGS ARE FIXED HERE, and both were found in a live `dumpsys alarm`
 * on 2026-08-17 after the 21:00 prompt failed to arrive. They are written
 * down because each one is invisible in code review and obvious in a dump.
 *
 * 1. INEXACT IS NOT "A FEW MINUTES". This class used to claim that
 *    `setAndAllowWhileIdle` fires within a few minutes of the requested
 *    time. It does not. The platform handed the 21:00 alarm a delivery
 *    window of `+1h0m0s0ms` — anywhere up to 22:00. Exact scheduling was
 *    the fix, and Play took it back: the permission is restricted to
 *    clocks and calendars. The window is a fact of life again — see
 *    [bookExactly] and the manifest.
 *
 * 2. OPENING THE APP CANCELLED THE ALARM THAT WAS ABOUT TO FIRE. Every
 *    launch called reschedule(), which asked [nextOccurrence] for the next
 *    21:00 — and at 21:04, with the alarm still pending inside its window,
 *    "next" is TOMORROW. The pending alarm was replaced and that evening's
 *    prompt was lost. Worse, it was self-reinforcing: a late notification
 *    makes you open the app to see why, and opening the app is what
 *    guaranteed it would never come. [reschedule] now leaves a live alarm
 *    alone unless the caller has a reason to override it.
 *
 * ONE ALARM AT A TIME, re-booked by the receiver after each fire, rather
 * than setRepeating: a repeating alarm keeps its original time when the
 * user changes the setting, and moving it means cancel-and-rebook anyway.
 */
@Singleton
class EndOfDayScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {

    /**
     * Reads the current setting and books (or cancels) accordingly.
     *
     * [force] must be true whenever the existing booking is known to be
     * wrong or gone — the user moved the time, the alarm just fired, or
     * the device rebooted and took its alarms with it. Left false (the
     * app-launch case) an alarm that is still inside its delivery window
     * is deliberately left where it is.
     */
    suspend fun reschedule(force: Boolean = false) {
        val setting = settings.endOfDay.first()

        if (!setting.enabled) {
            cancel()
            settings.setScheduledFor(NONE)
            return
        }

        if (!force && holdsLiveAlarm()) return

        book(nextOccurrence(setting.time, LocalDateTime.now()))
    }

    fun cancel() {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent())
    }

    /**
     * Is there a booking that has not had its chance yet?
     *
     * Both halves are needed. The stored instant alone can be stale — a
     * reboot drops every alarm without telling us — and the PendingIntent
     * alone says only that one exists, not what time it carries.
     *
     * The comparison is against the whole delivery window, not the target
     * instant, because an inexact alarm booked for 21:00 is still perfectly
     * alive at 21:30.
     */
    private suspend fun holdsLiveAlarm(): Boolean {
        if (existingPendingIntent() == null) return false
        return isStillDeliverable(settings.scheduledFor.first(), System.currentTimeMillis())
    }

    private suspend fun book(at: LocalDateTime) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        bookExactly(alarms, millis)
        settings.setScheduledFor(millis)
    }

    /**
     * INEXACT, and not by preference — Google Play restricts exact alarms
     * to apps whose core function is one (clocks, timers, calendars). See
     * the manifest. The platform was measured handing an inexact alarm a
     * delivery window of a full hour, so the prompt can be that late.
     *
     * ...AndAllowWhileIdle because Doze is the other way this fails
     * silently, and an evening prompt lands exactly when the phone has
     * been sitting untouched on a table for an hour.
     *
     * If punctuality ever has to be bought back, `setAlarmClock` is exact
     * without the restricted permission — at the price of a permanent
     * alarm icon in the status bar, which is loud for a daily nudge.
     */
    private fun bookExactly(alarms: AlarmManager, millis: Long) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, EndOfDayReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Null when nothing is booked — FLAG_NO_CREATE only ever looks. */
    private fun existingPendingIntent(): PendingIntent? = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, EndOfDayReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val REQUEST_ALARM = 200
        private const val NONE = 0L

        /**
         * How long a booked alarm stays "still coming".
         *
         * An hour, because that is the window the platform was observed to
         * grant an inexact alarm — measured, not guessed. Holding a
         * too-short grace here would reintroduce the cancellation bug for
         * anyone whose alarm is running inexact.
         */
        private const val DELIVERY_WINDOW_MILLIS = 60L * 60L * 1000L

        /**
         * Is an alarm booked for [bookedAt] still worth leaving alone?
         *
         * Pure, and separated from the Context work above precisely because
         * this is the rule that failed: it is the difference between the
         * evening prompt arriving and the app quietly eating it.
         *
         * A target in the FUTURE gives a negative age and is trivially
         * still deliverable; the interesting case is the few minutes after
         * the target has passed, when the alarm has not fired yet but
         * [nextOccurrence] would already point at tomorrow.
         */
        fun isStillDeliverable(bookedAt: Long, now: Long): Boolean =
            bookedAt > NONE && now - bookedAt < DELIVERY_WINDOW_MILLIS

        /**
         * Today at [time] if it is still ahead, otherwise tomorrow.
         *
         * The boundary case is the one that matters: setting the time to
         * 21:00 AT 21:00 must book tomorrow, not fire immediately and then
         * book tomorrow anyway. `isAfter` on the exact minute puts it in
         * the past branch, which is the quiet behaviour.
         */
        fun nextOccurrence(time: LocalTime, now: LocalDateTime): LocalDateTime {
            val todayAt = LocalDateTime.of(now.toLocalDate(), time)
            return if (todayAt.isAfter(now)) todayAt
            else LocalDateTime.of(now.toLocalDate().plusDays(1), time)
        }

        /** Convenience for tests and for reading the code out loud. */
        fun nextOccurrence(time: LocalTime, onDate: LocalDate, at: LocalTime): LocalDateTime =
            nextOccurrence(time, LocalDateTime.of(onDate, at))
    }
}
