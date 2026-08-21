package io.github.xep426.campaign.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.ui.components.AssignDialog
import io.github.xep426.campaign.ui.components.CampaignPickerDialog
import io.github.xep426.campaign.ui.components.CampaignTag
import io.github.xep426.campaign.ui.components.CompletionMark
import io.github.xep426.campaign.ui.components.Eyebrow
import io.github.xep426.campaign.ui.components.rememberDateFormat
import io.github.xep426.campaign.ui.components.FootNote
import io.github.xep426.campaign.ui.components.TallyStrip
import io.github.xep426.campaign.ui.components.GhostAction
import io.github.xep426.campaign.ui.components.Hairline
import io.github.xep426.campaign.ui.components.ReorderableColumn
import io.github.xep426.campaign.ui.components.SlotNumeral
import io.github.xep426.campaign.ui.components.SlotPips
import io.github.xep426.campaign.ui.components.SlotTextField
import io.github.xep426.campaign.ui.components.TimeDialog
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.ScreenPadding
import io.github.xep426.campaign.ui.theme.SurfaceCard
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SpaceXl
import io.github.xep426.campaign.ui.theme.TaskDone
import java.time.LocalTime
import java.util.Locale

@Composable
fun TodayScreen(
    state: TodayUiState,
    onToggle: (DailyTask) -> Unit,
    onSetSlot: (Int, String) -> Unit,
    onDelete: (DailyTask) -> Unit,
    onAssign: (DailyTask, Campaign) -> Unit,
    onPromote: (DailyTask, String) -> Unit,
    onUnassign: (DailyTask) -> Unit,
    onCarry: (DailyTask) -> Unit,
    onSetTurnTime: (LocalTime) -> Unit,
    onMoveSlot: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf<DailyTask?>(null) }
    var timeOpen by remember { mutableStateOf(false) }
    val dateFormat = rememberDateFormat(R.string.format_date_header)

    val focus = LocalFocusManager.current

    // Two parts: the day scrolls, the tally does not.
    //
    // The tally is pinned to the bottom edge, above the navigation, so it
    // never competes with the three tasks for position. Putting it in the
    // scroll flow made it the last thing on the page AND the biggest, which
    // is the opposite of subordinate.
    Column(
        modifier = modifier
            .fillMaxSize()
            // Tapping blank space drops focus, which is what makes "tap
            // away to save" actually happen: Compose does not clear focus
            // on its own, so without this the field stays live, the commit
            // never fires, and the edit looks lost. detectTapGestures only
            // consumes taps, so the scroll below still works.
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } },
    ) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(top = SpaceLg, bottom = SpaceXl),
    ) {
        // The date of the day the app is IN, which after the turn is
        // already tomorrow's. One list, no switch — see CampaignDay.
        Eyebrow(state.date.format(dateFormat), Muted)
        Spacer(Modifier.height(SpaceSm))
        Text(
            text = stringResource(
                if (state.isPlanning) R.string.planning_title else R.string.today_title
            ),
            style = if (state.isPlanning) MaterialTheme.typography.displayLarge
            else MaterialTheme.typography.headlineLarge,
            color = Paper,
        )

        Spacer(Modifier.height(SpaceMd))
        if (state.isPlanning) {
            // The subtitle takes the progress row's place. A progress
            // readout here would be counting a day that has not started.
            Text(
                text = stringResource(R.string.planning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotPips(completed = state.completed, total = DailyTask.SLOTS_PER_DAY)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(
                        R.string.today_progress,
                        state.completed,
                        DailyTask.SLOTS_PER_DAY,
                    ).uppercase(Locale.getDefault()),
                    style = MonoMeta,
                    color = Muted,
                )
            }
        }

        Spacer(Modifier.height(SpaceLg))
        Hairline()
        // Drag to reorder, long-press to pick up. Disabled while a slot is
        // being typed into: a long-press inside a text field belongs to
        // text selection, and stealing it would break editing to serve
        // reordering.
        ReorderableColumn(
            count = DailyTask.SLOTS_PER_DAY,
            // Identity of the rows as the DATA currently orders them. The
            // column watches this to know when its own drag order has been
            // superseded, which is what stops the drop from flickering.
            keys = state.slots.map { it?.id },
            enabled = editingSlot == null,
            onMove = onMoveSlot,
        ) { slot, isDragging ->
            SlotRow(
                slot = slot,
                task = state.slots.getOrNull(slot),
                planning = state.isPlanning,
                editing = editingSlot == slot,
                dragging = isDragging,
                onStartEdit = { editingSlot = slot },
                onCommit = { text ->
                    editingSlot = null
                    onSetSlot(slot, text)
                },
                onToggle = { state.slots.getOrNull(slot)?.let(onToggle) },
                onDelete = { state.slots.getOrNull(slot)?.let(onDelete) },
                onAssign = { state.slots.getOrNull(slot)?.let { assigning = it } },
                onPullFrom = { pickerOpen = true },
            )
            Hairline()
        }

        // Only when it has something to add. The all-three-set case used to
        // say "Three things. No fourth." directly under a subtitle already
        // reading "you can only pick three" — the same rule twice. And
        // during planning the subtitle covers the whole screen's posture,
        // so nothing belongs down here at all.
        // The open-slot line is gone. "One slot still open. It is fine to
        // leave it that way" was reassurance nobody had asked for — the
        // empty slot already says it is empty, and saying it is allowed
        // implies someone thought it might not be.
        val foot = when {
            state.isPlanning -> null
            state.filled == 0 -> R.string.today_foot_empty
            state.completed == DailyTask.SLOTS_PER_DAY -> R.string.today_foot_all_done
            else -> null
        }
        foot?.let {
            Spacer(Modifier.height(SpaceXl))
            FootNote(stringResource(it))
        }

        // The tally sits here rather than on History, where it was first
        // built. A number nobody visits motivates nobody, and this screen
        // is the one opened every day — below the three, though, never
        // above them: the three things are the point and a percentage is
        // commentary on them.
        //
        // The app's one setting, and it lives here because it governs this
        // screen: the hour the list empties and starts again. Last thing in
        // the scrolling half, which makes it the close of the day rather
        // than a footnote under a statistic.
        Spacer(Modifier.height(SpaceXl))
        TurnRow(
            time = state.turnsAt,
            enabled = state.notificationsEnabled,
            onClick = { timeOpen = true },
        )
    }

        // Hidden during planning, like everything else that reports on a
        // day. The window ends on a day that has not started — and the
        // strip would sit under a screen whose whole posture is "decide",
        // answering a question nobody is asking yet.
        if (!state.isPlanning) {
            TallyStrip(state.progress)
        }
    }

    if (timeOpen) {
        TimeDialog(
            initial = state.turnsAt,
            onConfirm = {
                timeOpen = false
                onSetTurnTime(it)
            },
            onDismiss = { timeOpen = false },
        )
    }

    if (pickerOpen) {
        CampaignPickerDialog(
            campaigns = state.carryable,
            onPick = {
                pickerOpen = false
                onCarry(it)
            },
            onDismiss = { pickerOpen = false },
        )
    }

    assigning?.let { task ->
        AssignDialog(
            campaigns = state.activeCampaigns,
            currentCampaignId = task.campaignId,
            onAssign = { id ->
                state.activeCampaigns.firstOrNull { it.id == id }?.let { onAssign(task, it) }
                assigning = null
            },
            onCreate = { title ->
                onPromote(task, title)
                assigning = null
            },
            onClear = {
                onUnassign(task)
                assigning = null
            },
            onDismiss = { assigning = null },
        )
    }
}

/**
 * When the day turns. Stated as a fact about the list, not as a setting
 * about notifications — "the day turns at 22:00" is what the user is
 * choosing; the prompt arriving is a consequence of it.
 */
@Composable
private fun TurnRow(time: LocalTime, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .background(if (enabled) Ember else LineStrong, CircleShape)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(
                R.string.turns_at,
                stringResource(R.string.format_clock, time.hour, time.minute),
            ).uppercase(Locale.getDefault()),
            style = MonoMeta,
            color = Muted,
        )
    }
}

/**
 * One slot, in one of three states: filled, empty, or being typed into.
 *
 * The whole filled row is the completion target, not just the ring. The
 * ring is 21dp — fine to look at, mean to hit — and there is nothing else
 * on the row that a tap could plausibly mean. Anything destructive is
 * behind the overflow, where a mis-tap cannot reach it.
 */
@Composable
private fun SlotRow(
    slot: Int,
    task: DailyTask?,
    planning: Boolean,
    dragging: Boolean,
    editing: Boolean,
    onStartEdit: () -> Unit,
    onCommit: (String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onAssign: () -> Unit,
    onPullFrom: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Lifted, not highlighted: the picked-up row gets a faint warm
            // ground so it reads as sitting above the others, which is the
            // only cue a flat list can give for "you are holding this".
            .background(if (dragging) SurfaceCard else Color.Transparent)
            .padding(vertical = SpaceMd),
        verticalAlignment = Alignment.Top,
    ) {
        SlotNumeral(
            numeral = DailyTask.numeralFor(slot),
            focused = focused || task?.completed == false,
            modifier = Modifier.padding(top = 2.dp),
        )

        Box(Modifier.weight(1f)) {
            when {
                editing -> EditingSlot(
                    initial = task?.title.orEmpty(),
                    onCommit = onCommit,
                    onFocusChanged = { focused = it },
                )

                task != null -> FilledSlot(
                    task = task,
                    planning = planning,
                    onToggle = onToggle,
                    onEdit = onStartEdit,
                    onDelete = onDelete,
                    onAssign = onAssign,
                )

                else -> Row(
                    horizontalArrangement = Arrangement.spacedBy(SpaceLg),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    GhostAction(stringResource(R.string.slot_add), onStartEdit)
                    GhostAction(stringResource(R.string.slot_pull), onPullFrom)
                }
            }
        }
    }
}

@Composable
private fun EditingSlot(
    initial: String,
    onCommit: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    // Guards the commit below. A text field reports "not focused" on its
    // very first composition, before the request lands — without this the
    // editor would commit and close itself in the same frame it opened.
    var everFocused by remember { mutableStateOf(false) }

    // THREE WAYS OUT, ONE OUTCOME. Enter, tapping elsewhere, and the editor
    // simply going away (another slot tapped, the tab changed) all mean the
    // same thing: keep what was typed. There is no Save button here to have
    // missed, so losing the text to any of them would be the app discarding
    // work the user clearly meant to keep.
    //
    // The flag makes it idempotent — focus loss and disposal usually both
    // fire, and without it the second one writes a second time.
    val latest = rememberUpdatedState(text)
    var committed by remember { mutableStateOf(false) }
    val commit = {
        if (!committed) {
            committed = true
            onCommit(latest.value)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { commit() } }

    SlotTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = stringResource(R.string.slot_placeholder),
        modifier = Modifier.focusRequester(focusRequester),
        imeAction = ImeAction.Done,
        onImeAction = commit,
        onFocusChanged = { hasFocus ->
            if (hasFocus) everFocused = true
            onFocusChanged(hasFocus)
            if (!hasFocus && everFocused) commit()
        },
    )
}

@Composable
private fun FilledSlot(
    task: DailyTask,
    planning: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssign: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.Top) {
        Row(
            // A tap does whatever the moment allows: complete it during the
            // day, refine the wording before the day starts. There is no
            // disabled control anywhere here — the mark is simply absent
            // while there is nothing that could truthfully be marked.
            modifier = Modifier
                .weight(1f)
                .clickable { if (planning) onEdit() else onToggle() },
            verticalAlignment = Alignment.Top,
        ) {
            if (!planning) {
                CompletionMark(completed = task.completed, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.size(14.dp))
            }
            Column {
                Text(
                    text = task.title,
                    style = if (task.completed) TaskDone else MaterialTheme.typography.bodyLarge,
                    color = if (task.completed) Muted else Paper,
                )
                task.campaignTitle?.let { title ->
                    Spacer(Modifier.height(10.dp))
                    CampaignTag(title)
                }
            }
        }

        Box {
            Text(
                text = "⋯",
                style = MaterialTheme.typography.titleMedium,
                color = Muted,
                modifier = Modifier
                    .clickable { menuOpen = true }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_edit)) },
                    onClick = { menuOpen = false; onEdit() },
                )
                // Always offered, assigned or not: the dialog is where you
                // change or remove the link as well as set it.
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_assign)) },
                    onClick = { menuOpen = false; onAssign() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_delete)) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}
