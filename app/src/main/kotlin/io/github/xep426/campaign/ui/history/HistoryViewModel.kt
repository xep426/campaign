package io.github.xep426.campaign.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.data.settings.SettingsRepository
import io.github.xep426.campaign.domain.model.CampaignDay
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.model.Progress
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class HistoryUiState(
    /** Finished campaigns — the long efforts that landed. */
    val finished: List<Campaign> = emptyList(),
    /** Finished tasks that were nobody's step but their own. */
    val tasks: List<DailyTask> = emptyList(),
    /** The tally at the top of the screen. */
    val progress: Progress = Progress(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    tasks: TaskRepository,
    private val campaigns: CampaignRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /**
     * The CAMPAIGN day, not the wall date.
     *
     * This screen used to hold LocalDate.now() and never read it. Now that
     * the tally measures a window ending today, the difference matters:
     * between the turn and midnight the wall date is a day behind the
     * records, and a window that ended there would drop the day currently
     * being filled.
     */
    private val day = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            settings.endOfDay.collect {
                day.value = CampaignDay.of(LocalDateTime.now(), it.time)
            }
        }
    }

    val ui: StateFlow<HistoryUiState> = combine(
        tasks.completedStandalone(RECORD_LIMIT),
        campaigns.finished(),
        day.flatMapLatest { tasks.progress(it) },
    ) { done, finished, progress ->
        HistoryUiState(finished = finished, tasks = done, progress = progress)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun refreshDate() = viewModelScope.launch {
        day.value = CampaignDay.of(LocalDateTime.now(), settings.endOfDay.first().time)
    }

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
