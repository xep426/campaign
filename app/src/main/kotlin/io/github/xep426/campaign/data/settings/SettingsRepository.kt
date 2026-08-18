package io.github.xep426.campaign.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.xep426.campaign.domain.model.CampaignDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "campaign_settings")

/**
 * When the day turns — and, at the same instant, when the app asks.
 *
 * ONE NUMBER, TWO JOBS, deliberately. [time] is both the moment the task
 * list rolls over to a fresh day (see
 * [io.github.xep426.campaign.domain.model.CampaignDay]) and the moment the
 * notification fires. Splitting them would let the app prompt you to
 * choose tomorrow's three things while still showing you today's — exactly
 * the mismatch that used to need a whole second screen to paper over.
 *
 * 22:00 by default: late enough that the day is genuinely over, early
 * enough that the choice is still made awake. Asking at 23:30 gets three
 * tasks typed to make the notification go away.
 */
data class EndOfDaySetting(
    val enabled: Boolean = true,
    val time: LocalTime = CampaignDay.DEFAULT_TURN,
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val store get() = context.settingsDataStore

    val endOfDay: Flow<EndOfDaySetting> = store.data.map { prefs ->
        EndOfDaySetting(
            enabled = prefs[KEY_ENABLED] ?: true,
            time = LocalTime.of(
                (prefs[KEY_HOUR] ?: CampaignDay.DEFAULT_TURN.hour).coerceIn(0, 23),
                (prefs[KEY_MINUTE] ?: 0).coerceIn(0, 59),
            ),
        )
    }

    suspend fun setTime(time: LocalTime) {
        store.edit {
            it[KEY_HOUR] = time.hour
            it[KEY_MINUTE] = time.minute
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    /**
     * The instant the currently booked alarm is aimed at, or 0 for none.
     *
     * Persisted because AlarmManager will not tell us what we booked — it
     * only says whether a PendingIntent exists. Without this the scheduler
     * cannot tell "an alarm for tonight is still waiting to fire" from
     * "a stale alarm from last week", and it needs to, or opening the app
     * cancels a prompt that was about to arrive. See [EndOfDayScheduler].
     */
    val scheduledFor: Flow<Long> = store.data.map { it[KEY_SCHEDULED_FOR] ?: 0L }

    suspend fun setScheduledFor(epochMillis: Long) {
        store.edit { it[KEY_SCHEDULED_FOR] = epochMillis }
    }

    private companion object {
        val KEY_HOUR = intPreferencesKey("end_of_day_hour")
        val KEY_MINUTE = intPreferencesKey("end_of_day_minute")
        val KEY_ENABLED = booleanPreferencesKey("end_of_day_enabled")
        val KEY_SCHEDULED_FOR = longPreferencesKey("end_of_day_scheduled_for")
    }
}
