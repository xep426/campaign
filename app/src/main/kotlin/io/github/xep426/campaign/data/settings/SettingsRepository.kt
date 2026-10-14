package io.github.xep426.campaign.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "campaign_settings")

/**
 * What little the app stores that is not a campaign.
 *
 * It used to hold the hour the day turned, which was also the hour the
 * notification fired — one number doing two jobs on purpose. Both jobs
 * are gone: campaigns do not roll over, and without a rollover there is
 * nothing for an evening prompt to announce.
 *
 * What is left is the theme.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val store get() = context.settingsDataStore

    /**
     * Dusk or daylight.
     *
     * Stored rather than read from the system night flag, and that is the
     * point: the app has an opinion about which it wants (see
     * [io.github.xep426.campaign.ui.theme.CampaignTheme]) and a user who
     * runs their phone in light mode has not thereby asked this screen to
     * be white at night. Defaults to dusk.
     */
    val darkTheme: Flow<Boolean> = store.data.map { it[KEY_DARK] ?: true }

    suspend fun setDarkTheme(dark: Boolean) {
        store.edit { it[KEY_DARK] = dark }
    }

    private companion object {
        val KEY_DARK = booleanPreferencesKey("dark_theme")
        // The end-of-day keys are gone with the notification. Their rows
        // may still sit in the store on an upgraded install; DataStore
        // ignores keys nothing reads, and clearing them would be a
        // migration written to tidy bytes nobody can see.
    }
}
