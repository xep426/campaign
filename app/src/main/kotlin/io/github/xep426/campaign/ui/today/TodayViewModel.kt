package io.github.xep426.campaign.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xep426.campaign.R
import io.github.xep426.campaign.data.settings.SettingsRepository
import io.github.xep426.campaign.domain.model.Campaign
import io.github.xep426.campaign.domain.model.CampaignDay
import io.github.xep426.campaign.domain.model.DailyTask
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.TaskRepository
import io.github.xep426.campaign.notify.EndOfDayScheduler
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    /** Always three entries; null is an open slot. */
    val slots: List<DailyTask?> = List(DailyTask.SLOTS_PER_DAY) { null },
    val activeCampaigns: List<Campaign> = emptyList(),
    /** When the day turns, which is also when the app asks. */
    val turnsAt: LocalTime = CampaignDay.DEFAULT_TURN,
    val notificationsEnabled: Boolean = true,
    /**
     * The day on screen has not started on the wall clock yet â€” the window
     * between the turn and midnight.
     *
     * In it the three are a PLAN, not a checklist. Completing one would be
     * a claim to have finished something on a day that has not begun, and
     * the app should not offer to record that. Editing them is the act
     * that makes sense in this window, so editing is what a tap does.
     */
    val isPlanning: Boolean = false,
) {
    val completed: Int get() = slots.count { it?.completed == true }
    val filled: Int get() = slots.count { it != null }
    val hasOpenSlot: Boolean get() = slots.any { it == null }

    /**
     * Outstanding campaign work sitting on some other day.
     *
     * Excludes today's own tasks: they are already here, and offering to
     * carry a task onto the day it is already on is an action that can
     * only be a no-op.
     */
    val carryable: List<Campaign>
        get() = activeCampaigns
            .map { c -> c.copy(openTasks = c.openTasks.filter { it.date != date }) }
            .filter { it.openTasks.isNotEmpty() }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val campaigns: CampaignRepository,
    private val settings: SettingsRepository,
    private val scheduler: EndOfDayScheduler,
) : ViewModel() {

    /**
     * The day the app is currently in â€” NOT the calendar date.
     *
     * It turns at the user's chosen hour (22:00 by default), so between
     * 22:00 and midnight this already reads as tomorrow. That is the whole
     * simplification: there is one list, and after the turn it is a fresh
     * one. See [CampaignDay].
     *
     * Re-stamped on every resume, because an app left open across the turn
     * would otherwise keep showing the finished day and, worse, write a
     * newly typed task onto it.
     */
    private val clock = MutableStateFlow(Clock(LocalDate.now(), LocalDate.now()))

    /**
     * Distinct so a re-stamp that lands on the same day does not re-run the
     * task query â€” but [clock] itself still emits, because midnight changes
     * [Clock.wall] without changing [Clock.day] and the planning window
     * closes on exactly that.
     */
    private val date = clock.map { it.day }.distinctUntilChanged()

    init {
        // Follows the setting as well as the clock: moving the turn earlier
        // in the evening must move the list with it, not at the next resume.
        viewModelScope.launch {
            settings.endOfDay.collect { clock.value = clockNow(it.time) }
        }
    }

    private fun clockNow(turnsAt: LocalTime): Clock {
        val now = LocalDateTime.now()
        return Clock(day = CampaignDay.of(now, turnsAt), wall = now.toLocalDate())
    }

    /** The two dates that differ only inside the planning window. */
    private data class Clock(val day: LocalDate, val wall: LocalDate)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val ui: StateFlow<TodayUiState> = combine(
        clock,
        date.flatMapLatest { tasks.tasksFor(it) },
        campaigns.active(),
        settings.endOfDay,
    ) { now, dayTasks, active, setting ->
        TodayUiState(
            date = now.day,
            isPlanning = now.day != now.wall,
            slots = (0 until DailyTask.SLOTS_PER_DAY).map { slot ->
                dayTasks.firstOrNull { it.slot == slot }
            },
            activeCampaigns = active,
            turnsAt = setting.time,
            notificationsEnabled = setting.enabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    /** Called on resume â€” cheap, and a no-op unless something actually moved. */
    fun refreshDate() = viewModelScope.launch {
        clock.value = clockNow(settings.endOfDay.first().time)
    }

    fun setTurnTime(time: LocalTime) = viewModelScope.launch {
        settings.setTime(time)
        // force: the user just changed what they want, so whatever booking
        // is live is by definition the wrong one.
        scheduler.reschedule(force = true)
        _messages.tryEmit(
            uiMessage(R.string.msg_asking_at, "%02d:%02d".format(time.hour, time.minute))
        )
    }

    fun toggle(task: DailyTask) = viewModelScope.launch {
        tasks.toggleCompleted(task.id)
    }

    fun setSlot(slot: Int, title: String) = viewModelScope.launch {
        tasks.setSlot(clock.value.day, slot, title)
    }

    /**
     * Drag-and-drop landed: commit the new order.
     *
     * Only reached on drop, never during the drag — the column keeps the
     * intermediate order to itself so one gesture is one write, and the
     * widget does not flicker through positions the user passed over.
     */
    fun moveSlot(from: Int, to: Int) = viewModelScope.launch {
        tasks.reorder(clock.value.day, from, to)
    }

    fun delete(task: DailyTask) = viewModelScope.launch {
        tasks.delete(task.id)
        _messages.tryEmit(uiMessage(R.string.msg_removed))
    }

    /** Links the task to an existing campaign. */
    fun assign(task: DailyTask, campaign: Campaign) = viewModelScope.launch {
        campaigns.assign(task.id, campaign.id)
        _messages.tryEmit(uiMessage(R.string.msg_assigned, campaign.title))
    }

    /** Creates a campaign named [title] and links the task to it. */
    fun promote(task: DailyTask, title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        campaigns.promote(task.id, title, clock.value.day)
        _messages.tryEmit(uiMessage(R.string.msg_assigned, title.trim()))
    }

    fun unassign(task: DailyTask) = viewModelScope.launch {
        campaigns.assign(task.id, null)
        _messages.tryEmit(uiMessage(R.string.msg_unassigned))
    }

    fun carryForward(task: DailyTask) = viewModelScope.launch {
        val slot = campaigns.carryForward(task.id, clock.value.day)
        _messages.tryEmit(
            if (slot == null) uiMessage(R.string.msg_no_open_slot)
            else uiMessage(R.string.msg_pulled, DailyTask.numeralFor(slot))
        )
    }
}

