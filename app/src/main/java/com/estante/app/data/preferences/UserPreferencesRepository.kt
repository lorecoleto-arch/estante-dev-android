package com.estante.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.estante.app.domain.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

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

    /** Preferências não são essenciais: se o arquivo não puder ser lido, valem os valores padrão. */
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val lastQuery: Flow<String> =
        preferences.map { it[LAST_QUERY] ?: DEFAULT_QUERY }

    override val shelfFilter: Flow<ReadingStatus?> =
        preferences.map { prefs ->
            prefs[SHELF_FILTER]?.let { name -> ReadingStatus.entries.firstOrNull { it.name == name } }
        }

    override suspend fun setLastQuery(query: String) {
        save { it[LAST_QUERY] = query }
    }

    override suspend fun setShelfFilter(status: ReadingStatus?) {
        save { prefs ->
            if (status == null) prefs.remove(SHELF_FILTER) else prefs[SHELF_FILTER] = status.name
        }
    }

    private suspend fun save(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { change(it) }
        } catch (_: IOException) {
            // A preferência só não será lembrada; o restante do app continua funcionando.
        }
    }

    companion object {
        const val DEFAULT_QUERY = "clean code"
        private val LAST_QUERY = stringPreferencesKey("last_query")
        private val SHELF_FILTER = stringPreferencesKey("shelf_filter")
    }
}
