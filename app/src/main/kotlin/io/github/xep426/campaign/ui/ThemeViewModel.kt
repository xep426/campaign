package io.github.xep426.campaign.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.xep426.campaign.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dusk or daylight, and nothing else.
 *
 * Its own view model rather than a flag on Today's, because the theme wraps
 * every tab and outlives all three: hanging it off one screen would mean
 * the shell could not read it without that screen being composed.
 *
 * [SharingStarted.Eagerly] on purpose. The palette is the first thing the
 * app draws, and WhileSubscribed would hand out the dusk default for a
 * frame before DataStore answers — which a user who chose daylight sees as
 * a black flash at every launch.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val dark: StateFlow<Boolean> = settings.darkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    fun toggle() = viewModelScope.launch {
        settings.setDarkTheme(!dark.value)
    }
}
