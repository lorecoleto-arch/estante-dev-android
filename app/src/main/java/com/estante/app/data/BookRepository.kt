package com.estante.app.data

import com.estante.app.data.local.BookDao
import com.estante.app.data.local.BookDescriptionEntity
import com.estante.app.data.local.toDomain
import com.estante.app.data.local.toEntity
import com.estante.app.data.remote.BookRemoteDataSource
import com.estante.app.domain.Book
import com.estante.app.domain.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BookRepository {
    /**
     * Busca livros. Usa o cache local se ele ainda for válido; senão consulta a API.
     * Se a API falhar mas houver cache, devolve o cache marcado como desatualizado.
     * Se não houver nada, lança a exceção para a tela mostrar o estado de erro.
     */
    suspend fun search(query: String, forceRefresh: Boolean = false): SearchResult

    fun observeBook(id: String): Flow<Book?>

    /** Descrição salva localmente. Emite [DescriptionState.NotLoaded] enquanto não foi buscada. */
    fun observeDescription(id: String): Flow<DescriptionState>

    /** Busca a descrição na API só se ainda não estiver salva (ou se [force] = true). */
    suspend fun refreshDescription(id: String, force: Boolean = false)
}

sealed interface DescriptionState {
    data object NotLoaded : DescriptionState
    data class Loaded(val text: String?) : DescriptionState
}

class DefaultBookRepository(
    private val remote: BookRemoteDataSource,
    private val bookDao: BookDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : BookRepository {

    override suspend fun search(query: String, forceRefresh: Boolean): SearchResult {
        val key = normalize(query)
        val fetchedAt = bookDao.getFetchedAt(key)
        val cacheIsFresh = fetchedAt != null && clock() - fetchedAt < CACHE_TTL_MILLIS

        if (cacheIsFresh && !forceRefresh) {
            return SearchResult(bookDao.getSearchResults(key).map { it.toDomain() }, isStale = false)
        }

        return try {
            val books = remote.search(key)
            bookDao.replaceSearch(key, books.map { it.toEntity() }, clock())
            SearchResult(books, isStale = false)
        } catch (e: CancellationException) {
            throw e // nunca "engolir" o cancelamento da coroutine
        } catch (e: Exception) {
            val cached = bookDao.getSearchResults(key)
            if (cached.isEmpty()) throw e
            SearchResult(cached.map { it.toDomain() }, isStale = true)
        }
    }

    override fun observeBook(id: String): Flow<Book?> =
        bookDao.observeBook(id).map { it?.toDomain() }

    override fun observeDescription(id: String): Flow<DescriptionState> =
        bookDao.observeDescription(id).map { entity ->
            if (entity == null) DescriptionState.NotLoaded else DescriptionState.Loaded(entity.description)
        }

    override suspend fun refreshDescription(id: String, force: Boolean) {
        if (!force && bookDao.hasDescription(id) > 0) return
        val description = remote.fetchDescription(id)
        bookDao.upsertDescription(BookDescriptionEntity(id, description))
    }

    companion object {
        /** Política de expiração: resultados de busca valem por 1 hora. */
        const val CACHE_TTL_MILLIS = 60 * 60 * 1000L

        fun normalize(query: String): String = query.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
