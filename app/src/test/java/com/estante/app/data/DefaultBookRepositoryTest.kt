package com.estante.app.data

import com.estante.app.data.local.BookDao
import com.estante.app.data.local.BookDescriptionEntity
import com.estante.app.data.local.BookEntity
import com.estante.app.data.local.SearchQueryEntity
import com.estante.app.data.local.SearchResultEntity
import com.estante.app.data.remote.BookRemoteDataSource
import com.estante.app.domain.Book
import com.estante.app.fakes.testBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultBookRepositoryTest {

    private val remote = FakeRemote()
    private val dao = InMemoryBookDao()
    private var now = 1_000_000L
    private val repository = DefaultBookRepository(remote, dao, clock = { now })

    @Test
    fun `primeira busca vai na API e salva no cache`() = runTest {
        remote.books = listOf(testBook("A"), testBook("B"))

        val result = repository.search("  Clean  Code ")

        assertEquals(listOf("A", "B"), result.books.map { it.id })
        assertFalse(result.isStale)
        assertEquals(listOf("clean code"), remote.queries) // termo normalizado
        assertEquals(2, dao.getSearchResults("clean code").size)
    }

    @Test
    fun `cache valido evita nova chamada a API`() = runTest {
        remote.books = listOf(testBook("A"))
        repository.search("kotlin")
        now += DefaultBookRepository.CACHE_TTL_MILLIS - 1

        repository.search("kotlin")

        assertEquals(1, remote.queries.size)
    }

    @Test
    fun `cache expirado ou forceRefresh consulta a API de novo`() = runTest {
        remote.books = listOf(testBook("A"))
        repository.search("kotlin")
        repository.search("kotlin", forceRefresh = true)
        now += DefaultBookRepository.CACHE_TTL_MILLIS
        repository.search("kotlin")

        assertEquals(3, remote.queries.size)
    }

    @Test
    fun `sem internet devolve o cache marcado como desatualizado`() = runTest {
        remote.books = listOf(testBook("A"))
        repository.search("kotlin")
        remote.error = IOException("sem rede")

        val result = repository.search("kotlin", forceRefresh = true)

        assertTrue(result.isStale)
        assertEquals(listOf("A"), result.books.map { it.id })
    }

    @Test(expected = IOException::class)
    fun `sem internet e sem cache propaga o erro`() = runTest {
        remote.error = IOException("sem rede")
        repository.search("kotlin")
    }

    @Test
    fun `descricao so e buscada uma vez`() = runTest {
        repository.refreshDescription("A")
        repository.refreshDescription("A")
        assertEquals(1, remote.descriptionCalls)
    }

    // --- Fakes das fontes de dados ---

    private class FakeRemote : BookRemoteDataSource {
        var books: List<Book> = emptyList()
        var error: Exception? = null
        val queries = mutableListOf<String>()
        var descriptionCalls = 0

        override suspend fun search(query: String): List<Book> {
            error?.let { throw it }
            queries += query
            return books
        }

        override suspend fun fetchDescription(bookId: String): String? {
            descriptionCalls++
            return "descrição"
        }
    }

    private class InMemoryBookDao : BookDao {
        private val books = MutableStateFlow<Map<String, BookEntity>>(emptyMap())
        private val results = mutableListOf<SearchResultEntity>()
        private val queries = mutableMapOf<String, Long>()
        private val descriptions = MutableStateFlow<Map<String, BookDescriptionEntity>>(emptyMap())

        override suspend fun getSearchResults(query: String) =
            results.filter { it.query == query }.sortedBy { it.position }.mapNotNull { books.value[it.bookId] }

        override suspend fun getFetchedAt(query: String) = queries[query]

        override suspend fun upsertBooks(books: List<BookEntity>) {
            this.books.value = this.books.value + books.associateBy { it.id }
        }

        override suspend fun upsertSearchResults(results: List<SearchResultEntity>) {
            this.results += results
        }

        override suspend fun upsertSearchQuery(query: SearchQueryEntity) {
            queries[query.query] = query.fetchedAt
        }

        override suspend fun clearSearchResults(query: String) {
            results.removeAll { it.query == query }
        }

        override fun observeBook(id: String): Flow<BookEntity?> = books.map { it[id] }

        override suspend fun getBook(id: String) = books.value[id]

        override fun observeDescription(id: String) = descriptions.map { it[id] }

        override suspend fun hasDescription(id: String) = if (descriptions.value.containsKey(id)) 1 else 0

        override suspend fun upsertDescription(description: BookDescriptionEntity) {
            descriptions.value = descriptions.value + (description.bookId to description)
        }
    }
}
