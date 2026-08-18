package io.github.xep426.campaign.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════
//  The dusk palette — lifted from the approved HTML mock (campaign-mock.html).
//
//  Warm near-black rather than neutral grey: every value below carries a
//  little red and yellow, so the screen reads as lamplight rather than as
//  a switched-off monitor. It is the whole reason the end-of-day screen
//  feels like an evening ritual instead of a form.
//
//  ONE accent (Ember) plus one state colour (Sage, and only for "done").
//  A second decorative accent would immediately start competing for the
//  attention that the three tasks are supposed to have.
//
//  Raw hex belongs HERE and nowhere else — screens reference the tokens
//  through MaterialTheme.colorScheme or the CampaignColors extras below.
// ═══════════════════════════════════════════════════════════════════════

/** Page ground. Nearly black, warmed. */
val Void = Color(0xFF0E0C0A)

/** Screen background — a shade above [Void], where content sits. */
val Ink = Color(0xFF15120E)

/**
 * Raised surfaces: campaign cards, sheets.
 *
 * Named `SurfaceCard`, not `Surface`, so it cannot sit in the same file as
 * Material's `Surface` composable and leave a reader guessing which one a
 * bare `Surface` means.
 */
val SurfaceCard = Color(0xFF1D1913)

/** Pressed/selected surface, and the widget card. */
val SurfaceRaised = Color(0xFF262019)

/** Primary text. Warm off-white — never pure #FFF, which glares at night. */
val Paper = Color(0xFFF0E9DB)

/** Secondary text: dates, campaign metadata, resolved history entries. */
val PaperDim = Color(0xFFA79D8B)

/** Tertiary text: captions, placeholders, everything deliberately quiet. */
val Muted = Color(0xFF736A5C)

/** The accent. Campaign tags, slot numerals under focus, the confirm button. */
val Ember = Color(0xFFE5913C)

/** Ember's shadow side — slot numerals at rest, button gradient floor. */
val EmberDeep = Color(0xFFB5651F)

/** Done. The only green in the app, and it means exactly one thing. */
val Sage = Color(0xFF93A97E)

/** Hairline rules and input underlines: paper at 10%. */
val Line = Color(0x1AF0E9DB)

/** The same hairline where it has to carry a shape — checkbox rings, chips. */
val LineStrong = Color(0x2EF0E9DB)
