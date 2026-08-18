package io.github.xep426.campaign.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.Line
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SurfaceCard
import java.time.format.DateTimeFormatter

/**
 * Outstanding campaign work, to bring onto a day.
 *
 * Lists TASKS grouped under their campaign, not campaigns. The earlier
 * version listed campaigns and copied a free-text "next step" from each —
 * that field is gone, because a campaign's content is now simply the tasks
 * assigned to it that are not done. So this picks the actual thing to be
 * moved rather than a description of it.
 *
 * The date beside each task is the point: it says how long that task has
 * been waiting, which is the only information that helps you choose.
 */
@Composable
fun CampaignPickerDialog(
    campaigns: List<Campaign>,
    onPick: (DailyTask) -> Unit,
    onDismiss: () -> Unit,
) {
    val dayLabel = rememberDateFormat(R.string.format_day_label)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(18.dp))
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(SpaceLg),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            Eyebrow(stringResource(R.string.picker_title), Ember)

            if (campaigns.isEmpty()) {
                Text(
                    text = stringResource(R.string.picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(SpaceSm),
                ) {
                    campaigns.forEach { campaign ->
                        item(key = "c${campaign.id}") {
                            Spacer(Modifier.height(SpaceSm))
                            Eyebrow(campaign.title, Muted)
                        }
                        items(
                            count = campaign.openTasks.size,
                            key = { i -> "t${campaign.openTasks[i].id}" },
                        ) { i ->
                            val task = campaign.openTasks[i]
                            OutstandingTask(
                                task = task,
                                dayLabel = dayLabel,
                                onClick = { onPick(task) },
                            )
                        }
                    }
                }
            }

            GhostAction(stringResource(R.string.picker_dismiss), onDismiss)
        }
    }
}

@Composable
private fun OutstandingTask(
    task: DailyTask,
    dayLabel: DateTimeFormatter,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SpaceSm),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper,
        )
        Text(
            text = stringResource(R.string.picker_waiting_since, task.date.format(dayLabel)),
            style = io.github.xep426.campaign.ui.theme.MonoMeta,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
