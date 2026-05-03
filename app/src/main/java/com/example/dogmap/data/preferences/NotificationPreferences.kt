package com.example.dogmap.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class NotificationSettings(
    val globalEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val dndStartHour: Int = -1,
    val dndEndHour: Int = -1
)

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

class NotificationPreferences(private val context: Context) {

    private object Keys {
        val GLOBAL_ENABLED = booleanPreferencesKey("global_enabled")
        val SOCIAL_ENABLED = booleanPreferencesKey("social_enabled")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val DND_START = intPreferencesKey("dnd_start_hour")
        val DND_END = intPreferencesKey("dnd_end_hour")
    }

    val settingsFlow: Flow<NotificationSettings> = context.notifDataStore.data.map { prefs ->
        NotificationSettings(
            globalEnabled = prefs[Keys.GLOBAL_ENABLED] ?: true,
            socialEnabled = prefs[Keys.SOCIAL_ENABLED] ?: true,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            dndStartHour = prefs[Keys.DND_START] ?: -1,
            dndEndHour = prefs[Keys.DND_END] ?: -1
        )
    }

    suspend fun update(block: (NotificationSettings) -> NotificationSettings) {
        context.notifDataStore.edit { prefs ->
            val current = NotificationSettings(
                globalEnabled = prefs[Keys.GLOBAL_ENABLED] ?: true,
                socialEnabled = prefs[Keys.SOCIAL_ENABLED] ?: true,
                remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
                dndStartHour = prefs[Keys.DND_START] ?: -1,
                dndEndHour = prefs[Keys.DND_END] ?: -1
            )
            val updated = block(current)
            prefs[Keys.GLOBAL_ENABLED] = updated.globalEnabled
            prefs[Keys.SOCIAL_ENABLED] = updated.socialEnabled
            prefs[Keys.REMINDERS_ENABLED] = updated.remindersEnabled
            prefs[Keys.DND_START] = updated.dndStartHour
            prefs[Keys.DND_END] = updated.dndEndHour
        }
    }
}
