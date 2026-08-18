package io.github.xep426.campaign.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.xep426.campaign.MainActivity
import io.github.xep426.campaign.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one notification this app sends.
 *
 * WORDING — the PRD's first open question. It asks rather than instructs:
 * "Three things for tomorrow?" not "Time to plan your day!". The
 * difference matters at 21:00, when a demand reads as one more obligation
 * and a question reads as an invitation. There is no streak to protect and
 * nothing is lost by ignoring it, so the copy must not imply otherwise.
 *
 * IMPORTANCE_DEFAULT, not HIGH: it makes a sound and sits in the shade,
 * and does not take over the screen. A heads-up banner for a ritual the
 * user chose the time for would be the app raising its voice at the one
 * moment it should be calm.
 */
@Singleton
class EndOfDayNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun post() {
        if (!hasPermission()) return
        ensureChannel()

        // No routing extras: the day has just turned, so the Tasks tab the
        // app opens on already shows the empty list this prompt is about.
        val open = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        // Guarded above, but the platform check is what actually satisfies
        // the permission requirement on 33+ — and a revoked permission
        // between the two is a throw, not a no-op.
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "end_of_day"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN = 100
    }
}
