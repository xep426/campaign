package io.github.xep426.campaign.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.xep426.campaign.R
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.Line
import io.github.xep426.campaign.ui.theme.MonoLabel
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SurfaceCard
import java.time.LocalTime

/** The shared dialog shell: one surface, one hairline, generous padding. */
@Composable
private fun DialogCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(SpaceLg),
        verticalArrangement = Arrangement.spacedBy(SpaceMd),
    ) { content() }
}

/** One line of text, asked for plainly. Renaming a campaign, mostly. */
@Composable
fun TextInputDialog(
    label: String,
    initial: String,
    placeholder: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Eyebrow(label, Ember)
            SlotTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = placeholder,
                imeAction = ImeAction.Done,
                onImeAction = { onConfirm(text) },
            )
            Hairline()
            Spacer(Modifier.height(SpaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                QuietButton(stringResource(R.string.dialog_cancel), onDismiss, Modifier.weight(1f))
                QuietButton(
                    text = confirmText,
                    onClick = { onConfirm(text) },
                    modifier = Modifier.weight(1f),
                    tint = Ember,
                )
            }
        }
    }
}

/**
 * A destructive act, stated plainly.
 *
 * The body says what SURVIVES, not only what goes — the fear that stops
 * someone deleting a mis-made campaign is that it will take their days
 * with it, and that fear is the thing worth answering here rather than in
 * a help page nobody opens.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Paper,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(Modifier.height(SpaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                QuietButton(stringResource(R.string.dialog_cancel), onDismiss, Modifier.weight(1f))
                QuietButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    tint = Ember,
                )
            }
        }
    }
}

/**
 * When the evening asks.
 *
 * Hand-built rather than Material's TimePicker, which is a clock face —
 * good for "when is my flight", oversized for a setting the user touches
 * once. Minutes step in fives because nobody's evening ritual needs 21:47,
 * and a coarse step makes the control usable with a thumb.
 */
@Composable
fun TimeDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute - initial.minute % MINUTE_STEP) }

    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Eyebrow(stringResource(R.string.dialog_time_title), Ember)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberStepper(
                    value = hour,
                    onChange = { hour = (it + 24) % 24 },
                    format = { "%02d".format(it) },
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayMedium,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = SpaceSm),
                )
                NumberStepper(
                    value = minute,
                    onChange = { minute = (it + 60) % 60 },
                    step = MINUTE_STEP,
                    format = { "%02d".format(it) },
                )
            }

            Spacer(Modifier.height(SpaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                QuietButton(stringResource(R.string.dialog_cancel), onDismiss, Modifier.weight(1f))
                QuietButton(
                    text = stringResource(R.string.dialog_set),
                    onClick = { onConfirm(LocalTime.of(hour, minute)) },
                    modifier = Modifier.weight(1f),
                    tint = Ember,
                )
            }
        }
    }
}

@Composable
private fun NumberStepper(
    value: Int,
    onChange: (Int) -> Unit,
    format: (Int) -> String,
    step: Int = 1,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepperArrow("▲") { onChange(value + step) }
        Text(
            text = format(value),
            style = MaterialTheme.typography.displayMedium,
            color = Paper,
            modifier = Modifier.padding(vertical = SpaceXsLocal),
        )
        StepperArrow("▼") { onChange(value - step) }
    }
}

@Composable
private fun StepperArrow(glyph: String, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = MonoLabel,
        color = Muted,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .width(24.dp),
    )
}

private val SpaceXsLocal = 4.dp
private const val MINUTE_STEP = 5
