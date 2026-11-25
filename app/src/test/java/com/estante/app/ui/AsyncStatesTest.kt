package com.estante.app.ui

import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.fakes.FakeBookRepository
import com.estante.app.fakes.FakePreferences
import com.estante.app.fakes.FakeShelfRepository
import com.estante.app.fakes.MainDispatcherRule
import com.estante.app.fakes.testBook
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.toErrorKind
import com.estante.app.ui.detail.BookDetailUiState
import com.estante.app.ui.detail.BookDetailViewModel
import com.estante.app.ui.editor.EditorResult
import com.estante.app.ui.editor.EntryEditorViewModel
import com.estante.app.ui.shelf.ShelfUiState
import com.estante.app.ui.shelf.ShelfViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Carregando, sucesso, vazio e erro (com "tentar de novo") nas telas que leem o banco local. */
@OptIn(ExperimentalCoroutinesApi::class)
class AsyncStatesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(testBook("A")))
    private val shelf = FakeShelfRepository(books)

    @Test
    fun `estante comeca carregando e depois mostra lista vazia`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = ShelfViewModel(shelf, FakePreferences())
        assertEquals(ShelfUiState.Loading, vm.uiState.value)

        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as ShelfUiState.Loaded).total)
    }

    @Test
    fun `erro na estante se recupera com tentar de novo`() = runTest(mainDispatcherRule.dispatcher) {
        shelf.readError = IllegalStateException("banco indisponível")
        val vm = ShelfViewModel(shelf, FakePreferences())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(ShelfUiState.Error, vm.uiState.value)

        shelf.readError = null
        vm.onRetry()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is ShelfUiState.Loaded)
    }

    @Test
    fun `erro nos detalhes se recupera com tentar de novo`() = runTest(mainDispatcherRule.dispatcher) {
        shelf.readError = IllegalStateException("banco indisponível")
        val vm = BookDetailViewModel("A", books, shelf)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(BookDetailUiState.Error, vm.uiState.value)

        shelf.readError = null
        vm.onRetry()
        advanceUntilIdle()

        assertEquals("A", (vm.uiState.value as BookDetailUiState.Content).book.id)
    }

    @Test
    fun `detalhes de livro que nao existe mostram nao encontrado`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = BookDetailViewModel("inexistente", books, shelf)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(BookDetailUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun `erro ao carregar o formulario se recupera com tentar de novo`() = runTest(mainDispatcherRule.dispatcher) {
        shelf.readError = IllegalStateException("banco indisponível")
        val vm = EntryEditorViewModel("A", books, shelf)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)

        shelf.readError = null
        vm.onRetryLoad()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.loadFailed)
        assertEquals("Código Limpo", vm.uiState.value.bookTitle)
    }

    @Test
    fun `formulario de livro que nao existe nao permite salvar`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = EntryEditorViewModel("inexistente", books, shelf)
        advanceUntilIdle()

        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.bookNotFound)
        assertTrue(shelf.entries.value.isEmpty())
    }

    @Test
    fun `falha ao salvar avisa e mantem o formulario aberto`() = runTest(mainDispatcherRule.dispatcher) {
        shelf.writeError = IllegalStateException("disco cheio")
        val vm = EntryEditorViewModel("A", books, shelf)
        advanceUntilIdle()

        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaving)
        assertNull(vm.uiState.value.result)
    }

    @Test
    fun `remover da estante informa sucesso`() = runTest(mainDispatcherRule.dispatcher) {
        shelf.save(ShelfEntry("A", ReadingStatus.WANT_TO_READ, null, null, null, 1L))
        val vm = EntryEditorViewModel("A", books, shelf)
        advanceUntilIdle()

        vm.onRemove()
        advanceUntilIdle()

        assertEquals(EditorResult.REMOVED, vm.uiState.value.result)
        assertTrue(shelf.entries.value.isEmpty())
    }

    @Test
    fun `cada falha tem a mensagem certa`() {
        assertEquals(ErrorKind.NO_CONNECTION, UnknownHostException().toErrorKind())
        assertEquals(ErrorKind.SERVER, SocketTimeoutException().toErrorKind())
        assertEquals(ErrorKind.UNKNOWN, IllegalStateException().toErrorKind())
    }
}
