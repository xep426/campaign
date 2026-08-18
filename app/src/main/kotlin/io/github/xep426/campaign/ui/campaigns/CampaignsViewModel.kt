package io.github.xep426.campaign.ui.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignStatus
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.repository.TaskRepository
import io.github.xep426.campaign.ui.UiMessage
import io.github.xep426.campaign.ui.uiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CampaignsUiState(
    val today: LocalDate = LocalDate.now(),
    val active: List<Campaign> = emptyList(),
    /** True when all three of today's slots are taken — every pull is off. */
    val dayIsFull: Boolean = false,
    /** Campaign ids already sitting in one of today's slots. */
    val alreadyToday: Set<Long> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CampaignsViewModel @Inject constructor(
    private val campaigns: CampaignRepository,
    tasks: TaskRepository,
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val ui: StateFlow<CampaignsUiState> = combine(
        date,
        campaigns.active(),
        date.flatMapLatest { tasks.tasksFor(it) },
    ) { day, active, todayTasks ->
        CampaignsUiState(
            today = day,
            active = active,
            dayIsFull = todayTasks.size >= DailyTask.SLOTS_PER_DAY,
            alreadyToday = todayTasks.mapNotNull { it.campaignId }.toSet(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampaignsUiState(),
    )

    fun refreshDate() {
        date.value = LocalDate.now()
    }

    fun carryForward(task: DailyTask) = viewModelScope.launch {
        val slot = campaigns.carryForward(task.id, date.value)
        _messages.tryEmit(
            if (slot == null) uiMessage(R.string.msg_day_full)
            else uiMessage(R.string.msg_pulled, DailyTask.numeralFor(slot))
        )
    }

    fun create(title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        campaigns.create(title, date.value)
        _messages.tryEmit(uiMessage(R.string.msg_campaign_created, title.trim()))
    }

    fun delete(campaign: Campaign) = viewModelScope.launch {
        campaigns.delete(campaign.id)
        _messages.tryEmit(uiMessage(R.string.msg_campaign_deleted, campaign.title))
    }

    fun rename(campaign: Campaign, title: String) = viewModelScope.launch {
        campaigns.rename(campaign.id, title)
    }

    fun setNotes(campaign: Campaign, notes: String) = viewModelScope.launch {
        campaigns.setNotes(campaign.id, notes)
    }

    fun close(campaign: Campaign, status: CampaignStatus) = viewModelScope.launch {
        campaigns.close(campaign.id, status, date.value)
        _messages.tryEmit(
            uiMessage(
                if (status == CampaignStatus.COMPLETED) R.string.msg_completed
                else R.string.msg_archived,
                campaign.title,
            )
        )
    }
}
