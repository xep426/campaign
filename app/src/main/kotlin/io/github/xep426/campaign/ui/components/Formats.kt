package io.github.xep426.campaign.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter

/**
 * A date formatter built from a LOCALISED pattern.
 *
 * The pattern is a string resource, not a constant, because word order and
 * punctuation are part of the translation: "Wednesday 19 August" against
 * "Mittwoch, 19. August". A single hardcoded pattern gives one of the two
 * languages something that reads as a bug.
 *
 * The month and weekday NAMES come from the device locale on their own —
 * `ofPattern` uses `Locale.getDefault()` — so only the shape needs saying.
 */
@Composable
fun rememberDateFormat(@StringRes patternRes: Int): DateTimeFormatter {
    val pattern = stringResource(patternRes)
    return remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
}
