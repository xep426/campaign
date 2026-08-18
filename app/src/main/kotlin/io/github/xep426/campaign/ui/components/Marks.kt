package io.github.xep426.campaign.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.Ink
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.MonoLabel
import io.github.xep426.campaign.ui.theme.Sage
import io.github.xep426.campaign.ui.theme.SpaceXs

/**
 * The completion mark: a hairline ring that fills sage and draws its tick.
 *
 * Drawn rather than iconed, because the tick has to be ANIMATED along its
 * own length — a check that fades in reads as a state change, while one
 * that draws itself reads as an act the user just performed. It is the
 * only flourish in the app and it is on the one interaction that repeats
 * three times a day.
 */
@Composable
fun CompletionMark(
    completed: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 21.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "tick",
    )
    val fill by animateColorAsState(
        targetValue = if (completed) Sage else Color.Transparent,
        animationSpec = tween(durationMillis = 260),
        label = "fill",
    )
    val ring by animateColorAsState(
        targetValue = if (completed) Sage else LineStrong,
        animationSpec = tween(durationMillis = 260),
        label = "ring",
    )

    Canvas(modifier = modifier.size(size)) {
        val d = this.size.minDimension
        val r = d / 2f
        drawCircle(color = fill, radius = r)
        drawCircle(color = ring, radius = r - 0.5f, style = Stroke(width = 1.dp.toPx()))

        if (progress <= 0f) return@Canvas

        // The tick, in the mock's 12×12 coordinates scaled to the ring.
        val u = d / 12f
        val a = Offset(2.6f * u, 6.2f * u)
        val b = Offset(5.0f * u, 8.7f * u)
        val c = Offset(9.6f * u, 3.4f * u)

        val len1 = (b - a).getDistance()
        val len2 = (c - b).getDistance()
        val drawn = progress * (len1 + len2)
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)

        // Short leg first, then the long one — the tick writes itself in
        // the direction a hand would.
        val firstEnd = if (drawn >= len1) b else a + (b - a) * (drawn / len1)
        drawLine(Ink, a, firstEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
        if (drawn > len1) {
            val t = ((drawn - len1) / len2).coerceIn(0f, 1f)
            drawLine(Ink, b, b + (c - b) * t, strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

/**
 * The campaign chip: which longer effort a task belongs to.
 *
 * Ember, and the only place ember appears on a task row — so a glance at
 * Today separates "something I chose this morning" from "the next step of
 * something I have been carrying for three weeks" without reading a word.
 */
@Composable
fun CampaignTag(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Ember,
) {
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceXs),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
        )
    }
}

/** The wide-tracked uppercase mono label. Section headings, dates, counts. */
@Composable
fun Eyebrow(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MonoLabel,
        color = color,
        modifier = modifier,
    )
}

/**
 * Today's progress: three short bars, one per slot.
 *
 * Bars rather than a number, and never a percentage — "67%" invites
 * optimising a figure, which is the gamification §12 rules out. Three
 * marks that fill in are just a picture of the day.
 */
@Composable
fun SlotPips(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .background(
                        if (index < completed) Sage else LineStrong,
                        RoundedCornerShape(2.dp),
                    )
            )
        }
    }
}
