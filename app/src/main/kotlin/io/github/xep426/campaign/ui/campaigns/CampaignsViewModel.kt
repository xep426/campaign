package io.github.xep426.campaign.ui.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.xep426.campaign.R
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.Step
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.StepRepository
import io.github.xep426.campaign.ui.UiMessage
import io.github.xep426.campaign.ui.uiMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CampaignsUiState(
    val today: LocalDate = LocalDate.now(),
    val active: List<Campaign> = emptyList(),
) {
    val pinnedCount: Int get() = active.count { it.isPinned }
    val slotsFree: Boolean get() = pinnedCount < Campaign.SLOTS
}

@HiltViewModel
class CampaignsViewModel @Inject constructor(
    private val campaigns: CampaignRepository,
    private val steps: StepRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val ui: StateFlow<CampaignsUiState> = combine(
        today,
        campaigns.active(),
    ) { day, active ->
        CampaignsUiState(today = day, active = active)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampaignsUiState(),
    )

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun create(title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        campaigns.create(title, today.value)
        _messages.tryEmit(uiMessage(R.string.msg_campaign_created, title.trim()))
    }

    /**
     * Pin, or say why not.
     *
     * The repository returns null when all three slots are taken rather
     * than evicting someone — the cap is the product, and quietly making
     * room for a fourth would dissolve it.
     */
    fun pin(campaign: Campaign) = viewModelScope.launch {
        val slot = campaigns.pin(campaign.id)
        _messages.tryEmit(
            if (slot == null) uiMessage(R.string.msg_slots_full)
            else uiMessage(R.string.msg_pinned, campaign.title)
        )
    }

    fun unpin(campaign: Campaign) = viewModelScope.launch {
        campaigns.unpin(campaign.id)
        _messages.tryEmit(uiMessage(R.string.msg_unpinned, campaign.title))
    }

    fun rename(campaign: Campaign, title: String) = viewModelScope.launch {
        campaigns.rename(campaign.id, title)
    }

    fun setNotes(campaign: Campaign, notes: String) = viewModelScope.launch {
        campaigns.setNotes(campaign.id, notes)
    }

    fun complete(campaign: Campaign) = viewModelScope.launch {
        campaigns.complete(campaign.id, today.value)
        _messages.tryEmit(uiMessage(R.string.msg_completed, campaign.title))
    }

    fun delete(campaign: Campaign) = viewModelScope.launch {
        campaigns.delete(campaign.id)
        _messages.tryEmit(uiMessage(R.string.msg_campaign_deleted, campaign.title))
    }

    fun addStep(campaign: Campaign, title: String) = viewModelScope.launch {
        steps.add(campaign.id, title)
    }

    fun toggleStep(step: Step) = viewModelScope.launch {
        steps.setCompleted(step.id, !step.completed, today.value)
    }

    fun deleteStep(step: Step) = viewModelScope.launch {
        steps.delete(step.id)
    }

    fun moveStep(campaign: Campaign, from: Int, to: Int) = viewModelScope.launch {
        steps.move(campaign.id, from, to)
    }
}
