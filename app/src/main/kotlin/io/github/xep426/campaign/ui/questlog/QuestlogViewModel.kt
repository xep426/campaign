package io.github.xep426.campaign.ui.questlog

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

data class QuestlogUiState(
    val today: LocalDate = LocalDate.now(),
    /** The pinned campaigns, in slot order. At most [Campaign.SLOTS]. */
    val pinned: List<Campaign> = emptyList(),
    /**
     * Whether anything exists to pin.
     *
     * An empty questlog means two completely different things — "you have
     * not started anything" and "you have campaigns but none in front of
     * you" — and the screen says something different for each.
     */
    val hasAny: Boolean = false,
) {
    val freeSlots: Int get() = Campaign.SLOTS - pinned.size
}

/**
 * The questlog: at most three campaigns, each showing its next step.
 *
 * It replaced TodayViewModel, which held a clock, a campaign day, a wall
 * date, a planning flag and a thirty-day tally. None of that survives a
 * model where nothing turns over at a particular hour. What is left is a
 * date — only so the screen can say how long a campaign has been ignored.
 */
@HiltViewModel
class QuestlogViewModel @Inject constructor(
    private val campaigns: CampaignRepository,
    private val steps: StepRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val ui: StateFlow<QuestlogUiState> = combine(
        today,
        campaigns.active(),
    ) { day, active ->
        QuestlogUiState(
            today = day,
            pinned = active.filter { it.isPinned },
            hasAny = active.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuestlogUiState(),
    )

    /** Cheap, and a no-op unless the date actually rolled over. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun complete(step: Step) = viewModelScope.launch {
        steps.setCompleted(step.id, true, today.value)
        _messages.tryEmit(uiMessage(R.string.msg_step_done, step.title))
    }

    fun addStep(campaign: Campaign, title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        steps.add(campaign.id, title)
    }

    fun unpin(campaign: Campaign) = viewModelScope.launch {
        campaigns.unpin(campaign.id)
        _messages.tryEmit(uiMessage(R.string.msg_unpinned, campaign.title))
    }

    fun complete(campaign: Campaign) = viewModelScope.launch {
        campaigns.complete(campaign.id, today.value)
        _messages.tryEmit(uiMessage(R.string.msg_completed, campaign.title))
    }
}
