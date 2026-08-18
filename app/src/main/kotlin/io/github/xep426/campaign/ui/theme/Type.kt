package io.github.xep426.campaign.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════
//  Campaign's type scale. Hierarchy comes from a FAMILY SWITCH — serif
//  headings against sans body — rather than from weight, which is why
//  almost everything here sits at Normal. Two families doing the work
//  means nothing has to shout.
// ═══════════════════════════════════════════════════════════════════════

/**
 * The uppercase, wide-tracked mono label: "TUESDAY EVENING", "3 ACTIVE",
 * "NEXT STEP". Call sites pair it with [Ember] or [Muted] and uppercase
 * the string themselves — the style does not transform text, so the
 * strings.xml value stays readable to a translator.
 */
val MonoLabel = TextStyle(
    fontFamily = MonoFamily,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 16.sp,
    letterSpacing = 2.2.sp,
)

/** Campaign metadata: "STARTED 12 AUG · 7 DAYS · 5 STEPS". */
val MonoMeta = TextStyle(
    fontFamily = MonoFamily,
    fontSize = 10.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 15.sp,
    letterSpacing = 1.1.sp,
)

/** A completed task's title. The strike is the whole point. */
val TaskDone = TextStyle(
    fontFamily = BodyFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 22.sp,
    textDecoration = TextDecoration.LineThrough,
)

val CampaignTypography = Typography(
    // ── Display — the end-of-day question, and nothing else ──────────
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 38.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 43.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    // ── Headlines — screen titles ("Today", "Campaigns", "History") ──
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Campaign card titles.
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    ),
    // ── Titles ──────────────────────────────────────────────────────
    // The roman slot numerals. Serif, because I / II / III set in a sans
    // read as the letter i and a pair of ones.
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.6.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    // ── Body — task titles live at bodyLarge ─────────────────────────
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Light,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    // ── Labels — buttons and chips ───────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
    labelSmall = MonoLabel,
)
