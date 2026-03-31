package net.pythonsden.ao3_.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        private val LAST_ROUTE = stringPreferencesKey("last_route")
        private val LAST_URL = stringPreferencesKey("last_url")
        private val LAST_EPUB_PATH = stringPreferencesKey("last_epub_path")
    }

    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE] ?: false
    }

    val lastRouteFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_ROUTE]
    }

    val lastUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_URL] ?: "https://archiveofourown.org/"
    }

    val lastEpubPathFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_EPUB_PATH]
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OFFLINE_MODE] = enabled
        }
    }

    suspend fun updateLastRoute(route: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ROUTE] = route
        }
    }

    suspend fun updateLastUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_URL] = url
        }
    }

    suspend fun updateLastEpubPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_EPUB_PATH] = path
        }
    }

    fun getReaderScrollFlow(filePath: String): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[intPreferencesKey("scroll_$filePath")] ?: 0
    }

    suspend fun updateReaderScroll(filePath: String, scrollY: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey("scroll_$filePath")] = scrollY
        }
    }
}
