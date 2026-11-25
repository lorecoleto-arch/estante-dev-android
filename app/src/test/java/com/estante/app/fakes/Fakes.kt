package com.estante.app.fakes

import com.estante.app.AppContainer
import com.estante.app.data.BookRepository
import com.estante.app.data.DescriptionState
import com.estante.app.data.ShelfRepository
import com.estante.app.data.preferences.UserPreferencesRepository
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.SearchResult
import com.estante.app.domain.ShelfEntry
import com.estante.app.domain.ShelfItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

fun testBook(
    id: String = "OL1W",
    title: String = "Código Limpo",
    pageCount: Int? = 256,
) = Book(
    id = id,
    title = title,
    authors = listOf("Robert C. Martin"),
    firstPublishYear = 1899,
    coverId = null, // sem capa: os testes não acessam a internet
    pageCount = pageCount,
    subjects = listOf("Engenharia de software"),
)

/** Repository falso em memória: controlamos o que a "API" devolve em cada teste. */
class FakeBookRepository(
    books: List<Book> = listOf(testBook()),
) : BookRepository {
    val catalog = MutableStateFlow(books.associateBy { it.id })
    private val descriptions = MutableStateFlow<Map<String, String?>>(emptyMap())

    var searchResult: suspend (String) -> SearchResult = { SearchResult(catalog.value.values.toList(), isStale = false) }
    val searchCalls = mutableListOf<String>()

    override suspend fun search(query: String, forceRefresh: Boolean): SearchResult {
        searchCalls += query
        return searchResult(query)
    }

    override fun observeBook(id: String): Flow<Book?> = catalog.map { it[id] }

    override fun observeDescription(id: String): Flow<DescriptionState> = descriptions.map { map ->
        if (map.containsKey(id)) DescriptionState.Loaded(map[id]) else DescriptionState.NotLoaded
    }

    override suspend fun refreshDescription(id: String, force: Boolean) {
        descriptions.update { it + (id to "Uma descrição de teste.") }
    }
}

class FakeShelfRepository(
    private val books: FakeBookRepository,
) : ShelfRepository {
    val entries = MutableStateFlow<Map<String, ShelfEntry>>(emptyMap())

    /** Quando preenchidos, simulam falha de leitura/gravação no banco. */
    var readError: Exception? = null
    var writeError: Exception? = null

    override fun observeShelf(): Flow<List<ShelfItem>> =
        combine(entries, books.catalog) { e, catalog ->
            e.values.sortedByDescending { it.updatedAt }.mapNotNull { entry ->
                catalog[entry.bookId]?.let { ShelfItem(it, entry) }
            }
        }.onStart { readError?.let { throw it } }

    override fun observeEntry(bookId: String): Flow<ShelfEntry?> =
        entries.map { it[bookId] }.onStart { readError?.let { throw it } }

    override suspend fun save(entry: ShelfEntry) {
        writeError?.let { throw it }
        entries.update { it + (entry.bookId to entry) }
    }

    override suspend fun remove(bookId: String) = entries.update { it - bookId }
}

class FakePreferences(initialQuery: String = "kotlin") : UserPreferencesRepository {
    private val query = MutableStateFlow(initialQuery)
    private val filter = MutableStateFlow<ReadingStatus?>(null)
    override val lastQuery: Flow<String> = query
    override val shelfFilter: Flow<ReadingStatus?> = filter
    override suspend fun setLastQuery(query: String) { this.query.value = query }
    override suspend fun setShelfFilter(status: ReadingStatus?) { filter.value = status }
}

class FakeAppContainer(
    override val bookRepository: FakeBookRepository = FakeBookRepository(),
    override val shelfRepository: FakeShelfRepository = FakeShelfRepository(bookRepository),
    override val preferences: FakePreferences = FakePreferences(),
) : AppContainer
