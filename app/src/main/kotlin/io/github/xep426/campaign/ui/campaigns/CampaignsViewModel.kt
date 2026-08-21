package io.github.xep426.campaign.ui.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.R
import io.github.xep426.campaign.ui.UiMessage
import io.github.xep426.campaign.ui.uiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class CampaignsViewModel @Inject constructor(
    private val campaigns: CampaignRepository,
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    // Today.s own tasks used to be folded in here, to grey out pulls once
    // the day was full. The card no longer offers a pull — carrying work
    // forward lives on Today, where the three slots it competes for are
    // actually visible — so this screen needs nothing but the campaigns.
    val ui: StateFlow<CampaignsUiState> = combine(
        date,
        campaigns.active(),
    ) { day, active ->
        CampaignsUiState(today = day, active = active)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampaignsUiState(),
    )

    fun refreshDate() {
        date.value = LocalDate.now()
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

    fun complete(campaign: Campaign) = viewModelScope.launch {
        campaigns.complete(campaign.id, date.value)
        _messages.tryEmit(uiMessage(R.string.msg_completed, campaign.title))
    }
}
