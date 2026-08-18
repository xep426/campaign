package io.github.xep426.campaign.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════
//  DARK ONLY — and this is a design decision, not an unfinished one.
//
//  The product's centre of gravity is the end-of-day screen: a notification
//  at 21:00, in a dim room, asking what tomorrow is for. A light theme
//  would put a white rectangle in the user's face at exactly that moment.
//  Kalimetra ships both because it is used at breakfast and at dinner;
//  Campaign is used at dusk.
//
//  The corollary is a rule: NO values-night resources anywhere in this app.
//  There is one palette, so a night-qualified resource can only ever
//  introduce a second, contradicting one.
// ═══════════════════════════════════════════════════════════════════════

private val CampaignColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF1B1409),
    primaryContainer = EmberDeep,
    onPrimaryContainer = Paper,
    secondary = Sage,
    onSecondary = Ink,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = SurfaceCard,
    onSurface = Paper,
    // Every "quiet" piece of text in the app resolves here, so it is the
    // most load-bearing entry in the scheme after onSurface.
    onSurfaceVariant = PaperDim,
    surfaceVariant = SurfaceRaised,
    outline = LineStrong,
    outlineVariant = Line,
    scrim = Void,
)

/** Calm radii. Nothing here is a pill — round enough to soften, not to bounce. */
private val CampaignShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun CampaignTheme(content: @Composable () -> Unit) {
    // Light icons in the system bars, always — the app has one palette and
    // it is dark, so there is no case where the system's own night flag
    // should get a say.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CampaignColorScheme,
        typography = CampaignTypography,
        shapes = CampaignShapes,
        content = content,
    )
}
