package io.github.xep426.campaign.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Progress
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.EmberDeep
import io.github.xep426.campaign.ui.theme.Line
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.MonoLabel
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm

/** Hairline rule. The app's only divider. */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/**
 * A labelled section break: "TODAY, IN REVIEW" with a rule running off to
 * the right. The rule stops the label from floating — and running it to
 * the edge rather than centring the text keeps the page left-aligned all
 * the way down, which is what makes it scannable at a glance.
 */
@Composable
fun SectionDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
    ) {
        Eyebrow(label, Muted)
        Hairline(Modifier.weight(1f))
    }
}

/**
 * The roman slot numeral. Ember when the slot has focus, its deeper shade
 * at rest — the only cue the app gives that a field is live, since a
 * focus ring around a bare line of text would be louder than the text.
 */
@Composable
fun SlotNumeral(
    numeral: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (focused) Ember else EmberDeep,
        animationSpec = tween(220),
        label = "numeral",
    )
    Box(modifier = modifier.width(26.dp)) {
        Text(
            text = numeral,
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}

/**
 * A quiet action: uppercase mono with a short rule in front of it.
 *
 * Deliberately not a button. "Pull from a campaign" and "Add a task" are
 * offers, not instructions, and an outlined button beside an empty slot
 * would read as something the user is supposed to press before the day
 * counts.
 */
@Composable
fun GhostAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Muted,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 0.dp,
            vertical = 4.dp,
        ),
    ) {
        Box(
            Modifier
                .size(width = 12.dp, height = 1.dp)
                .background(if (enabled) tint else Line)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MonoLabel,
            color = if (enabled) tint else Line,
        )
    }
}

/** The one filled button in the app: confirm tomorrow, and nothing else. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Ember,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = LineStrong,
            disabledContentColor = Muted,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action on a campaign card — outlined, never filled. */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Muted,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, if (enabled) LineStrong else Line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tint,
            disabledContentColor = Muted,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/**
 * Two or three mutually exclusive views of the same screen.
 *
 * A switch rather than tabs or a scroll: Today and Tomorrow are the same
 * three slots seen on different days, not different places, and the app's
 * bottom bar already owns "somewhere else".
 */
@Composable
fun SegmentedSwitch(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (on) Color.White.copy(alpha = 0.07f) else Color.Transparent,
                        RoundedCornerShape(7.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MonoLabel,
                    color = if (on) MaterialTheme.colorScheme.onSurface else Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The closing line at the foot of a screen — centred, dim, and the only
 * centred text in the app. It is a caption on the whole page rather than a
 * label on anything, which is why it sits apart from the left rail.
 */
@Composable
fun FootNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = SpaceMd),
    )
}

/**
 * Efficiency over the last thirty days, and everything ever finished.
 *
 * A number and the fraction it came from, and nothing else. No bar, no
 * chart, no streak — a progress bar lived on Today once and was taken out,
 * and a percentage whose arithmetic cannot be checked is the kind of number
 * that starts feeling like a scold.
 *
 * The fraction stays because it is what makes the percentage legible:
 * "62%" is a verdict, "37 of 60 possible" is a fact you can check. See
 * [Progress] for why three a day is the denominator.
 */
@Composable
fun Tally(progress: Progress, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.tally_window, progress.windowDays), Ember)
        Spacer(Modifier.height(SpaceSm))

        if (progress.possible == 0) {
            // Before the first task there is no window to be a fraction of,
            // and 0% would be a judgement on a day that has not happened.
            Text(
                text = stringResource(R.string.tally_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${progress.percent}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Paper,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleMedium,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(
                    stringResource(
                        R.string.tally_of_possible,
                        progress.completedInWindow,
                        progress.possible,
                    ),
                    stringResource(R.string.tally_all_time, progress.completedAllTime),
                ).joinToString(" · ").uppercase(java.util.Locale.getDefault()),
                style = MonoMeta,
                color = Muted,
            )
        }
    }
}
