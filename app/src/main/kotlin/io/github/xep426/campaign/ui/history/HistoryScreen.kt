package io.github.xep426.campaign.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.ui.components.CampaignTag
import io.github.xep426.campaign.ui.components.Eyebrow
import io.github.xep426.campaign.ui.components.FootNote
import io.github.xep426.campaign.ui.components.Hairline
import io.github.xep426.campaign.ui.components.rememberDateFormat
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.MonoLabel
import io.github.xep426.campaign.ui.theme.MonoMeta
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.PaperDim
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.Sage
import io.github.xep426.campaign.ui.theme.ScreenPadding
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SpaceXl
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Campaigns first, and the default. Finishing a campaign is the thing
 * worth looking back on — a day’s three tasks are the mechanism, the
 * campaign is the result. Opening on Days put the ledger before the
 * achievements.
 */
private enum class Tab { CAMPAIGNS, TASKS }

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onReopen: (Campaign) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(Tab.CAMPAIGNS) }
    val dayLabel = rememberDateFormat(R.string.format_day_label)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = SpaceLg,
            bottom = SpaceXl,
        ),
    ) {
        item {
            Column {
                // Says something true about the tab you are on: a span of
                // days under Days, a count of finished efforts under
                // Campaigns. "Since 17 Aug" above a campaign list is just
                // the wrong caption.
                Eyebrow(
                    when {
                        tab == Tab.CAMPAIGNS && state.finished.isEmpty() ->
                            stringResource(R.string.history_nothing_yet)
                        tab == Tab.CAMPAIGNS -> stringResource(
                            R.string.history_finished_count,
                            state.finished.size,
                        )
                        state.tasks.isEmpty() -> stringResource(R.string.history_nothing_yet)
                        else -> stringResource(
                            R.string.history_finished_count,
                            state.tasks.size,
                        )
                    },
                    Muted,
                )
                Spacer(Modifier.height(SpaceSm))
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Paper,
                )
                Spacer(Modifier.height(SpaceLg))
                Segmented(tab) { tab = it }
                Spacer(Modifier.height(SpaceSm))
            }
        }

        when (tab) {
            Tab.TASKS -> {
                items(state.tasks, key = { it.id }) { task ->
                    FinishedTaskRow(task, dayLabel)
                    Hairline()
                }
                item {
                    Spacer(Modifier.height(SpaceXl))
                    FootNote(
                        stringResource(
                            if (state.tasks.isEmpty()) R.string.history_foot_tasks_empty
                            else R.string.history_foot_tasks
                        )
                    )
                }
            }

            Tab.CAMPAIGNS -> {
                items(state.finished, key = { it.id }) { campaign ->
                    FinishedCampaignRow(campaign, dayLabel, onReopen = { onReopen(campaign) })
                    Hairline()
                }
                item {
                    Spacer(Modifier.height(SpaceXl))
                    FootNote(
                        stringResource(
                            if (state.finished.isEmpty()) R.string.history_foot_campaigns_empty
                            else R.string.history_foot_campaigns
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Segmented(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .border(1.dp, io.github.xep426.campaign.ui.theme.Line, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (on) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(7.dp),
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (tab == Tab.TASKS) R.string.history_tab_tasks
                        else R.string.history_tab_campaigns
                    ).uppercase(Locale.getDefault()),
                    style = MonoLabel,
                    color = if (on) Paper else Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A finished standalone task: what it was, and when it landed.
 *
 * No completion mark. Everything on this list is done — a row of identical
 * ticks would be decoration, and the sage title already says it.
 */
@Composable
private fun FinishedTaskRow(task: DailyTask, dayLabel: DateTimeFormatter) {
    Column(Modifier.fillMaxWidth().padding(vertical = SpaceMd)) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = PaperDim,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = task.date.format(dayLabel).uppercase(Locale.getDefault()),
            style = MonoMeta,
            color = Muted,
        )
    }
}

@Composable
private fun FinishedCampaignRow(
    campaign: Campaign,
    dayLabel: DateTimeFormatter,
    onReopen: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onReopen)
            .padding(vertical = SpaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = campaign.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Paper,
                modifier = Modifier.weight(1f),
            )
            CampaignTag(
                text = stringResource(
                    if (campaign.status == CampaignStatus.COMPLETED) R.string.status_completed
                    else R.string.status_archived
                ),
                tint = if (campaign.status == CampaignStatus.COMPLETED) Sage else Muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        val span = campaign.closedAt?.let {
            stringResource(
                R.string.history_campaign_range,
                campaign.createdAt.format(dayLabel),
                it.format(dayLabel),
            )
        } ?: campaign.createdAt.format(dayLabel)
        Text(
            text = stringResource(
                R.string.history_campaign_meta,
                span,
                pluralStringResource(
                    R.plurals.campaign_meta_steps,
                    campaign.stepsTaken,
                    campaign.stepsTaken,
                ),
            ).uppercase(Locale.getDefault()),
            style = MonoMeta,
            color = Muted,
        )
    }
}
