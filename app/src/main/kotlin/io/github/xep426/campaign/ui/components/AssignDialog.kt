package io.github.xep426.campaign.ui.components

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.ui.theme.Ember
import io.github.xep426.campaign.ui.theme.Line
import io.github.xep426.campaign.ui.theme.LineStrong
import io.github.xep426.campaign.ui.theme.Muted
import io.github.xep426.campaign.ui.theme.Paper
import io.github.xep426.campaign.ui.theme.Sage
import io.github.xep426.campaign.ui.theme.SpaceLg
import io.github.xep426.campaign.ui.theme.SpaceMd
import io.github.xep426.campaign.ui.theme.SpaceSm
import io.github.xep426.campaign.ui.theme.SurfaceCard

/**
 * Which campaign, if any, this task belongs to.
 *
 * ONE gesture where there used to be two. "Promote" was a separate act
 * only because campaigns could not be created any other way; now that they
 * can, promoting is just the branch of assignment where the campaign does
 * not exist yet. A menu offering both "Promote to campaign" and "Assign to
 * campaign" would be asking the user to know an implementation detail.
 *
 * The naming field is the fix for the older mistake: a campaign inherited
 * its task's title and therefore had to be renamed nearly every time,
 * because a task is a STEP and the campaign is the effort it serves.
 */
@Composable
fun AssignDialog(
    campaigns: List<Campaign>,
    currentCampaignId: Long?,
    onAssign: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var naming by remember { mutableStateOf(campaigns.isEmpty()) }
    var newTitle by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(18.dp))
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(SpaceLg),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            Eyebrow(stringResource(R.string.assign_title), Ember)

            if (naming) {
                Text(
                    text = stringResource(R.string.assign_new_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                SlotTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = stringResource(R.string.assign_new_placeholder),
                    imeAction = ImeAction.Done,
                    onImeAction = { if (newTitle.isNotBlank()) onCreate(newTitle) },
                )
                Hairline()
                Spacer(Modifier.height(SpaceSm))
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceSm)) {
                    QuietButton(
                        text = stringResource(R.string.dialog_cancel),
                        onClick = { if (campaigns.isEmpty()) onDismiss() else naming = false },
                        modifier = Modifier.weight(1f),
                    )
                    QuietButton(
                        text = stringResource(R.string.assign_create),
                        onClick = { onCreate(newTitle) },
                        modifier = Modifier.weight(1f),
                        enabled = newTitle.isNotBlank(),
                        tint = Ember,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(campaigns, key = { it.id }) { campaign ->
                        CampaignChoice(
                            title = campaign.title,
                            selected = campaign.id == currentCampaignId,
                            onClick = { onAssign(campaign.id) },
                        )
                    }
                }

                Hairline()

                GhostAction(stringResource(R.string.assign_new), { naming = true })

                // Only offered when there is something to undo. An always-
                // present "remove" on an unassigned task is a control that
                // can only ever do nothing.
                if (currentCampaignId != null) {
                    GhostAction(stringResource(R.string.assign_clear), onClear)
                }

                GhostAction(stringResource(R.string.picker_dismiss), onDismiss)
            }
        }
    }
}

@Composable
private fun CampaignChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(if (selected) Sage else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .border(1.dp, if (selected) Sage else LineStrong, CircleShape)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Paper else Paper,
        )
    }
}
