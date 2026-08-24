package io.github.xep426.campaign.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════
//  DUSK BY DEFAULT, DAYLIGHT BY CHOICE.
//
//  This was dark-only, deliberately, with a rule saying there would never
//  be a second palette: the product's centre of gravity is the end-of-day
//  prompt, in a dim room, and a light theme puts a white rectangle in the
//  user's face at exactly that moment.
//
//  Light mode arrived by request. The reasoning above did not stop being
//  true, which is why dusk stays the default and daylight is something the
//  user reaches for.
//
//  The corollary rule SURVIVES, and matters more now than it did: NO
//  values-night resources anywhere in this app. The palette follows an
//  in-app toggle, not the system night flag, and a night-qualified
//  resource would be a second source of truth disagreeing with it half the
//  time. Anything a drawable needs is set from code.
// ═══════════════════════════════════════════════════════════════════════

private fun schemeFor(p: Palette) = if (p.isDark) {
    darkColorScheme(
        primary = p.ember,
        onPrimary = Color(0xFF1B1409),
        primaryContainer = p.emberDeep,
        onPrimaryContainer = p.paper,
        secondary = p.sage,
        onSecondary = p.ink,
        secondaryContainer = p.surfaceRaised,
        onSecondaryContainer = p.paper,
        background = p.ink,
        onBackground = p.paper,
        surface = p.surfaceCard,
        onSurface = p.paper,
        // Every "quiet" piece of text in the app resolves here, so it is
        // the most load-bearing entry in the scheme after onSurface.
        onSurfaceVariant = p.paperDim,
        surfaceVariant = p.surfaceRaised,
        outline = p.lineStrong,
        outlineVariant = p.line,
        scrim = p.void,
    )
} else {
    lightColorScheme(
        primary = p.ember,
        // Text ON the accent, not the accent itself: near-white with the
        // same warmth, so a filled button does not turn into the only cold
        // rectangle in the app.
        onPrimary = Color(0xFFFFF8EC),
        primaryContainer = p.emberDeep,
        onPrimaryContainer = Color(0xFFFFF8EC),
        secondary = p.sage,
        onSecondary = Color(0xFFFFF8EC),
        secondaryContainer = p.surfaceRaised,
        onSecondaryContainer = p.paper,
        background = p.ink,
        onBackground = p.paper,
        surface = p.surfaceCard,
        onSurface = p.paper,
        onSurfaceVariant = p.paperDim,
        surfaceVariant = p.surfaceRaised,
        outline = p.lineStrong,
        outlineVariant = p.line,
        scrim = p.void,
    )
}

/** Calm radii. Nothing here is a pill — round enough to soften, not to bounce. */
private val CampaignShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun CampaignTheme(
    palette: Palette = DuskPalette,
    content: @Composable () -> Unit,
) {
    // System bar icons follow the palette, not the system night flag. On
    // daylight the bars sit on cream and need dark icons; getting this
    // wrong leaves the clock invisible, which is the sort of bug a user
    // reports as "the status bar broke".
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.isDark
                isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = schemeFor(palette),
            typography = CampaignTypography,
            shapes = CampaignShapes,
            content = content,
        )
    }
}
