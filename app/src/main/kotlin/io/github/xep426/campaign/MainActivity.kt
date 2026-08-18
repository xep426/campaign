package io.github.xep426.campaign

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Asked for once, on first launch, and never again if declined.
     *
     * There is no rationale dialog and no second ask. The end-of-day
     * notification is the app's one nudge; a user who says no to it has
     * said something clear, and an app built on "three things, no noise"
     * that then nags for permission would be contradicting itself on the
     * first screen. Everything still works without it — the evening screen
     * is reachable from Today.
     */
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent bars: the dusk background runs to both edges, and the
        // system's default scrim would put a lighter band across it.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        askForNotificationsOnce()

        setContent { CampaignApp() }
    }

    // The notification carries no routing any more. Once the day turns at
    // the user's chosen hour, the Tasks tab already IS the empty list the
    // prompt is about — there is nowhere else for it to send anyone.

    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

}
