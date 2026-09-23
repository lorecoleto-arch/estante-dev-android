package com.estante.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.estante.app.domain.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências simples do usuário: última busca e filtro da estante. */
interface UserPreferencesRepository {
    val lastQuery: Flow<String>
    val shelfFilter: Flow<ReadingStatus?>
    suspend fun setLastQuery(query: String)
    suspend fun setShelfFilter(status: ReadingStatus?)
}

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val lastQuery: Flow<String> =
        dataStore.data.map { it[LAST_QUERY] ?: DEFAULT_QUERY }

    override val shelfFilter: Flow<ReadingStatus?> =
        dataStore.data.map { prefs ->
            prefs[SHELF_FILTER]?.let { name -> ReadingStatus.entries.firstOrNull { it.name == name } }
        }

    override suspend fun setLastQuery(query: String) {
        dataStore.edit { it[LAST_QUERY] = query }
    }

    override suspend fun setShelfFilter(status: ReadingStatus?) {
        dataStore.edit { prefs ->
            if (status == null) prefs.remove(SHELF_FILTER) else prefs[SHELF_FILTER] = status.name
        }
    }

    companion object {
        const val DEFAULT_QUERY = "clean code"
        private val LAST_QUERY = stringPreferencesKey("last_query")
        private val SHELF_FILTER = stringPreferencesKey("shelf_filter")
    }
}
