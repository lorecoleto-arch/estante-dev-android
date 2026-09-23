package com.estante.app.ui

import com.estante.app.domain.SearchResult
import com.estante.app.fakes.FakeBookRepository
import com.estante.app.fakes.FakePreferences
import com.estante.app.fakes.FakeShelfRepository
import com.estante.app.fakes.MainDispatcherRule
import com.estante.app.fakes.testBook
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.search.SearchResultsState
import com.estante.app.ui.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(testBook("A"), testBook("B")))
    private val preferences = FakePreferences(initialQuery = "kotlin")

    private fun createViewModel() = SearchViewModel(books, FakeShelfRepository(books), preferences)

    @Test
    fun `ao abrir repete a ultima busca salva`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("kotlin", state.query)
        assertEquals(2, (state.results as SearchResultsState.Content).books.size)
    }

    @Test
    fun `digitar rapido faz so uma busca - debounce cancela as anteriores`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        books.searchCalls.clear()

        vm.onQueryChange("c")
        vm.onQueryChange("cl")
        vm.onQueryChange("cla")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        advanceUntilIdle()

        assertEquals(listOf("cla"), books.searchCalls)
        assertEquals("cla", preferences.lastQuery.first())
    }

    @Test
    fun `termo curto demais mostra estado inicial sem chamar a API`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        books.searchCalls.clear()

        vm.onQueryChange("a")
        advanceUntilIdle()

        assertEquals(SearchResultsState.Idle, vm.uiState.value.results)
        assertTrue(books.searchCalls.isEmpty())
    }

    @Test
    fun `lista vazia e erro sao estados diferentes`() = runTest(mainDispatcherRule.dispatcher) {
        books.searchResult = { SearchResult(emptyList(), isStale = false) }
        val vm = createViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(SearchResultsState.Empty("kotlin"), vm.uiState.value.results)

        books.searchResult = { throw IOException("sem rede") }
        vm.onRetry()
        advanceUntilIdle()
        assertEquals(SearchResultsState.Error(ErrorKind.NO_CONNECTION), vm.uiState.value.results)
    }
}
