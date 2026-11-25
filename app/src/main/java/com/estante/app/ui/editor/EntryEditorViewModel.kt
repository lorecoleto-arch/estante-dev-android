package com.estante.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estante.app.data.BookRepository
import com.estante.app.data.ShelfRepository
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.domain.ShelfEntryValidator
import com.estante.app.domain.ValidationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Valores digitados no formulário (texto cru, como o usuário digitou). */
data class EntryForm(
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val pagesRead: String = "",
    val rating: Int? = null,
    val note: String = "",
)

enum class EditorResult { SAVED, REMOVED }

data class EntryEditorUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** O livro não está (mais) salvo no aparelho, então não há o que registrar. */
    val bookNotFound: Boolean = false,
    val bookTitle: String = "",
    val totalPages: Int? = null,
    val isExistingEntry: Boolean = false,
    val form: EntryForm = EntryForm(),
    val validation: ValidationResult = ValidationResult(),
    /** Os erros só aparecem depois da primeira tentativa de salvar (não assusta quem está digitando). */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    /** Quando preenchido, a tela fecha. */
    val result: EditorResult? = null,
)

class EntryEditorViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val shelfRepository: ShelfRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryEditorUiState())
    val uiState: StateFlow<EntryEditorUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetryLoad() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                loadForm()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private suspend fun loadForm() {
        val book = bookRepository.observeBook(bookId).first()
        if (book == null) {
            _uiState.update { it.copy(isLoading = false, bookNotFound = true) }
            return
        }
        val entry = shelfRepository.observeEntry(bookId).first()
        val form = entry?.let {
            EntryForm(
                status = it.status,
                pagesRead = it.pagesRead?.toString().orEmpty(),
                rating = it.rating,
                note = it.note.orEmpty(),
            )
        } ?: EntryForm()
        _uiState.update {
            it.copy(
                isLoading = false,
                bookTitle = book.title,
                totalPages = book.pageCount,
                isExistingEntry = entry != null,
                form = form,
                validation = validate(form, book.pageCount),
            )
        }
    }

    fun onStatusChange(status: ReadingStatus) = updateForm { it.copy(status = status) }

    fun onPagesChange(text: String) {
        if (text.length > MAX_PAGES_DIGITS) return
        updateForm { it.copy(pagesRead = text) }
    }

    fun onRatingChange(rating: Int) = updateForm { it.copy(rating = rating) }

    fun onNoteChange(text: String) = updateForm { it.copy(note = text) }

    fun onSave() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.bookNotFound) return // evita salvar duas vezes com toque duplo

        val validation = validate(state.form, state.totalPages)
        if (!validation.isValid) {
            _uiState.update { it.copy(validation = validation, showErrors = true) }
            return
        }

        val form = state.form
        val entry = ShelfEntry(
            bookId = bookId,
            status = form.status,
            pagesRead = when (form.status) {
                ReadingStatus.READING -> form.pagesRead.trim().toInt()
                ReadingStatus.FINISHED -> state.totalPages
                ReadingStatus.WANT_TO_READ -> null
            },
            rating = form.rating.takeIf { form.status == ReadingStatus.FINISHED },
            note = form.note.trim().takeIf { it.isNotEmpty() },
            updatedAt = clock(),
        )
        persist(EditorResult.SAVED) { shelfRepository.save(entry) }
    }

    fun onRemove() {
        if (_uiState.value.isSaving) return
        persist(EditorResult.REMOVED) { shelfRepository.remove(bookId) }
    }

    private fun persist(result: EditorResult, block: suspend () -> Unit) {
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                block()
                _uiState.update { it.copy(isSaving = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    private fun updateForm(change: (EntryForm) -> EntryForm) {
        _uiState.update { state ->
            val form = change(state.form)
            state.copy(form = form, validation = validate(form, state.totalPages), saveFailed = false)
        }
    }

    private fun validate(form: EntryForm, totalPages: Int?) = ShelfEntryValidator.validate(
        status = form.status,
        pagesRead = form.pagesRead,
        rating = form.rating,
        note = form.note,
        totalPages = totalPages,
    )

    private companion object {
        const val MAX_PAGES_DIGITS = 6
    }
}
