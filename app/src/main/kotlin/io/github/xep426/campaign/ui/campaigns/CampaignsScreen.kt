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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.ui.components.ConfirmDialog
import io.github.xep426.campaign.ui.components.Eyebrow
import io.github.xep426.campaign.ui.components.FootNote
import io.github.xep426.campaign.ui.components.GhostAction
import io.github.xep426.campaign.ui.components.QuietButton
import io.github.xep426.campaign.ui.components.SlotTextField
import io.github.xep426.campaign.ui.components.TextInputDialog
import io.github.xep426.campaign.ui.components.rememberDateFormat
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.EmberDeep
import io.github.xep426.campaign.ui.theme.Line
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.Sage
import io.github.xep426.campaign.ui.theme.ScreenPadding
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SpaceXl
import io.github.xep426.campaign.ui.theme.SurfaceCard
import java.util.Locale

@Composable
fun CampaignsScreen(
    state: CampaignsUiState,
    onCarry: (DailyTask) -> Unit,
    onRename: (Campaign, String) -> Unit,
    onSetNotes: (Campaign, String) -> Unit,
    onClose: (Campaign, CampaignStatus) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Campaign) -> Unit,
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
                Eyebrow(stringResource(R.string.campaigns_active_count, state.active.size), Muted)
                Spacer(Modifier.height(SpaceSm))
                Text(
                    text = stringResource(R.string.campaigns_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Paper,
                )
                Spacer(Modifier.height(SpaceLg))
            }
        }

        items(state.active, key = { it.id }) { campaign ->
            CampaignCard(
                campaign = campaign,
                today = state.today,
                onCarry = onCarry,
                onRename = { renaming = campaign },
                onSetNotes = { onSetNotes(campaign, it) },
                onClose = { onClose(campaign, it) },
                onDelete = { deleting = campaign },
            )
        }

        // No closing note. The screen shows campaigns and offers to start
        // one; a paragraph explaining what a campaign is would be the
        // design apologising for itself, and the last one outlived the
        // model it described by two rewrites.
        item {
            Spacer(Modifier.height(SpaceMd))
            GhostAction(
                text = stringResource(R.string.campaign_new),
                onClick = { creating = true },
                tint = Ember,
            )
        }
    }

    renaming?.let { campaign ->
        TextInputDialog(
            label = stringResource(R.string.campaign_rename_title),
            initial = campaign.title,
            placeholder = stringResource(R.string.campaign_rename_placeholder),
            confirmText = stringResource(R.string.campaign_rename),
            onConfirm = {
                onRename(campaign, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    if (creating) {
        TextInputDialog(
            label = stringResource(R.string.campaign_new),
            initial = "",
            placeholder = stringResource(R.string.campaign_new_placeholder),
            confirmText = stringResource(R.string.assign_create),
            onConfirm = {
                onCreate(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }

    deleting?.let { campaign ->
        ConfirmDialog(
            title = stringResource(R.string.campaign_delete_title),
            body = stringResource(R.string.campaign_delete_body, campaign.title),
            confirmText = stringResource(R.string.campaign_delete_confirm),
            onConfirm = {
                onDelete(campaign)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CampaignCard(
    campaign: Campaign,
    today: java.time.LocalDate,
    onCarry: (DailyTask) -> Unit,
    onRename: () -> Unit,
    onSetNotes: (String) -> Unit,
    onClose: (CampaignStatus) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var notesOpen by remember { mutableStateOf(campaign.notes.isNotBlank()) }
    val started = rememberDateFormat(R.string.format_month_day)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(SpaceMd),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = campaign.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Paper,
                )
                Spacer(Modifier.height(6.dp))
                val days = campaign.daysRunning(today).toInt()
                Text(
                    // Plurals, not "%d days": German needs Tag/Tage, and
                    // English needs day/days on a campaign started today.
                    text = listOf(
                        stringResource(
                            R.string.campaign_meta_started,
                            campaign.createdAt.format(started),
                        ),
                        pluralStringResource(R.plurals.campaign_meta_days, days, days),
                        pluralStringResource(
                            R.plurals.campaign_meta_steps,
                            campaign.stepsTaken,
                            campaign.stepsTaken,
                        ),
                    ).joinToString(" · ").uppercase(Locale.getDefault()),
                    style = MonoMeta,
                    color = Muted,
                )
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
                        onClick = { menuOpen = false; onClose(CampaignStatus.COMPLETED) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_archive)) },
                        onClick = { menuOpen = false; onClose(CampaignStatus.ARCHIVED) },
                    )
                    // Last, and separated in meaning from the two above:
                    // complete and archive both say "this happened",
                    // delete says "this never should have existed".
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }

        Spacer(Modifier.height(SpaceMd))

        // The campaign's actual content: what is still outstanding. There
        // is nothing to author here — tasks are made on Today and assigned;
        // this is the lens over the ones that have not landed yet.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.28f),
                    RoundedCornerShape(9.dp),
                )
                .padding(start = 13.dp, end = 13.dp, top = 11.dp, bottom = 11.dp),
        ) {
            if (campaign.isQuiet) {
                Text(
                    text = stringResource(R.string.campaign_open_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            } else {
                campaign.openTasks.forEachIndexed { index, task ->
                    if (index > 0) Spacer(Modifier.height(SpaceSm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarry(task) },
                    ) {
                        // The ember rule marks a live line, as in the mock.
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(34.dp)
                                .background(EmberDeep)
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Paper,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                // The date is what makes this list useful:
                                // it says how long the thing has waited.
                                text = stringResource(
                                    R.string.picker_waiting_since,
                                    task.date.format(started),
                                ).uppercase(Locale.getDefault()),
                                style = MonoMeta,
                                color = Muted,
                            )
                        }
                    }
                }
            }
        }

        if (notesOpen) {
            Spacer(Modifier.height(SpaceSm))
            NotesField(initial = campaign.notes, onCommit = onSetNotes)
        }

        Spacer(Modifier.height(SpaceMd))

        // No pull button: the open tasks above ARE the pull targets, and a
        // button would have to guess which one you meant.
        Row(horizontalArrangement = Arrangement.spacedBy(SpaceSm)) {
            QuietButton(
                text = stringResource(R.string.campaign_complete),
                onClick = { onClose(CampaignStatus.COMPLETED) },
                modifier = Modifier.weight(1f),
                tint = Sage,
            )
            QuietButton(
                text = stringResource(R.string.campaign_archive),
                onClick = { onClose(CampaignStatus.ARCHIVED) },
                tint = Muted,
            )
        }
    }
}

@Composable
private fun NotesField(initial: String, onCommit: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    var everFocused by remember { mutableStateOf(false) }

    // Same rule as the task editor: Enter, tapping away and the field
    // simply going away all keep what was typed. Disposal matters most
    // here — collapsing the notes with the menu is the usual way out, and
    // that never produces a focus-loss event.
    val latest = rememberUpdatedState(text)
    var committed by remember { mutableStateOf(false) }
    val commit = {
        if (!committed) {
            committed = true
            onCommit(latest.value)
        }
    }
    DisposableEffect(Unit) { onDispose { commit() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LineStrong, RoundedCornerShape(9.dp))
            .padding(13.dp),
    ) {
        Eyebrow(stringResource(R.string.campaign_notes), Muted)
        Spacer(Modifier.height(6.dp))
        SlotTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.campaign_notes_placeholder),
            imeAction = ImeAction.Done,
            onImeAction = commit,
            onFocusChanged = { hasFocus ->
                if (hasFocus) everFocused = true
                if (!hasFocus && everFocused) commit()
            },
        )
    }
}
