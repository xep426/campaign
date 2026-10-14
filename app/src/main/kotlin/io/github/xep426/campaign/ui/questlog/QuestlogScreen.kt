package io.github.xep426.campaign.ui.questlog

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
import io.github.xep426.campaign.ui.components.Eyebrow
import io.github.xep426.campaign.ui.components.GhostAction
import io.github.xep426.campaign.ui.components.InlineEntry
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
 * What you are pushing: at most three campaigns, one next step each.
 *
 * This replaced the Today screen, and the difference is the whole rework.
 * Today showed three slots that emptied every night; this shows three
 * campaigns that stay until they are finished. The number is the same and
 * means something else — it is now a limit on how much you are PUSHING
 * rather than on what a single day may contain.
 *
 * ONE STEP PER CAMPAIGN IS VISIBLE, deliberately. A campaign may hold
 * twenty steps; showing them all here would turn the questlog back into
 * the backlog this app refuses. The next one is the only one you can act
 * on anyway, and the rest are a tap away on Campaigns.
 */
@Composable
fun QuestlogScreen(
    state: QuestlogUiState,
    onComplete: (Step) -> Unit,
    onAddStep: (Campaign, String) -> Unit,
    onUnpin: (Campaign) -> Unit,
    onCompleteCampaign: (Campaign) -> Unit,
    onGoToCampaigns: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    stringResource(R.string.questlog_count, state.pinned.size, Campaign.SLOTS),
                    AppColors.muted,
                )
                Spacer(Modifier.height(SpaceSm))
                Text(
                    text = stringResource(R.string.questlog_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppColors.paper,
                )
                Spacer(Modifier.height(SpaceLg))
            }
        }

        items(state.pinned, key = { it.id }) { campaign ->
            PinnedCard(
                campaign = campaign,
                today = state.today,
                onComplete = onComplete,
                onAddStep = { onAddStep(campaign, it) },
                onUnpin = { onUnpin(campaign) },
                onCompleteCampaign = { onCompleteCampaign(campaign) },
            )
        }

        // The free slots are drawn, not omitted. An empty place says the
        // limit exists and that you have not spent it; leaving it out
        // would make three campaigns and one campaign look like the same
        // screen with different amounts of content.
        items(state.freeSlots) {
            EmptySlot(
                hasAny = state.hasAny,
                onClick = onGoToCampaigns,
            )
        }
    }
}

@Composable
private fun PinnedCard(
    campaign: Campaign,
    today: LocalDate,
    onComplete: (Step) -> Unit,
    onAddStep: (String) -> Unit,
    onUnpin: () -> Unit,
    onCompleteCampaign: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.line, RoundedCornerShape(16.dp))
            .padding(SpaceMd),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = campaign.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.paper,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = metaLine(campaign, today),
                    style = MonoMeta,
                    // Ember once it has been quiet a while. The app no
                    // longer resets anything nightly, so this is the only
                    // thing left that can apply pressure — and it should
                    // look like pressure rather than like a date.
                    color = if (campaign.daysSinceProgress(today) >= STALE_AFTER_DAYS) {
                        AppColors.ember
                    } else {
                        AppColors.muted
                    },
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
                        text = { Text(stringResource(R.string.questlog_add_step)) },
                        onClick = { menuOpen = false; adding = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.questlog_unpin)) },
                        onClick = { menuOpen = false; onUnpin() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.campaign_complete)) },
                        onClick = { menuOpen = false; onCompleteCampaign() },
                    )
                }
            }
        }

        Spacer(Modifier.height(SpaceMd))

        val next = campaign.nextStep
        when {
            adding -> InlineEntry(
                placeholder = stringResource(R.string.questlog_step_placeholder),
                onCommit = { adding = false; onAddStep(it) },
                onCancel = { adding = false },
            )

            next != null -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The whole row, as on the widget and as the old Today
                    // screen did: one grammar across every surface.
                    .clickable { onComplete(next) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompletionMark(completed = false)
                Spacer(Modifier.width(13.dp))
                Text(
                    text = next.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.paper,
                    modifier = Modifier.weight(1f),
                )
            }

            // Pinned with nothing to do. Not an error — it is what a
            // campaign looks like between deciding to push it and knowing
            // what the next move is, and the fix belongs right here.
            else -> GhostAction(
                text = stringResource(R.string.questlog_no_step),
                onClick = { adding = true },
                tint = AppColors.ember,
            )
        }

        if (campaign.openSteps.size > 1) {
            Spacer(Modifier.height(SpaceSm))
            Text(
                text = pluralStringResource(
                    R.plurals.questlog_waiting,
                    campaign.openSteps.size - 1,
                    campaign.openSteps.size - 1,
                ).uppercase(Locale.getDefault()),
                style = MonoMeta,
                color = AppColors.muted,
            )
        }
    }
}

@Composable
private fun EmptySlot(hasAny: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(SpaceMd),
    ) {
        Text(
            text = stringResource(
                if (hasAny) R.string.questlog_slot_free else R.string.questlog_slot_first
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.muted,
        )
    }
}

@Composable
private fun metaLine(campaign: Campaign, today: LocalDate): String {
    val quiet = campaign.daysSinceProgress(today).toInt()
    val done = campaign.stepsTaken
    return listOf(
        pluralStringResource(R.plurals.campaign_meta_steps, done, done),
        if (campaign.lastProgress == null) {
            stringResource(R.string.questlog_never)
        } else {
            pluralStringResource(R.plurals.questlog_quiet, quiet, quiet)
        },
    ).joinToString(" · ").uppercase(Locale.getDefault())
}

/**
 * When silence starts being worth pointing at.
 *
 * A week, because anything shorter turns an ordinary busy stretch into a
 * reproach — and the whole premise of the rework is that these things move
 * slowly and that is allowed.
 */
private const val STALE_AFTER_DAYS = 7
