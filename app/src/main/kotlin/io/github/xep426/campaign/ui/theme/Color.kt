package io.github.xep426.campaign.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════
//  TWO PALETTES, ONE SET OF ROLES.
//
//  This file shipped with a single dusk palette and a rule saying there
//  would never be a second one. There is now a second one, by request, and
//  the rule it replaced is worth restating rather than deleting: the app's
//  centre of gravity is a prompt at 22:00 in a dim room, so DUSK IS THE
//  DEFAULT and daylight is the option, not the other way round.
//
//  Both palettes are WARM. Every value carries a little red and yellow, so
//  the dark one reads as lamplight rather than a switched-off monitor and
//  the light one as paper rather than as a browser page. A neutral-grey
//  light theme would have been a different app wearing this one's layout.
//
//  ONE accent (ember) plus one state colour (sage, and only for "done").
//  A second decorative accent would immediately start competing for the
//  attention the three tasks are supposed to have.
//
//  Raw hex belongs HERE and nowhere else. Screens read roles through
//  [AppColors], which resolves against whichever palette is in force.
// ═══════════════════════════════════════════════════════════════════════

/**
 * The colour roles, named for their job rather than their value.
 *
 * A data class rather than two sets of top-level vals, because the whole
 * point is that a screen cannot accidentally name one palette's value
 * while the other is in force — the compiler now has no way to offer it.
 */
@Immutable
data class Palette(
    /** True for dusk. Read by things that must branch, like the system bars. */
    val isDark: Boolean,
    /** Page ground, and the scrim behind dialogs. */
    val void: Color,
    /** Screen background, where content sits. */
    val ink: Color,
    /**
     * Raised surfaces: campaign cards, sheets.
     *
     * Named `surfaceCard`, not `surface`, so it cannot be confused with
     * Material's `Surface` composable at a glance.
     */
    val surfaceCard: Color,
    /** Pressed or selected surface. */
    val surfaceRaised: Color,
    /** Primary text. */
    val paper: Color,
    /** Secondary text: dates, metadata, resolved history entries. */
    val paperDim: Color,
    /** Tertiary text: captions, placeholders, everything deliberately quiet. */
    val muted: Color,
    /** The accent. Campaign tags, numerals under focus, the confirm button. */
    val ember: Color,
    /** Ember's shadow side — numerals at rest. */
    val emberDeep: Color,
    /** Done. The only green in the app, and it means exactly one thing. */
    val sage: Color,
    /** Hairline rules and input underlines. */
    val line: Color,
    /** The same hairline where it has to carry a shape — rings, chips. */
    val lineStrong: Color,
)

/**
 * Dusk. The original palette, and still the default.
 *
 * Warm near-black rather than neutral grey: never pure #FFF on top of it
 * either, which glares in a dark room.
 */
val DuskPalette = Palette(
    isDark = true,
    void = Color(0xFF0E0C0A),
    ink = Color(0xFF15120E),
    surfaceCard = Color(0xFF1D1913),
    surfaceRaised = Color(0xFF262019),
    paper = Color(0xFFF0E9DB),
    paperDim = Color(0xFFA79D8B),
    muted = Color(0xFF736A5C),
    ember = Color(0xFFE5913C),
    emberDeep = Color(0xFFB5651F),
    sage = Color(0xFF93A97E),
    line = Color(0x1AF0E9DB),
    lineStrong = Color(0x2EF0E9DB),
)

/**
 * Daylight. Paper rather than screen.
 *
 * Not an inversion — the accents had to be re-picked rather than reused.
 * Ember at #E5913C is legible on near-black and washes out to nothing on
 * cream, so the light palette takes ember's darker end and pushes it
 * further; sage does the same. The greys move the other way: on paper the
 * quiet text has to be DARKER than you expect, because a light background
 * flatters low contrast and then fails outdoors.
 *
 * Surfaces reverse their direction too. In dusk a card is LIGHTER than the
 * page it sits on; here it is nearer white than the ground is, which is
 * the same relationship — raised means closer to the light.
 */
val DaylightPalette = Palette(
    isDark = false,
    void = Color(0xFFE4D6BE),
    ink = Color(0xFFF7EFE2),
    surfaceCard = Color(0xFFFFFAF0),
    surfaceRaised = Color(0xFFF1E7D6),
    paper = Color(0xFF1A1611),
    paperDim = Color(0xFF574F42),
    muted = Color(0xFF6E6558),
    ember = Color(0xFFB0651A),
    emberDeep = Color(0xFF8F4E15),
    sage = Color(0xFF5F7749),
    line = Color(0x1F1A1611),
    lineStrong = Color(0x3D1A1611),
)

/**
 * Static rather than dynamic: the palette changes on a deliberate tap and
 * nothing else, so there is no reason to pay for reading it as state on
 * every recomposition.
 */
val LocalPalette = staticCompositionLocalOf { DuskPalette }

/**
 * How screens name a colour.
 *
 * `AppColors.paper` rather than a bare `Paper`, because a bare name is a
 * constant and constants cannot follow a theme. The extra six characters
 * are what make the switch possible at all.
 */
object AppColors {
    val void: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.void
    val ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
    val surfaceCard: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceCard
    val surfaceRaised: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceRaised
    val paper: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.paper
    val paperDim: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.paperDim
    val muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
    val ember: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ember
    val emberDeep: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.emberDeep
    val sage: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.sage
    val line: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line
    val lineStrong: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.lineStrong
}
