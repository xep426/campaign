package io.github.xep426.campaign.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    /** Finished campaigns — the long efforts that landed. */
    val finished: List<Campaign> = emptyList(),
    /** Finished tasks that were nobody's step but their own. */
    val tasks: List<DailyTask> = emptyList(),
)

// No date of its own any more. This screen shows finished campaigns and
// finished tasks, neither of which depends on which day it is — it held a
// LocalDate.now() that nothing read for exactly that reason.
@HiltViewModel
class HistoryViewModel @Inject constructor(
    tasks: TaskRepository,
    private val campaigns: CampaignRepository,
) : ViewModel() {

    val ui: StateFlow<HistoryUiState> = combine(
        tasks.completedStandalone(RECORD_LIMIT),
        campaigns.finished(),
    ) { done, finished ->
        HistoryUiState(finished = finished, tasks = done)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun reopen(campaign: Campaign) = viewModelScope.launch {
        campaigns.reopen(campaign.id)
    }

    private companion object {
        /**
         * Generous enough to be "all of it" for a single-user local app,
         * bounded so the query can never be unlimited. History is a record
         * to read back, not a dataset — if this ever needs paging, the app
         * has become something the PRD says it should not be.
         */
        const val RECORD_LIMIT = 500
    }
}
