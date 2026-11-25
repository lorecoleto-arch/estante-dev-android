package com.estante.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estante.app.fakes.testBook
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.RatingBar
import com.estante.app.ui.search.SearchResultsState
import com.estante.app.ui.search.SearchScreen
import com.estante.app.ui.search.SearchUiState
import com.estante.app.ui.theme.EstanteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Testes de componentes reutilizáveis e dos estados visuais, usando as versões "sem estado" das telas. */
@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ratingBar_tocarNaEstrelaAtualizaANota() {
        composeRule.setContent {
            var rating by remember { mutableStateOf<Int?>(null) }
            EstanteTheme { RatingBar(rating = rating, onRatingChange = { rating = it }) }
        }

        composeRule.onNodeWithContentDescription("Nota 3 de 5").performClick()

        composeRule.onNodeWithContentDescription("Nota 3 de 5").assertIsSelected()
    }

    @Test
    fun busca_estadoDeErroMostraMensagemEBotaoDeTentarDeNovo() {
        var retried = false
        composeRule.setContent {
            EstanteTheme {
                SearchScreen(
                    query = "kotlin",
                    state = SearchUiState(results = SearchResultsState.Error(ErrorKind.NO_CONNECTION)),
                    onQueryChange = {},
                    onSearch = {},
                    onClear = {},
                    onRetry = { retried = true },
                    onBookClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Sem conexão com a internet", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertTrue(retried)
    }

    @Test
    fun busca_estadoVazioMostraOTermoBuscado() {
        composeRule.setContent {
            EstanteTheme {
                SearchScreen(
                    query = "xyz",
                    state = SearchUiState(results = SearchResultsState.Empty("xyz")),
                    onQueryChange = {},
                    onSearch = {},
                    onClear = {},
                    onRetry = {},
                    onBookClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Nenhum livro encontrado").assertIsDisplayed()
        composeRule.onNodeWithText("Não achamos nada para “xyz”", substring = true).assertIsDisplayed()
    }

    @Test
    fun busca_avisoDeResultadosSalvosApareceMesmoComAListaJaNaTela() {
        val books = List(30) { testBook(id = "OL$it", title = "Livro $it") }
        var isStale by mutableStateOf(false)
        composeRule.setContent {
            EstanteTheme {
                SearchScreen(
                    query = "kotlin",
                    state = SearchUiState(results = SearchResultsState.Content(books, isStale)),
                    onQueryChange = {},
                    onSearch = {},
                    onClear = {},
                    onRetry = {},
                    onBookClick = {},
                )
            }
        }
        composeRule.onNodeWithText("30 resultados").assertIsDisplayed()

        isStale = true

        composeRule.onNodeWithText("mostrando resultados salvos", substring = true).assertIsDisplayed()
    }
}
