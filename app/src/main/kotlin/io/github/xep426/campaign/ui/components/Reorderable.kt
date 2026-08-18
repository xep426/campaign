package io.github.xep426.campaign.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A short, hand-rolled reorderable column.
 *
 * Hand-rolled because the list is three items and never more: a general
 * reordering library would bring lazy layout, autoscroll and a key system
 * for a column that fits on screen twice over. What it does need, and what
 * a fixed-height implementation would get wrong, is MEASURED rows — a task
 * can be one line or two, with or without a campaign chip, so the distance
 * to the next position is never a constant.
 *
 * LONG-PRESS TO PICK UP, not a drag handle. Every pixel of a task row is
 * already spoken for — tapping completes or edits it — and a handle would
 * add a fourth control to a row that reads as one thing.
 *
 * HANDING OVER TO THE DATA IS THE HARD PART, and it took three attempts.
 * A drop cannot commit and reset in the same breath, because the write is
 * asynchronous: the rows fall back to their old places until the database
 * answers, then swap again. Nor can the local order simply be kept — once
 * the data HAS swapped, applying the permutation on top of it reads as the
 * drag being undone.
 *
 * So the permutation is held and dropped the instant the data supersedes
 * it, and THAT DECISION IS MADE DURING COMPOSITION by comparing [keys].
 * Putting it in a LaunchedEffect is the near-miss that looks correct: an
 * effect runs after the frame it belongs to, so new data met the stale
 * permutation for exactly one frame — a flicker small enough to survive a
 * round of "fixed" and still be visible to the eye.
 */
@Composable
fun ReorderableColumn(
    count: Int,
    keys: List<Any?>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    item: @Composable (index: Int, isDragging: Boolean) -> Unit,
) {
    val identity = remember(count) { List(count) { it } }
    val heights = remember(count) { mutableStateListOf(*Array(count) { 0 }) }
    val order = remember(count) { mutableStateListOf(*Array(count) { it }) }

    var dragging by remember { mutableStateOf<Int?>(null) }

    /** Keys as they stood when the drag was dropped; null when not waiting. */
    var heldFor by remember { mutableStateOf<List<Any?>?>(null) }

    // Animatable rather than a plain Float so the release settles instead
    // of snapping: on drop the row is already in its final position, and
    // gliding the last pixels home is the difference between "placed" and
    // "teleported".
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // THE GESTURE LAMBDAS BELOW OUTLIVE THE COMPOSITION THAT MADE THEM.
    // pointerInput is keyed on [count] alone, so detectDragGesturesAfterLongPress
    // is not rebuilt when the data changes — its closure keeps whatever
    // [keys] and [onMove] were on first composition. Reading those stale
    // values on drop meant heldFor was set to keys from app start, which
    // never matched the current ones, so the hold released instantly and
    // the old order flashed up until the write came back. That was the
    // flicker that survived two fixes aimed at the wrong thing.
    val currentKeys by rememberUpdatedState(keys)
    val currentOnMove by rememberUpdatedState(onMove)

    // Composition-time, not effect-time. See the class doc.
    val display = if (dragging != null || (heldFor != null && heldFor == keys)) {
        order.toList()
    } else {
        identity
    }

    fun resetOrder() {
        for (i in 0 until count) order[i] = i
    }

    // Bookkeeping only — [display] stopped using the permutation in the
    // same frame the data arrived, so this can never cause one.
    LaunchedEffect(keys) {
        if (heldFor != null && heldFor != keys) {
            resetOrder()
            heldFor = null
        }
    }

    // Safety net for a drop the data cannot show — two empty positions
    // swapped, say. Then [keys] never changes and the hold would stick.
    LaunchedEffect(heldFor) {
        if (heldFor != null) {
            delay(600)
            resetOrder()
            heldFor = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        display.forEachIndexed { position, original ->
            val isDragging = dragging == position

            Column(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) offsetY.value else 0f }
                    .onSizeChanged { if (position < heights.size) heights[position] = it.height }
                    .then(
                        if (!enabled) Modifier else Modifier.pointerInput(count) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = position
                                    scope.launch { offsetY.snapTo(0f) }
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    dragging = null
                                    resetOrder()
                                    scope.launch { offsetY.animateTo(0f) }
                                },
                                onDragEnd = {
                                    val from = original
                                    val to = dragging
                                    dragging = null
                                    scope.launch { offsetY.animateTo(0f) }
                                    if (to != null && from != to) {
                                        // Hold the permutation against the
                                        // keys as they are NOW; the moment
                                        // they differ, display switches.
                                        heldFor = currentKeys
                                        currentOnMove(from, to)
                                    } else {
                                        resetOrder()
                                    }
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    val current = dragging
                                        ?: return@detectDragGesturesAfterLongPress
                                    val moving = offsetY.value + amount.y
                                    scope.launch { offsetY.snapTo(moving) }

                                    // Cross when the row has travelled past
                                    // half of its neighbour — measured, not
                                    // assumed, because neighbours differ in
                                    // height.
                                    val next = if (moving > 0) current + 1 else current - 1
                                    if (next in 0 until count) {
                                        val span = heights.getOrElse(next) { 0 }
                                        if (span > 0 && kotlin.math.abs(moving) > span / 2f) {
                                            val tmp = order[current]
                                            order[current] = order[next]
                                            order[next] = tmp
                                            dragging = next
                                            // Carry the leftover so the row
                                            // stays under the finger rather
                                            // than jumping a full step.
                                            val corrected =
                                                moving - if (moving > 0) span else -span
                                            scope.launch { offsetY.snapTo(corrected) }
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    )
            ) {
                item(original, isDragging)
            }
        }
    }
}
