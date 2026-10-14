package io.github.xep426.campaign.ui.campaigns

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.Step
import io.github.xep426.campaign.ui.components.CompletionMark
import io.github.xep426.campaign.ui.components.ConfirmDialog
import io.github.xep426.campaign.ui.components.Eyebrow
import io.github.xep426.campaign.ui.components.GhostAction
import io.github.xep426.campaign.ui.components.InlineEntry
import io.github.xep426.campaign.ui.components.NotesField
import io.github.xep426.campaign.ui.components.TextInputDialog
import io.github.xep426.campaign.ui.theme.AppColors
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.ScreenPadding
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SpaceXl
import java.time.LocalDate
import java.util.Locale

/**
 * Everything you are keeping, pinned or not.
 *
 * The questlog shows three; this shows all of them, and is where a
 * campaign is built: named, broken into steps, reordered, pinned when you
 * are ready to push it.
 *
 * THE STEPS ARE EDITABLE HERE AND NOWHERE ELSE. The questlog can add one
 * in a hurry, but the list, its order and its deletions live here — so
 * that the screen you look at every day stays a list of three things and
 * does not drift back into being a backlog with a view.
 */
@Composable
fun CampaignsScreen(
    state: CampaignsUiState,
    onCreate: (String) -> Unit,
    onPin: (Campaign) -> Unit,
    onUnpin: (Campaign) -> Unit,
    onRename: (Campaign, String) -> Unit,
    onSetNotes: (Campaign, String) -> Unit,
    onComplete: (Campaign) -> Unit,
    onDelete: (Campaign) -> Unit,
    onAddStep: (Campaign, String) -> Unit,
    onToggleStep: (Step) -> Unit,
    onDeleteStep: (Step) -> Unit,
    onMoveStep: (Campaign, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf<Campaign?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Campaign?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = SpaceLg,
            bottom = SpaceXl,
        ),
        verticalArrangement = Arrangement.spacedBy(SpaceSm),
    ) {
        item {
            Column {
                Eyebrow(
                    stringResource(R.string.campaigns_active_count, state.active.size),
                    AppColors.muted,
                )
                Spacer(Modifier.height(SpaceSm))
                Text(
                    text = stringResource(R.string.campaigns_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppColors.paper,
                )
                Spacer(Modifier.height(SpaceLg))
            }
        }

        items(state.active, key = { it.id }) { campaign ->
            CampaignCard(
                campaign = campaign,
                today = state.today,
                slotsFree = state.slotsFree,
                onPin = { onPin(campaign) },
                onUnpin = { onUnpin(campaign) },
                onRename = { renaming = campaign },
                onSetNotes = { onSetNotes(campaign, it) },
                onComplete = { onComplete(campaign) },
                onDelete = { deleting = campaign },
                onAddStep = { onAddStep(campaign, it) },
                onToggleStep = onToggleStep,
                onDeleteStep = onDeleteStep,
                onMoveStep = { from, to -> onMoveStep(campaign, from, to) },
            )
        }

        item {
            Spacer(Modifier.height(SpaceMd))
            if (creating) {
                InlineEntry(
                    placeholder = stringResource(R.string.campaign_name_placeholder),
                    onCommit = { creating = false; onCreate(it) },
                    onCancel = { creating = false },
                )
            } else {
                GhostAction(
                    text = stringResource(R.string.campaign_new),
                    onClick = { creating = true },
                    tint = AppColors.ember,
                )
            }
        }
    }

    renaming?.let { campaign ->
        TextInputDialog(
            label = stringResource(R.string.campaign_rename),
            initial = campaign.title,
            placeholder = stringResource(R.string.campaign_name_placeholder),
            confirmText = stringResource(R.string.campaign_rename),
            onConfirm = { renaming = null; onRename(campaign, it) },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { campaign ->
        ConfirmDialog(
            title = campaign.title,
            body = stringResource(R.string.campaign_delete_body),
            confirmText = stringResource(R.string.campaign_delete),
            onConfirm = { deleting = null; onDelete(campaign) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CampaignCard(
    campaign: Campaign,
    today: LocalDate,
    slotsFree: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRename: () -> Unit,
    onSetNotes: (String) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onAddStep: (String) -> Unit,
    onToggleStep: (Step) -> Unit,
    onDeleteStep: (Step) -> Unit,
    onMoveStep: (Int, Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var notesOpen by remember { mutableStateOf(campaign.notes.isNotBlank()) }
    var adding by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceCard, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                // A pinned campaign carries an ember edge. It is the only
                // difference between the two states on this screen, and
                // the state is worth seeing without reading anything.
                if (campaign.isPinned) AppColors.ember.copy(alpha = 0.45f) else AppColors.line,
                RoundedCornerShape(16.dp),
            )
            .padding(SpaceMd),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (campaign.isPinned) {
                        Text(
                            text = Campaign.numeralFor(campaign.slot ?: 0),
                            style = MonoMeta,
                            color = AppColors.ember,
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        text = campaign.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.paper,
                    )
                }
                Spacer(Modifier.height(5.dp))
                val days = campaign.daysRunning(today).toInt()
                Text(
                    text = listOf(
                        pluralStringResource(R.plurals.campaign_meta_days, days, days),
                        pluralStringResource(
                            R.plurals.campaign_meta_steps,
                            campaign.stepsTaken,
                            campaign.stepsTaken,
                        ),
                    ).joinToString(" · ").uppercase(Locale.getDefault()),
                    style = MonoMeta,
                    color = AppColors.muted,
                )
            }
            Box {
                Text(
                    text = "⋯",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.muted,
                    modifier = Modifier
                        .clickable { menuOpen = true }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (campaign.isPinned) R.string.questlog_unpin
                                    else R.string.campaign_pin
                                )
                            )
                        },
                        // Offered even with no slot free: the refusal says
                        // the limit exists, where a greyed-out row would
                        // just look broken.
                        onClick = {
                            menuOpen = false
                            if (campaign.isPinned) onUnpin() else onPin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.questlog_add_step)) },
                        onClick = { menuOpen = false; expanded = true; adding = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_rename)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (notesOpen) R.string.campaign_notes_hide
                                    else R.string.campaign_notes
                                )
                            )
                        },
                        onClick = { menuOpen = false; notesOpen = !notesOpen },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_complete)) },
                        onClick = { menuOpen = false; onComplete() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }

        Spacer(Modifier.height(SpaceMd))

        // Collapsed by default. A campaign may hold twenty steps and this
        // screen may hold thirty campaigns; opening on all of it is the
        // wall of text the whole app is built to avoid.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "▾" else "▸",
                style = MonoMeta,
                color = AppColors.ember,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.campaign_open_count,
                    campaign.openSteps.size,
                    campaign.openSteps.size,
                ).uppercase(Locale.getDefault()),
                style = MonoMeta,
                color = if (expanded) AppColors.paper else AppColors.muted,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(SpaceSm))
            campaign.openSteps.forEachIndexed { index, step ->
                StepRow(
                    step = step,
                    canMoveUp = index > 0,
                    canMoveDown = index < campaign.openSteps.size - 1,
                    onToggle = { onToggleStep(step) },
                    onDelete = { onDeleteStep(step) },
                    onMoveUp = { onMoveStep(index, index - 1) },
                    onMoveDown = { onMoveStep(index, index + 1) },
                )
            }

            if (adding) {
                Spacer(Modifier.height(SpaceSm))
                InlineEntry(
                    placeholder = stringResource(R.string.questlog_step_placeholder),
                    onCommit = { adding = false; onAddStep(it) },
                    onCancel = { adding = false },
                )
            } else {
                Spacer(Modifier.height(SpaceSm))
                GhostAction(
                    text = stringResource(R.string.questlog_add_step),
                    onClick = { adding = true },
                    tint = AppColors.muted,
                )
            }
        }

        if (notesOpen) {
            Spacer(Modifier.height(SpaceSm))
            NotesField(initial = campaign.notes, onCommit = onSetNotes)
        }
    }
}

@Composable
private fun StepRow(
    step: Step,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            CompletionMark(completed = step.completed, size = 18.dp)
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = step.title,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.paper,
            modifier = Modifier.weight(1f),
        )
        Box {
            Text(
                text = "⋯",
                style = MonoMeta,
                color = AppColors.muted,
                modifier = Modifier
                    .clickable { menuOpen = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                // Arrows rather than drag: a step list sits inside a card
                // inside a scrolling list, and a long-press drag there
                // fights the scroll it lives in. The order of a campaign
                // changes rarely enough that two taps is not a cost.
                if (canMoveUp) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.step_move_up)) },
                        onClick = { menuOpen = false; onMoveUp() },
                    )
                }
                if (canMoveDown) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.step_move_down)) },
                        onClick = { menuOpen = false; onMoveDown() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_delete)) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}
