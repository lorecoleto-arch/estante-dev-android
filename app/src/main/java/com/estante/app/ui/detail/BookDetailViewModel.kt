package com.estante.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estante.app.data.BookRepository
import com.estante.app.data.DescriptionState
import com.estante.app.data.ShelfRepository
import com.estante.app.domain.Book
import com.estante.app.domain.ShelfEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DescriptionUi {
    data object Loading : DescriptionUi
    data class Loaded(val text: String?) : DescriptionUi
    data object Error : DescriptionUi
}

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data object NotFound : BookDetailUiState
    data object Error : BookDetailUiState
    data class Content(
        val book: Book,
        /** `null` = o livro ainda não está na estante. */
        val entry: ShelfEntry?,
        val description: DescriptionUi,
    ) : BookDetailUiState
}

class BookDetailViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val shelfRepository: ShelfRepository,
) : ViewModel() {

    private val descriptionFailed = MutableStateFlow(false)
    private var descriptionJob: Job? = null

    /** Cada toque em "Tentar de novo" muda este número, e isso refaz a leitura do banco. */
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BookDetailUiState> = attempt
        .flatMapLatest { observeBook() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailUiState.Loading)

    /** Junta três fontes reativas: livro (cache), registro na estante e descrição. */
    private fun observeBook(): Flow<BookDetailUiState> = combine(
        bookRepository.observeBook(bookId),
        shelfRepository.observeEntry(bookId),
        bookRepository.observeDescription(bookId),
        descriptionFailed,
    ) { book, entry, description, failed ->
        if (book == null) {
            BookDetailUiState.NotFound
        } else {
            val descriptionUi = when {
                description is DescriptionState.Loaded -> DescriptionUi.Loaded(description.text)
                failed -> DescriptionUi.Error
                else -> DescriptionUi.Loading
            }
            BookDetailUiState.Content(book, entry, descriptionUi)
        }
    }.catch { emit(BookDetailUiState.Error) }

    init {
        loadDescription()
    }

    fun onRetry() {
        attempt.update { it + 1 }
        loadDescription()
    }

    fun onRetryDescription() = loadDescription()

    /** Roda no viewModelScope: se o usuário sair da tela, a requisição é cancelada junto. */
    private fun loadDescription() {
        descriptionJob?.cancel()
        descriptionJob = viewModelScope.launch {
            descriptionFailed.value = false
            try {
                bookRepository.refreshDescription(bookId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                descriptionFailed.value = true
            }
        }
    }
}
