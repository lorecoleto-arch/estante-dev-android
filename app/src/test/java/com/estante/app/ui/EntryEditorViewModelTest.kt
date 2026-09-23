package com.estante.app.ui

import com.estante.app.domain.FieldError
import com.estante.app.domain.ReadingStatus
import com.estante.app.fakes.FakeBookRepository
import com.estante.app.fakes.FakeShelfRepository
import com.estante.app.fakes.MainDispatcherRule
import com.estante.app.fakes.testBook
import com.estante.app.ui.editor.EditorResult
import com.estante.app.ui.editor.EntryEditorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(testBook("A", pageCount = 200)))
    private val shelf = FakeShelfRepository(books)

    private fun createViewModel() = EntryEditorViewModel("A", books, shelf, clock = { 42L })

    @Test
    fun `salvar com erro mostra os erros e nao grava`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onStatusChange(ReadingStatus.READING)
        vm.onPagesChange("999")
        vm.onSave()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.showErrors)
        assertEquals(FieldError.PAGES_OUT_OF_RANGE, state.validation.pagesError)
        assertNull(state.result)
        assertTrue(shelf.entries.value.isEmpty())
    }

    @Test
    fun `salvar valido grava na estante e fecha a tela`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onStatusChange(ReadingStatus.FINISHED)
        vm.onRatingChange(5)
        vm.onNoteChange("  Releitura obrigatória.  ")
        vm.onSave()
        advanceUntilIdle()

        val saved = shelf.entries.value.getValue("A")
        assertEquals(ReadingStatus.FINISHED, saved.status)
        assertEquals(5, saved.rating)
        assertEquals(200, saved.pagesRead) // lido = todas as páginas
        assertEquals("Releitura obrigatória.", saved.note)
        assertEquals(EditorResult.SAVED, vm.uiState.value.result)
    }

    @Test
    fun `editar carrega o registro existente no formulario`() = runTest(mainDispatcherRule.dispatcher) {
        val first = createViewModel()
        advanceUntilIdle()
        first.onStatusChange(ReadingStatus.READING)
        first.onPagesChange("50")
        first.onSave()
        advanceUntilIdle()

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isExistingEntry)
        assertEquals(ReadingStatus.READING, state.form.status)
        assertEquals("50", state.form.pagesRead)
    }
}
