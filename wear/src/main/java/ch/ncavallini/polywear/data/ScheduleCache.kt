package ch.ncavallini.polywear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_cache")

/**
 * Persists the last successful raw schedule JSON per semester so the watch can
 * show something when offline or briefly unauthenticated.
 */
class ScheduleCache(private val context: Context) {

    data class Entry(val rawJson: String, val fetchedAtEpochMillis: Long)

    private fun jsonKey(semkez: String) = stringPreferencesKey("json_$semkez")
    private fun timeKey(semkez: String) = longPreferencesKey("time_$semkez")

    suspend fun save(semkez: String, rawJson: String) {
        context.scheduleDataStore.edit { prefs ->
            prefs[jsonKey(semkez)] = rawJson
            prefs[timeKey(semkez)] = System.currentTimeMillis()
        }
    }

    suspend fun load(semkez: String): Entry? {
        val prefs = context.scheduleDataStore.data.first()
        val json = prefs[jsonKey(semkez)] ?: return null
        val time = prefs[timeKey(semkez)] ?: 0L
        return Entry(json, time)
    }
}
