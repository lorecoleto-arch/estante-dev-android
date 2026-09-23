package com.estante.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estante.app.domain.ReadingStatus
import com.estante.app.fakes.FakeAppContainer
import com.estante.app.fakes.testBook
import com.estante.app.ui.theme.EstanteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de interface do fluxo principal, rodando na JVM (Robolectric) com dados falsos:
 * busca -> detalhes -> formulário (com validação) -> salvar -> estante.
 */
@RunWith(AndroidJUnit4::class)
class EstanteAppFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val container = FakeAppContainer().apply {
        bookRepository.catalog.value = mapOf("OL1W" to testBook("OL1W", "Código Limpo", pageCount = 256))
    }

    @Test
    fun fluxoCompleto_adicionarLivroLidoNaEstante() {
        composeRule.setContent { EstanteTheme { EstanteApp(container) } }

        // 1. Busca: a lista mostra o livro
        composeRule.onNodeWithContentDescription("Código Limpo", substring = true).performClick()

        // 2. Detalhes: descrição carregada e botão para adicionar
        composeRule.onNodeWithText("Uma descrição de teste.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar à estante").performScrollTo().performClick()

        // 3. Formulário: "Lido" sem nota -> erro de validação
        composeRule.onNodeWithText("Lido").performClick()
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Dê uma nota de 1 a 5 para o livro lido.")[0].assertExists()
        assertEquals(emptyMap<String, Any>(), container.shelfRepository.entries.value)

        // 4. Corrige (nota 4) e salva
        composeRule.onNodeWithContentDescription("Nota 4 de 5").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Nota 4 de 5").assertIsSelected()
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ReadingStatus.FINISHED, container.shelfRepository.entries.value["OL1W"]?.status)

        // 5. Voltou aos detalhes, que já mostram o novo estado
        composeRule.onNodeWithText("Editar leitura").performScrollTo().assertIsDisplayed()

        // 6. Aba "Minha estante" lista o livro
        composeRule.onNodeWithText("Minha estante", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Todos (1)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Código Limpo", substring = true).assertIsDisplayed()
    }

    @Test
    fun estanteVazia_mostraEstadoVazio() {
        composeRule.setContent { EstanteTheme { EstanteApp(container) } }

        composeRule.onNodeWithText("Minha estante", useUnmergedTree = true).performClick()

        composeRule.onNodeWithText("Sua estante está vazia").assertIsDisplayed()
    }
}
