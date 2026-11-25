package com.estante.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estante.app.data.BookRepository
import com.estante.app.data.ShelfRepository
import com.estante.app.data.preferences.UserPreferencesRepository
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.toErrorKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado dos resultados — cada caso vira uma tela visualmente diferente. */
sealed interface SearchResultsState {
    /** Ainda não há o que buscar (campo vazio ou curto demais). */
    data object Idle : SearchResultsState
    data object Loading : SearchResultsState
    data class Content(val books: List<Book>, val isStale: Boolean) : SearchResultsState
    data class Empty(val query: String) : SearchResultsState
    data class Error(val kind: ErrorKind) : SearchResultsState
}

data class SearchUiState(
    val results: SearchResultsState = SearchResultsState.Loading,
    /** Situação de cada livro que já está na estante (para mostrar o selo na lista). */
    val shelfStatus: Map<String, ReadingStatus> = emptyMap(),
)

/**
 * Fluxo unidirecional: a tela chama funções (eventos) -> o ViewModel atualiza o estado ->
 * a tela observa [uiState] e se redesenha.
 */
class SearchViewModel(
    private val bookRepository: BookRepository,
    shelfRepository: ShelfRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /**
     * Texto do campo de busca. Fica fora do [uiState] de propósito: o `combine` entrega o valor com
     * atraso, e um campo de texto atrasado embaralha as letras de quem digita rápido.
     */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val results = MutableStateFlow<SearchResultsState>(SearchResultsState.Loading)

    private val shelfStatus = shelfRepository.observeShelf()
        .map { items -> items.associate { it.book.id to it.entry.status } }
        .onStart { emit(emptyMap()) }
        .catch { emit(emptyMap()) } // sem os selos, mas a busca continua funcionando

    val uiState: StateFlow<SearchUiState> =
        combine(results, shelfStatus, ::SearchUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    /** Busca em andamento. Uma nova busca sempre cancela a anterior. */
    private var searchJob: Job? = null

    init {
        // Ao abrir o app, repete a última busca salva no DataStore.
        searchJob = viewModelScope.launch {
            val last = preferences.lastQuery.first()
            _query.value = last
            runSearch(last, forceRefresh = false)
        }
    }

    fun onQueryChange(text: String) {
        if (text.length > MAX_QUERY_LENGTH) return
        _query.value = text
        // Debounce: espera o usuário parar de digitar antes de chamar a API.
        launchSearch(delayMillis = DEBOUNCE_MILLIS, forceRefresh = false)
    }

    fun onSearch() = launchSearch(delayMillis = 0, forceRefresh = false)

    /** Toque num atalho de tema: preenche o campo e busca na hora (sem debounce). */
    fun onTopicSelected(topicQuery: String) {
        _query.value = topicQuery
        launchSearch(delayMillis = 0, forceRefresh = false)
    }

    fun onClearQuery() = onQueryChange("")

    /** "Tentar de novo" e "Atualizar" ignoram o cache e vão à API. */
    fun onRetry() = launchSearch(delayMillis = 0, forceRefresh = true)

    private fun launchSearch(delayMillis: Long, forceRefresh: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(delayMillis)
            runSearch(_query.value, forceRefresh)
        }
    }

    private suspend fun runSearch(text: String, forceRefresh: Boolean) {
        val term = text.trim()
        if (term.length < MIN_QUERY_LENGTH) {
            results.value = SearchResultsState.Idle
            return
        }
        results.value = SearchResultsState.Loading
        results.value = try {
            val result = bookRepository.search(term, forceRefresh)
            preferences.setLastQuery(term)
            if (result.books.isEmpty()) {
                SearchResultsState.Empty(term)
            } else {
                SearchResultsState.Content(result.books, result.isStale)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SearchResultsState.Error(e.toErrorKind())
        }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_QUERY_LENGTH = 100
        const val DEBOUNCE_MILLIS = 500L
    }
}
